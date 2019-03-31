package ru.byprogminer.Lab6_Programming.udp;

import ru.byprogminer.Lab6_Programming.PriorityThing;
import sun.plugin.dom.exception.InvalidStateException;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Parts structure
 *
 * +--------+-------+-------+----+------+---------+
 * | Action | Index | Count | ID | Time | Content |
 * +--------+-------+-------+----+------+---------+
 *
 *   - Action  -  byte  - Action ({@see Action})
 *   - Index   -  byte  - Number of this part
 *   - Count   -  byte  - Count of parts in this packet (decreased by one)
 *   - ID      -  long  - Unique ID of this packet
 *   - Time    -  long  - Sending time of this part
 *   - Content - byte[] - Content of this part
 */
public abstract class UDPSocket<D> {

    public final static int HEADER_SIZE = 3 * Byte.BYTES + 2 * Long.BYTES;

    private final static long REPEATING_PERIOD = 1000;
    private final static BigInteger BYTE_MASK = BigInteger.valueOf(0xFF);

    // General
    protected SocketAddress remote = null;
    protected final BigDecimal packetSize;
    protected final D device;

    // Sending
    private final AtomicLong nextId = new AtomicLong();
    private final Set<Long> confirmedObjects = new HashSet<>();

    // Receiving
    private final Map<Long, PriorityQueue<PriorityThing<Byte, ByteBuffer>>> packets = Collections.synchronizedMap(new HashMap<>());
    private final Map<Long, Long> objectsSendTime = Collections.synchronizedMap(new HashMap<>());
    private final Set<Long> notConfirmedObjects = Collections.synchronizedSet(new HashSet<>());
    private final Set<Long> objectsEndReceived = Collections.synchronizedSet(new HashSet<>());
    private final PriorityQueue<PriorityThing<Long, Object>> objects = new PriorityQueue<>();
    private final Set<Long> receivedObjects = Collections.synchronizedSet(new HashSet<>());

    public UDPSocket(D device, int packetSize) {
        this.packetSize = BigDecimal.valueOf(packetSize);
        this.device = device;
    }

    public synchronized void connect(SocketAddress server) throws IOException {
        if (remote != null) {
            throw new InvalidStateException("socket is already connected");
        }

        remote = server;

        try {
            final ByteBuffer packet = ByteBuffer.allocate(HEADER_SIZE);
            do {
                sendPacket(Action.CONNECT, (byte) 0, (byte) 0, 0, ByteBuffer.allocate(0));
                remote = receiveDatagram(packet);
                packet.flip();
            } while (Action.by(packet.get()) != Action.CONNECT);
        } catch (IOException | RuntimeException | Error e) {
            remote = null;
            throw e;
        }
    }

    public synchronized void accept(SocketAddress from) throws IOException {
        if (remote != null) {
            throw new InvalidStateException("socket is already connected");
        }

        remote = from;
        sendPacket(Action.CONNECT, (byte) 0, (byte) 0, 0, ByteBuffer.allocate(0));
    }

    public final void send(Object object) throws IOException {
        send(object, Action.TRANSPORT, nextId.getAndIncrement());
    }

    public final void sendCaring(Object object) throws IOException {
        final long id = nextId.getAndIncrement();
        send(object, Action.CARING_TRANSPORT, id);

        final Thread repeatingThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(REPEATING_PERIOD);
                    send(object, Action.REPEAT, id);
                } catch (IOException | InterruptedException ignored) {}
            }
        });

        repeatingThread.start();
        synchronized (confirmedObjects) {
            while (!confirmedObjects.contains(id)) {
                try {
                    receivePacket();
                } catch (ClassNotFoundException ignored) {}
            }
        }

        repeatingThread.interrupt();
        sendPacket(Action.CONFIRM_CONFIRM, (byte) 0, (byte) 0, id, ByteBuffer.allocate(0));
    }

    public final <T> T receive(Class<T> clazz) throws IOException, ClassNotFoundException {
        final T ret;

        while (true) {
            synchronized (objects) {
                while (objects.isEmpty()) {
                    receivePacket();
                }

                final Object object = objects.remove().getThing();
                if (clazz.isInstance(object)) {
                    ret = clazz.cast(object);
                    break;
                }
            }
        }

        return ret;
    }

    private void send(Object object, Action action, long id) throws IOException {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(object);
        objectStream.close();

        final ByteBuffer buffer = ByteBuffer.wrap(byteStream.toByteArray());
        final short count = BigDecimal.valueOf(buffer.remaining())
                .divide(packetSize, BigDecimal.ROUND_CEILING)
                .subtract(BigDecimal.ONE).toBigInteger()
                .and(BYTE_MASK).shortValue();
        for (int index = 0; index <= count; ++index) {
            sendPacket(action, (byte) (index & 0xFF), (byte) count, id, buffer);
        }
    }

    private void sendPacket(Action action, byte index, byte count, long id, ByteBuffer content) throws IOException {
        final ByteBuffer packet = ByteBuffer.allocate(HEADER_SIZE + packetSize.intValue());

        packet.put(action.getCode());
        packet.put(index);
        packet.put(count);
        packet.putLong(id);
        packet.putLong(System.currentTimeMillis());
        while (packet.hasRemaining() && content.hasRemaining()) {
            packet.put(content.get());
        }

        packet.flip();
        sendDatagram(packet);
    }

    private void receivePacket() throws IOException, ClassNotFoundException {
        final ByteBuffer packet = ByteBuffer.allocate(HEADER_SIZE + packetSize.intValue());
        receiveDatagram(packet);

        if (packet.position() < HEADER_SIZE) {
            return;
        }

        packet.flip();
        final Action action = Action.by(packet.get());
        final byte index = packet.get();
        final byte count = packet.get();
        final Long id = packet.getLong();
        final Long time = packet.getLong();
        packet.compact();
        packet.flip();

        if (action == Action.CONNECT) {
            return;
        }

        if (action == Action.CONFIRM_CONFIRM) {
            notConfirmedObjects.remove(id);
            return;
        }

        if (action == Action.CONFIRM_TRANSPORT) {
            confirmedObjects.add(id);
            return;
        }

        if (action == Action.REPEAT && !notConfirmedObjects.contains(id)) {
            return;
        }

        if (action == Action.REPEAT && receivedObjects.contains(id)) {
            sendPacket(Action.CONFIRM_TRANSPORT, (byte) 0, (byte) 0, id, ByteBuffer.allocate(0));
            return;
        }

        if (action == Action.CARING_TRANSPORT) {
            notConfirmedObjects.add(id);
        }

        final PriorityQueue<PriorityThing<Byte, ByteBuffer>> objectPackets = packets
                .computeIfAbsent(id, aLong -> new PriorityQueue<>());

        synchronized (objectPackets) {
            objectPackets.add(new PriorityThing<>(index, packet));

            final Long prevTime = objectsSendTime.put(id, time);
            if (prevTime != null && prevTime < time) {
                objectsSendTime.put(id, prevTime);
            }

            if (index == count) {
                objectsEndReceived.add(id);
            }

            if (objectsEndReceived.contains(id) && objectPackets.size() - 1 == count) {
                constructPacket(id);
            }
        }
    }

    private void constructPacket(Long id) throws IOException, ClassNotFoundException {
        final PriorityQueue<PriorityThing<Byte, ByteBuffer>> packetParts = packets.remove(id);

        final byte[] buffer = new byte[packetParts.size() * packetSize.intValue()];
        final ByteBuffer packetBuffer = ByteBuffer.wrap(buffer);
        while (!packetParts.isEmpty()) {
            packetBuffer.put(packetParts.remove().getThing());
        }

        ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(buffer, 0, packetBuffer.position()));
        Object object = objectStream.readObject();
        objectStream.close();

        final Long packetSendTime = objectsSendTime.remove(id);
        objects.add(new PriorityThing<>(packetSendTime, object));
        receivedObjects.add(id);

        sendPacket(Action.CONFIRM_TRANSPORT, (byte) 0, (byte) 0, id, ByteBuffer.allocate(0));
    }

    protected abstract SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException;
    protected abstract void sendDatagram(ByteBuffer content) throws IOException;
}

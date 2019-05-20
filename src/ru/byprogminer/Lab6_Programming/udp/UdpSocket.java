package ru.byprogminer.Lab6_Programming.udp;

import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 * Packets structure
 *
 * +-----------+--------+----------+-------+---------+
 * | Signature | Action | Previous | Count | Content |
 * +-----------+--------+----------+-------+---------+
 *
 *   - Signature -  int   - Signature
 *   - Action    -  byte  - Action ({@see Action})
 *   - Previous  -  long  - Hash of the previous sent packet
 *   - Count     -  byte  - Count of packets in this object (decreased by one)
 *   - Content   - byte[] - Content of this packet
 */
public abstract class UdpSocket<D> implements Closeable {

    /**
     * Inner class for receive packets
     */
    private class Receiver {

        private final Logger log = Loggers.getObjectLogger(this);

        /**
         * {@link Thread} object of this Receiver
         */
        private volatile Thread thread = null;

        /**
         * Set of expected, but not received, confirmations (CONFIRM packets)
         */
        private final Set<Long> expectedConfirmations = new ConcurrentSkipListSet<>();

        /**
         * Blocking queue of received objects
         */
        private final BlockingQueue<Object> receivedObjects = new LinkedBlockingQueue<>();

        /**
         * Buffer of receiving object
         */
        private volatile ByteBuffer receivingObject = null;

        /**
         * Pull of received but not processed packets
         */
        private final Map<Long, ByteBuffer> pool = new HashMap<>();

        /**
         * Previous received and processed packet hash
         */
        private final AtomicLong previous = new AtomicLong();

        /**
         * Starts receiver
         *
         * @throws IllegalStateException if a receiver is already started
         */
        public void start() {
            if (thread != null) {
                throw new IllegalStateException("receiver is already started");
            }

            thread = new Thread(this::run);
            thread.setName("UDP socket's #" + System.identityHashCode(UdpSocket.this) + " receiver");
            thread.start();
        }

        private void run() {
            log.info("receiver started");

            while (!closed) {
                try {
                    receivePacket();
                } catch (InterruptedException | SocketTimeoutException ignored) {
                } catch (Throwable e) {
                    log.log(Level.INFO, "exception in receiver loop", e);
                }
            }

            log.info("receiver finished");
        }

        /**
         * Receives packet and processes it
         */
        private void receivePacket() throws IOException, InterruptedException, ClassNotFoundException {
            final ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + packetSize.intValue());
            final SocketAddress address = receiveDatagram(buffer);
            buffer.flip();

            final ByteBuffer packet = buffer.asReadOnlyBuffer();
            if (buffer.remaining() < HEADER_SIZE || buffer.getInt() != SIGNATURE) {
                return;
            }

            log.info("received packet: " + packetToString(packet));
            packet.rewind();

            final Action action = Action.by(buffer.get());
            if (action == Action.CONNECT) {
                synchronized (UdpSocket.this) {
                    if (remote == null) {
                        remote = address;
                    }
                }

                return;
            }

            preprocessPacket(packet);
        }

        private void preprocessPacket(ByteBuffer packet) throws IOException, InterruptedException, ClassNotFoundException {
            packet.getInt(); // Skip Signature

            final Action action = Action.by(packet.get());
            final Long previous = packet.getLong();
            packet.rewind();

            switch (action) {
                case CONNECT:
                    return;

                case TRANSPORT:
                    final long hash = crc32(packet);
                    packet.rewind();

                    sendDatagram(makePacket(Action.CONFIRM, hash, 0, ByteBuffer.allocate(0)));

                case FINISH:
                    pool.putIfAbsent(previous, packet);

                    processPool();
                    return;

                case CONFIRM:
                    expectedConfirmations.remove(previous);
            }
        }

        private void processPool() throws IOException, InterruptedException, ClassNotFoundException {
            synchronized (pool) {
                long previous = this.previous.get();

                ByteBuffer packet;
                while ((packet = pool.remove(previous)) != null) {
                    previous = crc32(packet);
                    packet.rewind();

                    processPacket(packet);
                }

                if (this.previous.get() != previous) {
                    log.info(String.format("rotate previous: %d -> %d", this.previous.get(), previous));
                }

                this.previous.set(previous);
            }
        }

        private void processPacket(ByteBuffer packet) throws IOException, InterruptedException, ClassNotFoundException {
            packet.getInt(); // Skip Signature

            switch (Action.by(packet.get())) {
                case TRANSPORT:
                    break;

                case FINISH:
                    closed = true;

                default:
                    return;
            }

            packet.getLong(); // Skip Previous
            final int count = packet.get() + 1;

            final ByteBuffer objectBuffer;
            if (count > 1) {
                final ByteBuffer receivingObject = this.receivingObject;
                this.receivingObject = objectBuffer = (receivingObject == null ? ByteBuffer.allocate(packetSize.intValue() * count) : receivingObject);

                objectBuffer.put(packet);
            } else {
                objectBuffer = packet.slice();
                objectBuffer.position(objectBuffer.remaining()); // It's OK
            }

            if (objectBuffer.remaining() < packetSize.intValue()) {
                objectBuffer.flip();

                receivingObject = null;

                final byte[] buffer = new byte[objectBuffer.remaining()];
                objectBuffer.get(buffer);

                final ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(buffer));
                final Object receivedObject = stream.readObject();
                log.info("received object: " + receivedObject);
                receivedObjects.put(receivedObject);
                stream.close();
            }
        }

        private String packetToString(ByteBuffer packet) {
            packet.getInt(); // Skip Signature

            final Action action = Action.by(packet.get());
            final long previous = packet.getLong();
            final byte count = packet.get();
            final int contentSize = packet.remaining();

            final StringBuilder packetStart = new StringBuilder();
            for (int i = 0; i < 3 && packet.hasRemaining(); ++i) {
                if (i != 0) {
                    packetStart.append(", ");
                }

                packetStart.append(String.format("0x%X", packet.get()));
            }

            if (packet.hasRemaining()) {
                packetStart.append("...");
            }

            return String.format("%2$d -> %1$s/%3$d new byte[%4$d] {%5$s}",
                    action, previous, count, contentSize, packetStart);
        }
    }

    /**
     * Signature of packets
     */
    public static final int SIGNATURE = 0x55_44_50_53;

    /**
     * Size of packet header in bytes
     */
    public static final int HEADER_SIZE = 2 * Byte.BYTES + Integer.BYTES + Long.BYTES;

    // Logging
    private final Logger log = Loggers.getObjectLogger(this);

    // General
    protected volatile SocketAddress remote = null;
    private volatile boolean closed = false;
    protected final BigDecimal packetSize;
    protected final D device;

    // Sending
    private static final long RESEND_DELAY = 100;

    private final AtomicLong previous = new AtomicLong();
    private final Object sendMutex = new Object();

    // Receiving
    private final Receiver receiver = new Receiver();

    /**
     * Initializes socket and starts receiver thread
     *
     * @param device device for working with datagrams
     * @param packetSize size of content field of packets
     */
    public UdpSocket(D device, int packetSize) {
        this.packetSize = BigDecimal.valueOf(packetSize);
        this.device = device;

        log.info("starting receiver");
        receiver.start();
    }

    /**
     * Sends CONNECT packet to server
     *
     * @param to server address
     *
     * @throws IllegalStateException if socket is connected
     * @throws SocketTimeoutException if connection timed out
     */
    public final void connect(SocketAddress to, long timeout) throws IOException {
        synchronized (this) {
            assertNotConnected();

            remote = to;
            sendDatagram(makePacket(Action.CONNECT, 0, ByteBuffer.allocate(0)));
            remote = null;
        }

        log.info("try to connect to server");
        final long start = System.currentTimeMillis();
        while (!closed && remote == null && System.currentTimeMillis() - start < timeout) {
            Thread.yield();
        }

        if (remote == null) {
            log.info("connection timed out");
            throw new SocketTimeoutException("connection timed out");
        }
    }

    /**
     * Sends CONNECT packet from server
     *
     * @param from client address
     *
     * @throws IllegalStateException if socket is connected
     */
    public final synchronized void accept(SocketAddress from) throws IOException {
        assertNotConnected();

        log.info("connect to client");

        remote = from;
        sendDatagram(makePacket(Action.CONNECT, 0, ByteBuffer.allocate(0)));
    }

    /**
     * Sends object to remote host
     *
     * @param object object to send
     *
     * @throws IllegalStateException if socket isn't connected
     * @throws SocketTimeoutException if time is out
     */
    public final void send(Object object, long timeout) throws IOException, InterruptedException {
        if (remote == null) {
            log.info("socket isn't connected");
            throw new IllegalStateException("socket isn't connected");
        }

        assertNotClosed();
        synchronized (sendMutex) {
            final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
            final ObjectOutputStream objectStream = new ObjectOutputStream(byteArrayStream);
            objectStream.writeObject(object);
            objectStream.close();

            log.info("send object " + object);
            final ByteBuffer objectBuffer = ByteBuffer.wrap(byteArrayStream.toByteArray());
            final int count = BigDecimal.valueOf(objectBuffer.remaining()).divide(packetSize, 0, RoundingMode.UP)
                    .toBigInteger().subtract(BigInteger.ONE).and(BigInteger.valueOf(0xFF)).intValue();

            int i;
            final long start = System.currentTimeMillis();
            for (i = 0; i <= count && !closed && System.currentTimeMillis() - start < timeout; ++i) {
                final ByteBuffer packet;
                final Long hash;

                synchronized (previous) {
                    packet = makePacket(Action.TRANSPORT, count, objectBuffer);
                    hash = crc32(packet);
                    previous.set(hash);
                }

                receiver.expectedConfirmations.add(hash);
                log.info(String.format("send %d/%d packet", i, count));

                packet.rewind();
                sendDatagram(packet);

                Thread.sleep(RESEND_DELAY);
                while (!closed && receiver.expectedConfirmations.contains(hash) && System.currentTimeMillis() - start < timeout) {
                    try {
                        log.info(String.format("resend %d/%d packet", i, count));

                        packet.rewind();
                        sendDatagram(packet);
                        Thread.sleep(RESEND_DELAY);
                    } catch (InterruptedException | SocketTimeoutException ignored) {
                    } catch (Throwable e) {
                        log.log(Level.INFO, "exception in resending loop", e);
                    }
                }
            }

            if (closed) {
                log.info("socket closed while sending");
                throw new IllegalStateException("socked closed");
            }

            if (i <= count) {
                log.info("sending is timed out");
                throw new SocketTimeoutException("sending is timed out");
            }

            log.info("object sent");
        }
    }

    /**
     * Receives object
     *
     * @param <T> type of object
     *
     * @param clazz class of type of object
     *
     * @return object
     *
     * @throws SocketTimeoutException if time is out
     */
    public final <T> T receive(Class<? extends T> clazz, long timeout) throws InterruptedException, SocketTimeoutException {
        assertNotClosed();

        final long start = System.currentTimeMillis();
        while (!closed && System.currentTimeMillis() - start <= timeout) {
            final Object object = receiver.receivedObjects.poll(timeout, TimeUnit.MILLISECONDS);

            if (clazz.isInstance(object)) {
                log.info("received object: " + object);
                return clazz.cast(object);
            }
        }

        if (closed) {
            log.info("socket closed while receiving");
            throw new IllegalStateException("socked closed");
        }

        log.info("receiving is timed out");
        throw new SocketTimeoutException("receiving is timed out");
    }

    /**
     * Closes socket
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        log.info("close socket");

        try {
            sendDatagram(makePacket(Action.FINISH, 0, ByteBuffer.allocate(0)));
        } catch (IOException e) {
            log.log(Level.WARNING, "unable to send FINISH packet", e);
        }

        closed = true;
        receiver.thread.interrupt();
    }

    private ByteBuffer makePacket(Action action, int count, ByteBuffer content) {
        return makePacket(action, previous.get(), count, content);
    }

    private ByteBuffer makePacket(Action action, long previous, int count, ByteBuffer content) {
        final ByteBuffer packet = ByteBuffer.allocate(HEADER_SIZE + Math.min(packetSize.intValue(), content.remaining()));

        packet.putInt(SIGNATURE);
        packet.put(action.getCode());
        packet.putLong(previous);
        packet.put((byte) (count & 0xFF));
        while (packet.hasRemaining() && content.hasRemaining()) {
            packet.put(content.get());
        }

        packet.flip();
        return packet;
    }

    private static long crc32(ByteBuffer buffer) {
        final CRC32 crc32 = new CRC32();
        crc32.update(buffer);

        return crc32.getValue();
    }

    private void assertNotConnected() {
        if (remote != null) {
            log.info("socket is connected already");
            throw new IllegalStateException("socket is connected already");
        }
    }

    private void assertNotClosed() {
        if (closed) {
            log.info("socket is closed");
            throw new IllegalStateException("socket is closed");
        }
    }

    protected abstract SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException;
    protected abstract void sendDatagram(ByteBuffer content) throws IOException;

    public boolean isConnected() {
        return remote != null;
    }

    public boolean isClosed() {
        return closed;
    }

    public D getDevice() {
        return device;
    }
}

package ru.byprogminer.Lab6_Programming.udp;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
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
public abstract class UDPSocket<D> implements Closeable {

    /**
     * Signature of packets
     */
    public final static int SIGNATURE = 0x55_44_50_53;

    /**
     * Size of packet header in bytes
     */
    public final static int HEADER_SIZE = 2 * Byte.BYTES + Integer.BYTES + Long.BYTES;

    /**
     * Inner class for receive packets
     */
    private class Receiver {

        /**
         * {@link Thread} object of this Receiver
         */
        private volatile Thread thread = null;

        /**
         * Set of expected, but not received, confirmations (CONFIRM packets)
         */
        private final Set<Long> expectedConfirmations = Collections.synchronizedSet(new HashSet<>());

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
        private final Map<Long, ByteBuffer> pull = new HashMap<>();

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
            thread.start();
        }

        private void run() {
            while (!closed) {
                try {
                    receivePacket();
                } catch (SocketTimeoutException ignored) {
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
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

            final Action action = Action.by(buffer.get());
            if (action == Action.CONNECT) {
                synchronized (UDPSocket.this) {
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
                    pull.putIfAbsent(previous, packet);

                    processPull();
                    return;

                case CONFIRM:
                    expectedConfirmations.remove(previous);
            }
        }

        private void processPull() throws IOException, InterruptedException, ClassNotFoundException {
            synchronized (pull) {
                long previous = this.previous.get();

                while (pull.containsKey(previous)) {
                    final ByteBuffer packet = pull.remove(previous);

                    previous = crc32(packet);
                    packet.rewind();

                    processPacket(packet);
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
                synchronized (this) {
                    objectBuffer = receivingObject = (receivingObject == null ? ByteBuffer.allocate(packetSize.intValue() * count) : receivingObject);
                }

                objectBuffer.put(packet);
            } else {
                objectBuffer = packet.slice();
                objectBuffer.position(objectBuffer.remaining());
            }

            if (objectBuffer.remaining() < packetSize.intValue()) {
                objectBuffer.flip();

                receivingObject = null;

                final byte[] buffer = new byte[objectBuffer.remaining()];
                objectBuffer.get(buffer);

                final ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(buffer));
                receivedObjects.put(stream.readObject());
                stream.close();
            }
        }
    }

    // General
    protected volatile SocketAddress remote = null;
    private volatile boolean closed = false;
    protected final BigDecimal packetSize;
    protected final D device;

    // Sending
    private final static long RESEND_DELAY = 100;
    private final AtomicLong previous = new AtomicLong();
    private final Object sendSemaphore = new Object();

    // Receiving
    private final Receiver receiver = new Receiver();

    /**
     * Initializes socket and starts receiver thread
     *
     * @param device device for working with datagrams
     * @param packetSize size of content field of packets
     */
    public UDPSocket(D device, int packetSize) {
        this.packetSize = BigDecimal.valueOf(packetSize);
        this.device = device;

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

        final long start = System.currentTimeMillis();
        while (remote == null && System.currentTimeMillis() - start < timeout) {
            Thread.yield();
        }

        if (remote == null) {
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

        remote = from;
        sendDatagram(makePacket(Action.CONNECT, 0, ByteBuffer.allocate(0)));
    }

    /**
     * Sends object to remote host
     *
     * @param object object to send
     *
     * @throws IllegalStateException if socket isn't connected
     */
    public final void send(Object object) throws IOException {
        if (remote == null) {
            throw new IllegalStateException("socket isn't connected");
        }

        assertNotClosed();
        synchronized (sendSemaphore) {
            final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
            final ObjectOutputStream objectStream = new ObjectOutputStream(byteArrayStream);
            objectStream.writeObject(object);
            objectStream.close();

            final ByteBuffer objectBuffer = ByteBuffer.wrap(byteArrayStream.toByteArray());
            final int count = BigDecimal.valueOf(objectBuffer.remaining()).divide(packetSize, 0, RoundingMode.UP)
                    .toBigInteger().subtract(BigInteger.ONE).and(BigInteger.valueOf(0xFF)).intValue();

            for (int i = 0; i <= count; ++i) {
                final ByteBuffer packet;
                final Long hash;

                synchronized (previous) {
                    packet = makePacket(Action.TRANSPORT, count, objectBuffer);
                    hash = crc32(packet);
                    previous.set(hash);
                }

                receiver.expectedConfirmations.add(hash);

                packet.rewind();
                sendDatagram(packet);
                while (receiver.expectedConfirmations.contains(hash)) {
                    try {
                        Thread.yield();
                        Thread.sleep(RESEND_DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    packet.rewind();
                    sendDatagram(packet);
                }
            }
        }
    }

    /**
     * Receives object
     *
     * @param <T> type of object
     *
     * @param clazz class of type of object
     * @param timeout timeout
     *
     * @return object
     *
     * @throws SocketTimeoutException if time is out
     */
    public final <T> T receive(Class<T> clazz, long timeout) throws InterruptedException, SocketTimeoutException {
        assertNotClosed();

        while (true) {
            final Object object = receiver.receivedObjects.poll(timeout, TimeUnit.MILLISECONDS);

            if (object == null) {
                throw new SocketTimeoutException("timed out");
            }

            if (clazz.isInstance(object)) {
                return clazz.cast(object);
            }
        }
    }

    /**
     * Closes socket
     */
    @Override
    public void close() throws IOException {
        sendDatagram(makePacket(Action.FINISH, 0, ByteBuffer.allocate(0)));
        closed = true;
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
            packet.put(content);
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
            throw new IllegalStateException("socket is connected already");
        }
    }

    private void assertNotClosed() {
        if (closed) {
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

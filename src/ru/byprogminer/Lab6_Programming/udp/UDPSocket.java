package ru.byprogminer.Lab6_Programming.udp;

import java.io.*;
import java.math.BigDecimal;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

/**
 * Packets structure
 *
 * +--------+----------+-------+---------+
 * | Action | Previous | Count | Content |
 * +--------+----------+-------+---------+
 *
 *   - Action   -  byte  - Action ({@see Action})
 *   - Previous -  long  - Hash of the previous sent packet
 *   - Count    -  byte  - Count of packets in this object (decreased by one)
 *   - Content  - byte[] - Content of this packet
 */
public abstract class UDPSocket<D> {

    public final static int HEADER_SIZE = 2 * Byte.BYTES + Long.BYTES;

    /**
     * Inner class for receive packets
     */
    private class Receiver {

        /**
         * {@link ScheduledFuture} object of this Receiver
         */
        private ScheduledFuture<?> future = null;

        /**
         * Blocking queue of received objects
         */
        private final BlockingQueue<Object> receivedObjects = new LinkedBlockingQueue<>();

        /**
         * Starts receiver
         *
         * @return created {@link ScheduledFuture} object
         *
         * @throws IllegalStateException if a receiver is already started
         */
        public ScheduledFuture<?> start() {
            if (future != null) {
                throw new IllegalStateException("Receiver is already started");
            }

            return future = Executors.newSingleThreadScheduledExecutor()
                    .scheduleWithFixedDelay(this::tick, 0, 1, TimeUnit.MICROSECONDS);
        }

        private void tick() {
            try {
                receivePacket();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        /**
         * Receives packet and processes it
         */
        private void receivePacket() {
            // TODO
        }
    }

    // General
    protected volatile SocketAddress remote = null;
    protected final BigDecimal packetSize;
    protected final D device;

    // Sending
    // TODO
    private final AtomicLong previous = new AtomicLong();

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
            if (remote != null) {
                throw new IllegalStateException("socket is connected already");
            }

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
        if (remote != null) {
            throw new IllegalStateException("socket is connected already");
        }

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

        // TODO
    }

    /**
     * Receives object
     *
     * @param <T> type of object
     *
     * @param clazz class of type of object
     *
     * @return object
     */
    public final <T> T receive(Class<T> clazz) throws InterruptedException {
        while (true) {
            final Object object = receiver.receivedObjects.take();

            if (clazz.isInstance(object)) {
                return clazz.cast(object);
            }
        }
    }

    private ByteBuffer makePacket(Action action, int count, ByteBuffer content) {
        final ByteBuffer packet = ByteBuffer.allocate(HEADER_SIZE + Math.min(packetSize.intValue(), content.remaining()));

        packet.put(action.getCode());
        packet.putLong(previous.get());
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

    protected abstract SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException;
    protected abstract void sendDatagram(ByteBuffer content) throws IOException;
}

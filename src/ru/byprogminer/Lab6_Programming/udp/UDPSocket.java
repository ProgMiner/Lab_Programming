package ru.byprogminer.Lab6_Programming.udp;

import sun.plugin.dom.exception.InvalidStateException;

import java.io.*;
import java.math.BigDecimal;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.zip.CRC32;

/**
 * Parts structure
 *
 * +------+--------+-------+---------+
 * | Hash | Action | Count | Content |
 * +------+--------+-------+---------+
 *
 *   - Hash     -  long  - Hash of this part without Hash
 *   - Action   -  byte  - Action ({@see Action})
 *   - Count    -  byte  - Count of parts in this packet (decreased by one)
 *   - Content  - byte[] - Content of this part
 */
public abstract class UDPSocket<D> {

    public final static int HEADER_SIZE = 2 * Byte.BYTES + Long.BYTES;

    /**
     * Inner class for receive parts
     */
    private class Receiver {

        /**
         * {@link ScheduledFuture} object of this Receiver
         */
        private ScheduledFuture<?> future = null;

        /**
         * Starts receiver
         *
         * @return created {@link ScheduledFuture} object
         *
         * @throws InvalidStateException if a receiver is already started
         */
        public ScheduledFuture<?> start() {
            if (future != null) {
                throw new InvalidStateException("Receiver is already started");
            }

            return future = Executors.newSingleThreadScheduledExecutor()
                    .scheduleWithFixedDelay(this::tick, 0, 1, TimeUnit.MICROSECONDS);
        }

        private void tick() {
            try {
                receivePart();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        /**
         * Receives part and processes it
         */
        private void receivePart() {
            // TODO
        }
    }

    // General
    protected volatile SocketAddress remote = null;
    protected final BigDecimal partSize;
    protected final D device;

    // Sending
    // TODO

    // Receiving
    private final Receiver receiver = new Receiver();

    /**
     * Initializes socket and starts receiver thread.
     *
     * @param device device for working with datagrams
     * @param partSize size of content field of parts
     */
    public UDPSocket(D device, int partSize) {
        this.partSize = BigDecimal.valueOf(partSize);
        this.device = device;

        receiver.start();
    }

    /**
     * Sends CONNECT part to server.
     *
     * @param to server address
     */
    public final void connect(SocketAddress to, long timeout) throws IOException {
        // TODO
    }

    /**
     * Sends CONNECT part from server.
     *
     * @param from client address
     */
    public final synchronized void accept(SocketAddress from) throws IOException {
        // TODO
    }

    public final void send(Object packet) throws InterruptedException {
        // TODO
    }

    public final <T> T receive(Class<T> clazz) {
        final T ret;

        while (true) {
            // TODO

            if (clazz.isInstance(object)) {
                ret = clazz.cast(object);
                break;
            }
        }

        return ret;
    }

    private synchronized void sendPart(Action action, int count, ByteBuffer content) throws IOException {
        // TODO
    }

    private static long crc32(ByteBuffer buffer) {
        final CRC32 crc32 = new CRC32();
        crc32.update(buffer);

        return crc32.getValue();
    }

    protected abstract SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException;
    protected abstract void sendDatagram(ByteBuffer content) throws IOException;
}

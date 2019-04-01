package ru.byprogminer.Lab6_Programming.udp;

import sun.plugin.dom.exception.InvalidStateException;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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

    private class Part {

        private final long hash;
        private final Action action;
        private final int count;
        private final ByteBuffer content;

        public Part(long hash, Action action, int count, ByteBuffer content) {
            this.hash = hash;
            this.action = action;
            this.count = count;
            this.content = content;
        }
    }

    /**
     * Inner class for receive parts
     */
    private class Receiver {

        /**
         * Received packet
         */
        private volatile Object packet = null;

        /**
         * Received TRANSPORT parts
         */
        private volatile ByteBuffer parts = null;

        /**
         * Hash of last received TRANSPORT part
         */
        private final AtomicLong lastReceivedPart = new AtomicLong();

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
                    .scheduleWithFixedDelay(this::receivePart, 0, 1, TimeUnit.MICROSECONDS);
        }

        /**
         * Receives part and processes it
         *
         * If received part isn't a part or corrupted, does nothing.
         *
         * If part is a CONNECT part and {@link UDPSocket} currently isn't connected, connect it.
         * If part is a FINISH part, stops receiver.
         *
         * If part is a CONFIRM part, removes part with specified in ID field hash from sentParts map.
         *
         * Else sends CONFIRM and invokes processTransport method for received part.
         */
        private void receivePart() {
            try {
                final ByteBuffer partBuffer = ByteBuffer.allocate(HEADER_SIZE + partSize.intValue());
                final SocketAddress address = receiveDatagram(partBuffer);
                partBuffer.flip();

                final Part part = parsePart(partBuffer);
                if (part == null) {
                    return;
                }

                switch (part.action) {
                    case CONNECT:
                        if (remote == null) {
                            remote = address;
                        }

                        return;
                    case FINISH:
                        future.cancel(false);

                        return;
                    case CONFIRM:
                        if (sentPartHash.get() == part.content.getLong()) {
                            sentPartReceived.set(true);
                        }

                        return;
                }

                final ByteBuffer confirm = ByteBuffer.allocate(Long.BYTES);
                confirm.putLong(part.hash);
                confirm.flip();
                sendPart(Action.CONFIRM, 0, confirm);

                if (lastReceivedPart.get() != part.hash) {
                    lastReceivedPart.set(part.hash);

                    processTransport(part);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        /**
         * Processes TRANSPORT part.
         *
         * Puts part to parts, if it is a last part of this packet,
         * constructs the packet and put it to packet.
         *
         * Updates previous to current hash
         *
         * @param part part for processing
         */
        private void processTransport(Part part) throws IOException, ClassNotFoundException {
            final ByteBuffer packetBuffer = (parts == null ? (parts = ByteBuffer.allocate(part.count * partSize.intValue())) : parts);
            packetBuffer.put(part.content);

            if (packetBuffer.remaining() < partSize.intValue()) {
                parts = null;

                packetBuffer.flip();

                final byte[] packetBytes = new byte[packetBuffer.remaining()];
                packetBuffer.get(packetBytes);

                final ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(packetBytes));
                final Object packet = objectStream.readObject();
                objectStream.close();

                this.packet = packet;
            }
        }
    }

    /**
     * Inner class for send parts
     */
    private class Sender {

        /**
         * Packets to send
         */
        private final BlockingQueue<Object> packets = new LinkedBlockingQueue<>();

        /**
         * {@link ScheduledFuture} object of this Sender
         */
        private ScheduledFuture<?> future = null;

        /**
         * Starts sender
         *
         * @return created {@link ScheduledFuture} object
         *
         * @throws InvalidStateException if a sender is already started
         */
        public ScheduledFuture<?> start() {
            if (future != null) {
                throw new InvalidStateException("Sender is already started");
            }

            return future = Executors.newSingleThreadScheduledExecutor()
                    .scheduleWithFixedDelay(this::sendPacket, 0, 1, TimeUnit.MICROSECONDS);
        }

        /**
         * Sends packet
         *
         * If receiver receives a packet, waits for ending of receiving
         */
        private void sendPacket() {
            while (receiver.parts != null) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                final Object packet = packets.take();

                final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                final ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
                objectStream.writeObject(packet);
                objectStream.close();

                final ByteBuffer buffer = ByteBuffer.wrap(byteStream.toByteArray());
                final int count = BigDecimal.valueOf(buffer.remaining())
                        .divide(partSize, BigDecimal.ROUND_CEILING)
                        .subtract(BigDecimal.ONE).toBigInteger()
                        .and(BYTE_MASK).intValue();

                for (int index = 0; index <= count; ++index) {
                    sendPart(Action.TRANSPORT, count, buffer);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private final static BigInteger BYTE_MASK = BigInteger.valueOf(0xFF);

    // General
    protected volatile SocketAddress remote = null;
    protected final BigDecimal partSize;
    protected final D device;

    // Sending
    private final Sender sender = new Sender();
    private final AtomicLong sentPartHash = new AtomicLong();
    private final AtomicBoolean sentPartReceived = new AtomicBoolean(true);

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
        sender.start();
    }

    /**
     * Sends CONNECT part to server.
     *
     * @param to server address
     */
    public final void connect(SocketAddress to, long timeout) throws IOException {
        synchronized (this) {
            if (remote != null) {
                throw new InvalidStateException("socket is already connected");
            }

            remote = to;
            sendPart(Action.CONNECT, 0, ByteBuffer.allocate(0));
            remote = null;
        }

        final long start = System.currentTimeMillis();
        while (remote == null && System.currentTimeMillis() - start < timeout);

        if (remote == null) {
            throw new SocketTimeoutException("connection timed out");
        }
    }

    /**
     * Sends CONNECT part from server.
     *
     * @param from client address
     */
    public final synchronized void accept(SocketAddress from) throws IOException {
        if (remote != null) {
            throw new InvalidStateException("socket is already connected");
        }

        remote = from;
        sendPart(Action.CONNECT, 0, ByteBuffer.allocate(0));
    }

    public final void send(Object packet) throws InterruptedException {
        sender.packets.put(packet);
    }

    public final <T> T receive(Class<T> clazz) {
        final T ret;

        while (true) {
            while (receiver.packet == null) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            final Object object = receiver.packet;
            receiver.packet = null;

            if (clazz.isInstance(object)) {
                ret = clazz.cast(object);
                break;
            }
        }

        return ret;
    }

    private synchronized void sendPart(Action action, int count, ByteBuffer content) throws IOException {
        final ByteBuffer part = ByteBuffer.allocate(HEADER_SIZE + partSize.intValue());

        part.putLong(0);
        part.put(action.getCode());
        part.put((byte) (count & 0xFF));
        while (part.hasRemaining() && content.hasRemaining()) {
            part.put(content.get());
        }

        part.flip();
        part.getLong();
        final long hash = crc32(part);

        part.rewind();
        part.putLong(hash);

        if (action == Action.TRANSPORT) {
            sentPartReceived.set(false);
            sentPartHash.set(hash);
        }

        do {
            part.rewind();
            sendDatagram(part);

            if (action == Action.TRANSPORT) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while (action == Action.TRANSPORT && !sentPartReceived.get());
    }

    private static long crc32(ByteBuffer buffer) {
        final CRC32 crc32 = new CRC32();
        crc32.update(buffer);

        return crc32.getValue();
    }

    private Part parsePart(ByteBuffer part) {
        if (part.limit() < HEADER_SIZE) {
            return null;
        }

        final long hash = part.getLong();
        part.compact();
        part.flip();

        final long realHash = crc32(part);
        if (hash != realHash) {
            return null;
        }

        part.rewind();
        final Action action = Action.by(part.get());
        final byte count = part.get();
        part.compact();
        part.flip();

        return new Part(hash, action, count + 1, part);
    }

    protected abstract SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException;
    protected abstract void sendDatagram(ByteBuffer content) throws IOException;
}

package ru.byprogminer.Lab6_Programming.udp;

import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.io.*;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            final ByteBuffer buffer = ByteBuffer.allocate(packetUtils.packetSize);
            final SocketAddress address = receiveDatagram(buffer);
            buffer.flip();

            final ParsedPacket packet = packetUtils.parsePacket(buffer);
            if (packet == null) {
                return;
            }

            log.info("received packet: " + packet);
            if (packet.action == Action.CONNECT) {
                synchronized (UdpSocket.this) {
                    if (remote == null) {
                        remote = address;
                    }
                }

                return;
            }

            preprocessPacket(packet);
        }

        private void preprocessPacket(ParsedPacket packet) throws IOException, InterruptedException, ClassNotFoundException {
            switch (packet.action) {
                case CONNECT:
                    return;

                case CONFIRM:
                    expectedConfirmations.remove(packet.previous);
                    return;

                case TRANSPORT:
                    sendDatagram(packetUtils.writePacket(new Packet(Action.CONFIRM, packet.hash)));

                case FINISH:
                    if (rotatePrevious(packet)) {
                        processPacket(packet);
                    }
            }
        }

        private boolean rotatePrevious(ParsedPacket packet) {
            synchronized (this.previous) {
                if (this.previous.get() != packet.previous) {
                    return false;
                }

                log.info(String.format("rotate previous: %d -> %d", packet.previous, packet.hash));
                this.previous.set(packet.hash);
            }

            return true;
        }

        private void processPacket(Packet packet) throws IOException, InterruptedException, ClassNotFoundException {
            switch (packet.action) {
                case TRANSPORT:
                    break;

                case FINISH:
                    closed = true;

                default:
                    return;
            }

            final ByteBuffer objectBuffer;
            if (packet.count > 0) {
                final ByteBuffer receivingObject = this.receivingObject;
                objectBuffer = this.receivingObject = (receivingObject == null ?
                        ByteBuffer.allocate(packetUtils.contentSize * (packet.count + 1)) : receivingObject);

                objectBuffer.put(packet.getContent());
            } else {
                objectBuffer = ByteBuffer.allocate(packetUtils.contentSize);
                objectBuffer.put(packet.getContent());
            }

            if (objectBuffer.remaining() < packetUtils.contentSize) {
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
    }

    // Logging
    private final Logger log = Loggers.getObjectLogger(this);

    // General
    protected volatile SocketAddress remote = null;
    private volatile boolean closed = false;
    private final PacketUtils packetUtils;
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
     * @param contentSize size of content field of packets
     */
    public UdpSocket(D device, int contentSize) {
        this.packetUtils = new PacketUtils(contentSize);
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
            sendDatagram(packetUtils.writePacket(new Packet(Action.CONNECT)));
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
        sendDatagram(packetUtils.writePacket(new Packet(Action.CONNECT)));
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
            final int count = packetUtils.getPacketsCount(objectBuffer.remaining());

            int i;
            final long start = System.currentTimeMillis();
            for (i = 0; i <= count && !closed && System.currentTimeMillis() - start < timeout; ++i) {
                final ByteBuffer packet;
                final Long hash;

                synchronized (previous) {
                    packet = packetUtils.writePacket(packetUtils.makePacket(Action.TRANSPORT, previous.get(), count, objectBuffer));
                    hash = PacketUtils.crc32(packet);

                    log.info(String.format("rotate previous: %d -> %d", previous.get(), hash));
                    previous.set(hash);
                }

                receiver.expectedConfirmations.add(hash);
                log.info(String.format("send %d/%d packet", i, count));

                packet.rewind();
                sendDatagram(packet);
                Thread.sleep(RESEND_DELAY);
                while (!closed && receiver.expectedConfirmations.contains(hash) &&
                        System.currentTimeMillis() - start < timeout) {
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
            sendDatagram(packetUtils.writePacket(new Packet(Action.FINISH, previous.get())));
        } catch (Throwable e) {
            log.log(Level.WARNING, "unable to send FINISH packet", e);
        }

        closed = true;
        receiver.thread.interrupt();
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

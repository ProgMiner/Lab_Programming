package ru.byprogminer.Lab6_Programming.udp;

import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

public abstract class UdpServerSocket<D> {

    protected final PacketUtils packetUtils;
    protected final D device;

    private final Queue<SocketAddress> clients = new LinkedList<>();

    private final Logger log = Loggers.getObjectLogger(this);

    public UdpServerSocket(D device, int contentSize) {
        packetUtils = new PacketUtils(contentSize);
        this.device = device;
    }

    public final UdpSocket<?> accept() throws IOException {
        synchronized (clients) {
            while (clients.isEmpty()) {
                receivePacket();
                Thread.yield();
            }

            final SocketAddress address = clients.remove();
            log.info("accepted socket from " + address);

            final UdpSocket<?> socket = makeSocket(packetUtils.contentSize);
            socket.accept(address);
            return socket;
        }
    }

    private void receivePacket() throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(packetUtils.packetSize);
        final SocketAddress address = receiveDatagram(buffer);
        buffer.flip();

        final ParsedPacket packet = packetUtils.parsePacket(buffer);
        if (packet == null || packet.action != Action.CONNECT) {
            return;
        }

        clients.add(address);
    }

    public D getDevice() {
        return device;
    }

    protected abstract SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException;
    protected abstract UdpSocket<?> makeSocket(int packetSize) throws IOException;
}

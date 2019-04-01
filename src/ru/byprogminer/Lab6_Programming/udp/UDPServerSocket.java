package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.Queue;

import static ru.byprogminer.Lab6_Programming.udp.UDPSocket.HEADER_SIZE;

public abstract class UDPServerSocket<D> {

    protected final int packetSize;
    protected final D device;

    private final Queue<SocketAddress> clients = new LinkedList<>();

    public UDPServerSocket(D device, int packetSize) {
        this.packetSize = packetSize;
        this.device = device;
    }

    public static <D extends DatagramChannel> ChannelUDPServerSocket<D> by(D device, int packetSize) {
        return new ChannelUDPServerSocket<>(device, packetSize);
    }

    public final UDPSocket<D> accept() throws IOException {
        synchronized (clients) {
            while (clients.isEmpty()) {
                receivePacket();
            }

            final SocketAddress address = clients.remove();
            final UDPSocket<D> socket = makeSocket(device, packetSize);
            socket.accept(address);
            return socket;
        }
    }

    private void receivePacket() throws IOException {
        final ByteBuffer packet = ByteBuffer.allocate(HEADER_SIZE + packetSize);
        final SocketAddress address = receiveDatagram(packet);

        if (packet.position() < HEADER_SIZE) {
            return;
        }

        packet.flip();
        final Action action = Action.by(packet.get());
        packet.get();
        packet.getLong();

        if (action != Action.CONNECT) {
            return;
        }

        clients.add(address);
    }

    protected abstract SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException;
    protected abstract UDPSocket<D> makeSocket(D device, int packetSize);
}

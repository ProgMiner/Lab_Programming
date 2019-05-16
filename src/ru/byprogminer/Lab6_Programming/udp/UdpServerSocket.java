package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.Queue;

import static ru.byprogminer.Lab6_Programming.udp.UdpSocket.HEADER_SIZE;

public abstract class UdpServerSocket<D> {

    protected final int packetSize;
    protected final D device;

    private final Queue<SocketAddress> clients = new LinkedList<>();

    public UdpServerSocket(D device, int packetSize) {
        this.packetSize = packetSize;
        this.device = device;
    }

    public static <D extends DatagramChannel> ChannelUdpServerSocket<D> by(D device, int packetSize) {
        return new ChannelUdpServerSocket<>(device, packetSize);
    }

    public final UdpSocket<D> accept() throws IOException {
        synchronized (clients) {
            while (clients.isEmpty()) {
                receivePacket();
            }

            final SocketAddress address = clients.remove();
            final UdpSocket<D> socket = makeSocket(device, packetSize);
            socket.accept(address);
            return socket;
        }
    }

    private void receivePacket() throws IOException {
        final ByteBuffer packet = ByteBuffer.allocate(HEADER_SIZE + packetSize);
        final SocketAddress address = receiveDatagram(packet);
        packet.flip();

        if (packet.remaining() < HEADER_SIZE || packet.getInt() != UdpSocket.SIGNATURE) {
            return;
        }

        if (Action.by(packet.get()) != Action.CONNECT) {
            return;
        }

        clients.add(address);
    }

    public D getDevice() {
        return device;
    }

    protected abstract SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException;
    protected abstract UdpSocket<D> makeSocket(D device, int packetSize);
}

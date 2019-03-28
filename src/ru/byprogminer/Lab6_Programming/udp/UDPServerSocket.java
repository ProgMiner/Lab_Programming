package ru.byprogminer.Lab6_Programming.udp;

import ru.byprogminer.Lab6_Programming.PriorityThing;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.PriorityQueue;

import static ru.byprogminer.Lab6_Programming.udp.UDPSocket.HEADER_SIZE;

public abstract class UDPServerSocket<D> {

    protected final int packetSize;
    protected final D device;

    private final PriorityQueue<PriorityThing<Long, SocketAddress>> clients = new PriorityQueue<>();

    public UDPServerSocket(D device, int packetSize) {
        this.packetSize = packetSize;
        this.device = device;
    }

    public static <D extends DatagramChannel> ChannelUDPServerSocket<D> by(D device, int packetSize) {
        return new ChannelUDPServerSocket<>(device, packetSize);
    }

    public final synchronized UDPSocket<D> accept() throws IOException {
        while (clients.isEmpty()) {
            receivePacket();
        }

        final SocketAddress address = clients.remove().getThing();
        return makeSocket(device, packetSize, address);
    }

    private synchronized void receivePacket() throws IOException {
        final ByteBuffer packet = ByteBuffer.allocate(HEADER_SIZE + packetSize);
        final SocketAddress address = receiveDatagram(packet);

        if (packet.position() < HEADER_SIZE) {
            return;
        }

        packet.flip();
        final Action action = Action.by(packet.get());
        packet.get();
        packet.get();
        packet.getLong();
        final Long time = packet.getLong();

        if (action != Action.CONNECT) {
            return;
        }

        clients.add(new PriorityThing<>(time, address));
    }

    protected abstract SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException;
    protected abstract UDPSocket<D> makeSocket(D device, int packetSize, SocketAddress address);
}

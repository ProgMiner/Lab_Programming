package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class ChannelUDPServerSocket<D extends DatagramChannel> extends UDPServerSocket<D> {

    public ChannelUDPServerSocket(D device, int packetSize) {
        super(device, packetSize);
    }

    @Override
    protected SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException {
        SocketAddress ret;

        do {
            ret = device.receive(buffer);
        } while (ret == null);

        return ret;
    }

    @Override
    protected UDPSocket<D> makeSocket(D device, int packetSize) {
        return new ChannelUDPSocket<>(device, packetSize);
    }
}

package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class ChannelUdpServerSocket<D extends DatagramChannel> extends UdpServerSocket<D> {

    public ChannelUdpServerSocket(D device, int packetSize) {
        super(device, packetSize);
    }

    @Override
    protected SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException {
        SocketAddress ret;

        while ((ret = device.receive(buffer)) == null) {
            Thread.yield();
        }

        return ret;
    }

    @Override
    protected UdpSocket<D> makeSocket(D device, int packetSize) {
        return new ChannelUdpSocket<>(device, packetSize);
    }
}

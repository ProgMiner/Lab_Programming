package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class ChannelUdpServerSocket<D extends DatagramChannel> extends UdpServerSocket<D> {

    public ChannelUdpServerSocket(D device, int contentSize) {
        super(device, contentSize);
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
    protected UdpSocket<DatagramChannel> makeSocket(int contentSize) throws IOException {
        return new ChannelUdpSocket<>(DatagramChannel.open(), contentSize);
    }
}

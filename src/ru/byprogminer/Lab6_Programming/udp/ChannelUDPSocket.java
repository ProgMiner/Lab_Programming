package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class ChannelUDPSocket<D extends DatagramChannel> extends UDPSocket<D> {

    public ChannelUDPSocket(D device, int packetSize) {
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
    protected void sendDatagram(ByteBuffer content) throws IOException {
        device.send(content, remote);
    }
}

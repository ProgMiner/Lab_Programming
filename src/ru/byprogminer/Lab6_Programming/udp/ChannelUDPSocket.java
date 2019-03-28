package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class ChannelUDPSocket<D extends DatagramChannel> extends UDPSocket<D> {

    private static final long RECEIVING_DELAY = 100;

    public ChannelUDPSocket(D device, int packetSize) {
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
    protected void sendDatagram(ByteBuffer content) throws IOException {
        device.send(content, remote);
    }
}

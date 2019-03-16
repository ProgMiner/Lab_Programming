package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class ChannelPacketSender<S extends DatagramChannel> extends AbstractPacketSender<S> implements PacketSender {

    public ChannelPacketSender(S source, int partSize) {
        super(source, partSize);
    }

    @Override
    protected void sendDatagram(ByteBuffer buffer, SocketAddress to) throws IOException {
        source.send(buffer, to);
    }
}

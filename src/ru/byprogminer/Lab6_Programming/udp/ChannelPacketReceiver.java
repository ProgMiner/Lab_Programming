package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class ChannelPacketReceiver<S extends DatagramChannel> extends AbstractPacketReceiver<S> implements PacketReceiver {

    public ChannelPacketReceiver(S source, int partSize) {
        super(source, partSize);
    }

    @Override
    protected SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException {
        SocketAddress ret;

        do {
            ret = source.receive(buffer);
        } while (ret == null);

        return ret;
    }
}

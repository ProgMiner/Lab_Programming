package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class ChannelPacketSender<D extends DatagramChannel> extends AbstractPacketSender<D> implements PacketSender {

    public ChannelPacketSender(D device, int partSize) {
        super(device, partSize);
    }

    @Override
    protected void sendDatagram(ByteBuffer buffer, SocketAddress to) throws IOException {
        device.send(buffer, to);
    }
}

package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class ChannelPacketReceiver<D extends DatagramChannel> extends AbstractPacketReceiver<D> implements PacketReceiver {

    public ChannelPacketReceiver(D device, int partSize) {
        super(device, partSize);
    }

    @Override
    protected SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException {
        SocketAddress ret;

        do {
            ret = device.receive(buffer);
        } while (ret == null);

        return ret;
    }
}

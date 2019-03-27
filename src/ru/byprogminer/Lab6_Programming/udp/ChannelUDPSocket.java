package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class ChannelUDPSocket extends UDPSocket<DatagramChannel> {

    private static final long RECEIVING_DELAY = 100;

    public ChannelUDPSocket(DatagramChannel device, int partSize, SocketAddress remote) {
        super(device, partSize, remote);
    }

    @Override
    protected ByteBuffer receiveDatagram(int size) throws IOException {
        final ByteBuffer content = ByteBuffer.allocate(size);

        while (device.receive(content) == null) {
            try {
                Thread.sleep(RECEIVING_DELAY);
            } catch (InterruptedException ignored) {}
        }

        return content;
    }

    @Override
    protected void sendDatagram(ByteBuffer content) throws IOException {
        device.send(content, remote);
    }
}

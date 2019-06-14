package ru.byprogminer.Lab6_Programming.udp;

import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.logging.Logger;

public class ChannelUdpSocket<D extends DatagramChannel> extends UdpSocket<D> {

    private final Logger log = Loggers.getObjectLogger(this);

    public ChannelUdpSocket(D device, int contentSize) {
        super(device, contentSize);
    }

    @Override
    protected SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException {
        SocketAddress ret;

        while ((ret = device.receive(buffer)) == null) {
            Thread.yield();
        }

        log.info("received datagram from " + ret);
        return ret;
    }

    @Override
    protected void sendDatagram(ByteBuffer content) throws IOException {
        if (remote == null) {
            return;
        }

        device.send(content, remote);

        log.info("sent datagram to " + remote);
    }
}

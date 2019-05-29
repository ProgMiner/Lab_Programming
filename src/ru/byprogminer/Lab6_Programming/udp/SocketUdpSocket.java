package ru.byprogminer.Lab6_Programming.udp;

import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class SocketUdpSocket<D extends DatagramSocket> extends UdpSocket<D> {

    private final Logger log = Loggers.getObjectLogger(this);

    public SocketUdpSocket(D device, int contentSize) {
        super(device, contentSize);
    }

    @Override
    protected SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException {
        byte[] bufferArray = new byte[buffer.remaining()];

        DatagramPacket packet = new DatagramPacket(bufferArray, bufferArray.length);
        device.receive(packet);

        log.info("received datagram from " + packet.getSocketAddress());
        buffer.put(bufferArray, 0, packet.getLength());
        return packet.getSocketAddress();
    }

    @Override
    protected void sendDatagram(ByteBuffer content) throws IOException {
        byte[] bufferArray = new byte[content.remaining()];
        content.get(bufferArray);

        DatagramPacket packet = new DatagramPacket(bufferArray, bufferArray.length, remote);
        device.send(packet);

        log.info("sent datagram to " + remote);
    }
}

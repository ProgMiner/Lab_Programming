package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class SocketUdpSocket<D extends DatagramSocket> extends UdpSocket<D> {

    public SocketUdpSocket(D device, int packetSize) {
        super(device, packetSize);
    }

    @Override
    protected SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException {
        byte[] bufferArray = new byte[buffer.remaining()];

        DatagramPacket packet = new DatagramPacket(bufferArray, bufferArray.length);
        device.receive(packet);

        buffer.put(bufferArray, 0, packet.getLength());
        return packet.getSocketAddress();
    }

    @Override
    protected void sendDatagram(ByteBuffer content) throws IOException {
        byte[] bufferArray = new byte[content.remaining()];
        content.get(bufferArray);

        DatagramPacket packet = new DatagramPacket(bufferArray, bufferArray.length, remote);
        device.send(packet);
    }
}

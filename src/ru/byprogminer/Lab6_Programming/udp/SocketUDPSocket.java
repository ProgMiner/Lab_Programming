package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class SocketUDPSocket extends UDPSocket<DatagramSocket> {

    public SocketUDPSocket(DatagramSocket device, int partSize, SocketAddress remote) {
        super(device, partSize, remote);
    }

    @Override
    protected ByteBuffer receiveDatagram(int size) throws IOException {
        byte[] buffer = new byte[size];

        DatagramPacket packet = new DatagramPacket(buffer, size);
        device.receive(packet);

        return ByteBuffer.wrap(buffer);
    }

    @Override
    protected void sendDatagram(ByteBuffer content) throws IOException {
        byte[] bufferArray = new byte[content.remaining()];
        content.get(bufferArray);

        DatagramPacket packet = new DatagramPacket(bufferArray, bufferArray.length, remote);
        device.send(packet);
    }
}

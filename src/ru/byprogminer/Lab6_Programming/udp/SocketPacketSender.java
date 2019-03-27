package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class SocketPacketSender<D extends DatagramSocket> extends AbstractPacketSender<D> implements PacketSender {

    public SocketPacketSender(D device, int partSize) {
        super(device, partSize);
    }

    @Override
    protected void sendDatagram(ByteBuffer buffer, SocketAddress to) throws IOException {
        byte[] bufferArray = new byte[buffer.remaining()];
        buffer.get(bufferArray);

        DatagramPacket packet = new DatagramPacket(bufferArray, bufferArray.length, to);
        device.send(packet);
    }
}

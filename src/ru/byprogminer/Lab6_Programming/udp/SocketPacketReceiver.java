package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class SocketPacketReceiver <S extends DatagramSocket> extends AbstractPacketReceiver<S> implements PacketReceiver {

    public SocketPacketReceiver(S source, int partSize) {
        super(source, partSize);
    }

    @Override
    protected SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException {
        byte[] bufferArray = new byte[buffer.remaining()];

        DatagramPacket packet = new DatagramPacket(bufferArray, bufferArray.length);
        source.receive(packet);

        buffer.put(bufferArray, 0, packet.getLength());
        return packet.getSocketAddress();
    }
}

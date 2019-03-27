package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class SocketPacketReceiver <D extends DatagramSocket> extends AbstractPacketReceiver<D> implements PacketReceiver {

    public SocketPacketReceiver(D device, int partSize) {
        super(device, partSize);
    }

    @Override
    protected SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException {
        byte[] bufferArray = new byte[buffer.remaining()];

        DatagramPacket packet = new DatagramPacket(bufferArray, bufferArray.length);
        device.receive(packet);

        buffer.put(bufferArray, 0, packet.getLength());
        return packet.getSocketAddress();
    }
}

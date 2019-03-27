package ru.byprogminer.Lab6_Programming.udp;

import ru.byprogminer.Lab6_Programming.Packet;
import ru.byprogminer.Lab6_Programming.Pair;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

public interface PacketReceiver {

    static <T extends DatagramSocket> SocketPacketReceiver<T> by(T device, int partSize) {
        return new SocketPacketReceiver<>(device, partSize);
    }

    static <T extends DatagramChannel> ChannelPacketReceiver<T> by(T device, int partSize) {
        return new ChannelPacketReceiver<>(device, partSize);
    }

    Pair<Packet, SocketAddress> receive() throws IOException, ClassNotFoundException;
}

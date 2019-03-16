package ru.byprogminer.Lab6_Programming.udp;

import ru.byprogminer.Lab6_Programming.Packet;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

public interface PacketSender {

    static <T extends DatagramSocket> SocketPacketSender<T> by(T source, int partSize) {
        return new SocketPacketSender<>(source, partSize);
    }

    static <T extends DatagramChannel> ChannelPacketSender<T> by(T source, int partSize) {
        return new ChannelPacketSender<>(source, partSize);
    }

    void send(Packet packet, SocketAddress to) throws IOException;
}

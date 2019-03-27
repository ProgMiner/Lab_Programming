package ru.byprogminer.Lab6_Programming.udp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

public interface PacketSender {

    static <T extends DatagramSocket> SocketPacketSender<T> by(T device, int partSize) {
        return new SocketPacketSender<>(device, partSize);
    }

    static <T extends DatagramChannel> ChannelPacketSender<T> by(T device, int partSize) {
        return new ChannelPacketSender<>(device, partSize);
    }

    void send(Object data, SocketAddress to) throws IOException;
}

package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab5_Programming.Main;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Comparator;
import java.util.stream.Collectors;

public class Server<C extends DatagramChannel> implements Runnable {

    private final Main main;
    private final C channel;
    private final PacketSender sender;
    private final PacketReceiver receiver;

    private class ClientWorker implements Runnable {

        private SocketAddress address;
        private final Packet packet;

        public ClientWorker(Packet packet, SocketAddress address) {
            this.address = address;
            this.packet = packet;
        }

        public ClientWorker(Pair<Packet, SocketAddress> receivedPacket) {
            this(receivedPacket.getA(), receivedPacket.getB());
        }

        @Override
        public void run() {
            try {
                if (packet instanceof Packet.Request.CurrentState) {
                    sendCurrentState();
                }
            } catch (IOException ignored) {}
        }

        private void sendCurrentState() throws IOException {
            sender.send(new Packet.Response.CurrentState(main.getLivingObjects().parallelStream()
                    .sorted(Comparator.comparing(LivingObject::getName))
                    .collect(Collectors.toList())), address);
        }
    }

    public Server(Main main, C channel, int partSize) {
        this.main = main;
        this.channel = channel;
        sender = PacketSender.by(channel, partSize);
        receiver = PacketReceiver.by(channel, partSize);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                new Thread(new ClientWorker(receiver.receive())).start();
            } catch (Throwable ignored) {}
        }
    }

    public DatagramChannel getChannel() {
        return channel;
    }
}

package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab5_Programming.Main;
import ru.byprogminer.Lab6_Programming.udp.UDPServerSocket;
import ru.byprogminer.Lab6_Programming.udp.UDPSocket;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Comparator;
import java.util.stream.Collectors;

public class Server<C extends DatagramChannel> implements Runnable {

    private final Main main;
    private final C channel;
    private final UDPServerSocket<C> serverSocket;

    private class ClientWorker implements Runnable {

        private final UDPSocket socket;

        public ClientWorker(UDPSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // TODO
        }
    }

    public Server(Main main, C channel, int partSize) {
        this.main = main;
        this.channel = channel;
        serverSocket = UDPServerSocket.by(channel, partSize);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                new Thread(new ClientWorker(serverSocket.accept())).start();
            } catch (Throwable ignored) {}
        }
    }

    public DatagramChannel getChannel() {
        return channel;
    }
}

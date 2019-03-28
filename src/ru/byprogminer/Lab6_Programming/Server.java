package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab5_Programming.CommandRunner;
import ru.byprogminer.Lab5_Programming.Console;
import ru.byprogminer.Lab5_Programming.Main;
import ru.byprogminer.Lab6_Programming.udp.UDPServerSocket;
import ru.byprogminer.Lab6_Programming.udp.UDPSocket;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.channels.DatagramChannel;

public class Server<C extends DatagramChannel> implements Runnable {

    private final Main main;
    private final C channel;
    private final UDPServerSocket<C> serverSocket;

    private class ClientWorker implements Runnable {

        private final UDPSocket<?> socket;

        public ClientWorker(UDPSocket<?> socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                final PipedInputStream out = new PipedInputStream();
                final PipedOutputStream in = new PipedOutputStream();
                final Console console = new Console(
                        CommandRunner.getCommandRunner(main),
                        new PipedInputStream(in),
                        new PrintStream(new PipedOutputStream(out))
                );

                new Thread(console::exec).start();
                while (!Thread.currentThread().isInterrupted() && !console.isRunning());
                if (Thread.interrupted()) {
                    return;
                }

                final Thread outputThread = new Thread(() -> {
                    while (!Thread.interrupted()) {
                        try {
                            if (out.available() == 0) {
                                Thread.sleep(10);
                                continue;
                            }

                            final byte[] content = new byte[out.available()];
                            out.read(content);

                            socket.send(new Packet.Response.ConsoleOutput(content));
                        } catch (IOException | InterruptedException ignored) {}
                    }
                });

                outputThread.start();
                while (!Thread.interrupted() && console.isRunning()) {
                    try {
                        final Packet packet = socket.receive(Packet.class);

                        if (packet instanceof Packet.Request.ConsoleInput) {
                            in.write(((Packet.Request.ConsoleInput) packet).getContent());
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            } catch (Throwable ignored) {
                ignored.printStackTrace();
            }
        }
    }

    public DatagramChannel getChannel() {
        return channel;
    }
}

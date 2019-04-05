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
                while (!console.isRunning());

                final Thread outputThread = new Thread(() -> {
                    while (true) {
                        try {
                            final byte[] buffer = new byte[out.available()];
                            if (buffer.length == 0) {
                                Thread.yield();
                                continue;
                            }

                            out.read(buffer);
                            socket.send(new Packet.Response.ConsoleOutput(buffer));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                outputThread.start();
                while (console.isRunning()) {
                    final Packet packet = socket.receive(Packet.class);

                    if (packet instanceof Packet.Request.ConsoleInput) {
                        in.write(((Packet.Request.ConsoleInput) packet).getContent());
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
        while (true) {
            try {
                new Thread(new ClientWorker(serverSocket.accept())).start();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public DatagramChannel getChannel() {
        return channel;
    }
}

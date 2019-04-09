package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab5_Programming.Console;
import ru.byprogminer.Lab5_Programming.Main;
import ru.byprogminer.Lab5_Programming.ReflectionCommandRunner;
import ru.byprogminer.Lab6_Programming.udp.UDPServerSocket;
import ru.byprogminer.Lab6_Programming.udp.UDPSocket;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;

public class Server<C extends DatagramChannel> implements Runnable {

    private final Main main;
    private final C channel;
    private final UDPServerSocket<C> serverSocket;

    private class ClientWorker implements Runnable {

        private final UDPSocket<?> socket;

        public ClientWorker(UDPSocket<?> socket) {
            this.socket = socket;

            final DatagramSocket datagramSocket;
            final Object device = socket.getDevice();
            if (device instanceof DatagramChannel) {
                datagramSocket = ((DatagramChannel) device).socket();
            } else if (device instanceof DatagramSocket) {
                datagramSocket = (DatagramSocket) device;
            } else {
                return;
            }

            try {
                datagramSocket.setSoTimeout(3000);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            final PipedInputStream out = new PipedInputStream();
            final PipedOutputStream in = new PipedOutputStream();
            try {
                final Console console = new Console(
                        ReflectionCommandRunner.make(main),
                        new PipedInputStream(in),
                        new PrintStream(new PipedOutputStream(out))
                );

                new Thread(console::exec).start();
                while (!console.isRunning()) {
                    Thread.yield();
                }

                final Thread outputThread = new Thread(() -> {
                    while (console.isRunning() && !socket.isClosed()) {
                        try {
                            final int available = out.available();
                            if (available == 0) {
                                Thread.yield();
                                continue;
                            }

                            final byte[] buffer = new byte[available];
                            out.read(buffer);

                            socket.send(new Packet.Response.ConsoleOutput(buffer));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                outputThread.start();
                while (console.isRunning() && !socket.isClosed()) {
                    try {
                        final Packet packet = socket.receive(Packet.class, 500);

                        if (packet instanceof Packet.Request.ConsoleInput) {
                            in.write(((Packet.Request.ConsoleInput) packet).getContent());
                        }
                    } catch (SocketTimeoutException ignored) {
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    out.close();
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

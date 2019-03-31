package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab6_Programming.udp.SocketUDPSocket;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

public class Main {

    public static final int PART_SIZE = 10240;
    public static final int CONNECT_DELAY = 3000;

    private static final String USAGE = "Usage: java -jar lab6_client.jar <port> [server]\n" +
            "  - port\n" +
            "    Port number\n" +
            "  - server\n" +
            "    Not required server address";

    public static void main(String[] args) {
        // Check is argument provided

        if (args.length < 1) {
            System.err.println("Port is not provided");
            System.err.println(USAGE);
            System.exit(1);
        }

        try {
            final int port = Integer.parseInt(args[0]);

            final SocketAddress address;
            if (args.length > 1) {
                address = new InetSocketAddress(args[1], port);
            } else {
                address = new InetSocketAddress(port);
            }

            final DatagramSocket socket = new DatagramSocket();
            final SocketUDPSocket<DatagramSocket> udpSocket = new SocketUDPSocket<>(socket, PART_SIZE);

            socket.setSoTimeout(CONNECT_DELAY);
            while (true) {
                try {
                    udpSocket.connect(address);
                } catch (SocketTimeoutException e) {
                    System.out.println("Server is unavailable. Retry");
                    continue;
                }

                break;
            }
            socket.setSoTimeout(0);

            final Thread outputThread = new Thread(() -> {
                while (true) {
                    try {
                        final Packet packet = udpSocket.receive(Packet.class);

                        if (packet instanceof Packet.Response.ConsoleOutput) {
                            System.out.write(((Packet.Response.ConsoleOutput) packet).getContent());
                        }
                    } catch (IOException | ClassNotFoundException ignored) {}
                }
            });
            outputThread.start();

            while (true) {
                try {
                    final int available = System.in.available();
                    if (available == 0) {
                        Thread.sleep(1);
                        continue;
                    }

                    final byte[] buffer = new byte[available];
                    System.in.read(buffer, 0, available);

                    udpSocket.sendCaring(new Packet.Request.ConsoleInput(buffer));
                } catch (Throwable ignored) {}
            }

            // System.exit(0);
        } catch (Throwable e) {
            System.err.printf("Execution error: %s\n", e.getMessage());
            System.err.println(USAGE);
            System.exit(2);
        }
    }
}

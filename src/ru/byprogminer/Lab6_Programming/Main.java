package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab5_Programming.command.CallableCommandRunner;
import ru.byprogminer.Lab5_Programming.command.Console;
import ru.byprogminer.Lab6_Programming.udp.SocketUDPSocket;
import ru.byprogminer.Lab6_Programming.udp.UDPSocket;

import java.io.IOException;
import java.net.*;

import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;

public class Main {

    public final static int PART_SIZE = 1024;
    public final static int CONNECT_DELAY = 3000;

    private static final String USAGE = "Usage: java -jar lab6_client.jar <port> [server]\n" +
            "  - port\n" +
            "    Port number\n" +
            "  - server\n" +
            "    Not required server address";

    private final static CallableCommandRunner commandRunner = new CallableCommandRunner();

    private volatile static UDPSocket<DatagramPacket> socket = null;

    static {
        commandRunner
                .command("add").usage("add <element>")
                .description("Adds element to collection.\n" +
                        "Element represents in JSON and must have a 'name' field")
                .callable(arrayOf(String.class, Console.class), args -> {
                    assertSocketCreated();

                    // TODO
                }).save();
    }

    public static void main(String[] args) {
        // Check is argument provided

        if (args.length < 1) {
            System.err.println("Port is not provided");
            System.err.println(USAGE);
            System.exit(1);
        }

        final SocketUDPSocket<DatagramSocket> udpSocket;
        try {
            final int port = Integer.parseInt(args[0]);

            final SocketAddress address;
            if (args.length > 1) {
                address = new InetSocketAddress(args[1], port);
            } else {
                address = new InetSocketAddress(port);
            }

            final DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(3000);

            udpSocket = new SocketUDPSocket<>(socket, PART_SIZE);

            do {
                try {
                    udpSocket.connect(address, CONNECT_DELAY);
                } catch (SocketTimeoutException e) {
                    System.out.println("Server is unavailable. Retry");
                }
            } while (!udpSocket.isConnected());
        } catch (Throwable e) {
            System.err.printf("Execution error: %s\n", e.getMessage());
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        new Thread(() -> {
            while (!udpSocket.isClosed()) {
                try {
                    final Packet packet = udpSocket.receive(Packet.class, 600000);

                    if (packet instanceof Packet.Response.ConsoleOutput) {
                        System.out.write(((Packet.Response.ConsoleOutput) packet).getContent());
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("It looks like the server is unavailable.");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        try {
            while (!udpSocket.isClosed()) {
                try {
                    final int bufferStart = System.in.read();
                    if (bufferStart < 0) {
                        break;
                    }

                    final byte[] buffer = new byte[System.in.available() + 1];
                    System.in.read(buffer, 1, System.in.available());
                    buffer[0] = (byte) (bufferStart & 0xFF);

                    udpSocket.send(new Packet.Request.ConsoleInput(buffer));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            try {
                udpSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.exit(0);
    }

    private static void assertSocketCreated() {
        if (socket == null) {
            throw new IllegalStateException("socket isn't created");
        }
    }
}

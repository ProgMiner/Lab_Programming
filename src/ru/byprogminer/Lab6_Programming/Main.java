package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab5_Programming.command.CallableCommandRunner;
import ru.byprogminer.Lab5_Programming.command.Console;
import ru.byprogminer.Lab5_Programming.command.ListCommandRunner.Invokable;
import ru.byprogminer.Lab6_Programming.Packet.Response;
import ru.byprogminer.Lab6_Programming.udp.SocketUDPSocket;
import ru.byprogminer.Lab6_Programming.udp.UDPSocket;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.function.Function;

import static ru.byprogminer.Lab5_Programming.LabUtils.*;

public class Main {

    public static final int PART_SIZE = 1024;
    public static final int CONNECT_DELAY = 3000;
    public static final long CLIENT_TIMEOUT = 10000;

    private static final String USAGE = "Usage: java -jar lab6_client.jar <port> [server]\n" +
            "  - port\n" +
            "    Port number\n" +
            "  - server\n" +
            "    Not required server address";

    private static final CallableCommandRunner commandRunner = new CallableCommandRunner();

    private volatile static SocketAddress address = null;
    private volatile static UDPSocket<?> socket = null;
    private volatile static Console console = null;

    static {
        commandRunner
                .command("add").usage("add <element>")
                .description("Adds element to collection.\n" +
                        "Element represents in JSON and must have a 'name' field")
                .callable(arrayOf(String.class), sendElement(Packet.Request.Add.class)).save()

                .command("help").usage("help [command]")
                .description("Shows available commands or description of provided command")
                .callable(arrayOf(Console.class), args -> ((Console) args[0]).printHelp(arrayOf()))
                .callable(arrayOf(String.class, Console.class), args ->
                        ((Console) args[1]).printHelp(arrayOf(args[0].toString()))).save()

                .command("exit").description("Exit")
                .callable(arrayOf(Console.class), args -> ((Console) args[0]).quit()).save()

                .command("load").description("Reloads current file on server")
                .callable(arrayOf(), sendSimple(Packet.Request.Load.class)).save()

                .command("ls").description("Alias for `show`")
                .callable(arrayOf(), sendSimple(Packet.Request.Show.class)).save()

                .command("save").description("Saves current file on server")
                .callable(arrayOf(), sendSimple(Packet.Request.Save.class)).save()

                .command("show").description("Shows all elements from the collection")
                .callable(arrayOf(), sendSimple(Packet.Request.Show.class)).save()

                .command("remove_lower").usage("remove_lower <element>")
                .description("Removes all lower than provided element elements from collection.\n" +
                        "Element represents in JSON and must have a 'name' field")
                .callable(arrayOf(String.class), sendElement(Packet.Request.RemoveLower.class)).save()

                .command("remove_greater").usage("remove_greater <element>")
                .description("Removes all greater than provided element elements from collection.\n" +
                        "Element represents in JSON and must have a 'name' field")
                .callable(arrayOf(String.class), sendElement(Packet.Request.RemoveGreater.class)).save()

                .command("remove").usage("remove <element>")
                .description("Removes element from collection.\n" +
                        "Element represents in JSON and must have a 'name' field")
                .callable(arrayOf(String.class), sendElement(Packet.Request.Remove.class)).save()

                .command("info").description("Prints information about collection")
                .callable(arrayOf(), sendSimple(Packet.Request.Info.class)).save()

                .command("import").usage("import <filename>")
                .description("Imports file with name <filename> to the remote collection")
                .callable(arrayOf(String.class), send(throwing().function(args -> {
                    try {
                        return new Packet.Request.Import(Files.readAllBytes(Paths.get(args[0].toString())));
                    } catch (NoSuchFileException e) {
                        console.printError("File " + args[0].toString() + " not found");
                        return null;
                    }
                }))).save();
    }

    public static void main(String[] args) {
        // Check is argument provided

        if (args.length < 1) {
            System.err.println("Port is not provided");
            System.err.println(USAGE);
            System.exit(1);
        }

        try {
            final int port = Integer.parseInt(args[0]);

            if (args.length > 1) {
                address = new InetSocketAddress(args[1], port);
            } else {
                address = new InetSocketAddress(port);
            }

            connect();
        } catch (Throwable e) {
            System.err.printf("Execution error: %s\n", e.getMessage());
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        System.out.println("Connected successfully");

        try {
            console = new Console(commandRunner);
            console.exec();
        } catch (Throwable e) {
            // e.printStackTrace();
        } finally {
            try {
                socket.close();
                System.exit(0);
            } catch (IOException e) {
                // e.printStackTrace();
            }
        }
    }

    private static void assertSocketCreated() {
        if (socket == null) {
            throw new IllegalStateException("socket isn't created");
        }
    }

    private static Invokable send(Function<Object[], Packet> packet) {
        return (args) -> {
            assertSocketCreated();

            try {
                final Packet request = packet.apply(args);
                if (request == null) {
                    return;
                }

                socket.send(request, CLIENT_TIMEOUT);

                while (!socket.isClosed()) {
                    try {
                        final Response response = socket.receive(Response.class, CLIENT_TIMEOUT);

                        if (response instanceof Response.Message) {
                            switch (((Response.Message) response).getStatus()) {
                                case ERR:
                                    console.printError(((Response.Message) response).getContent());
                                    break;
                                case WARN:
                                    console.printWarning(((Response.Message) response).getContent());
                                    break;
                                case OK:
                                    console.print(((Response.Message) response).getContent());
                                    break;
                            }
                        } else if (response instanceof Response.Done) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        // e.printStackTrace();
                    }
                }
            } catch (SocketTimeoutException e) {
                try {
                    System.out.println("It looks like the server is unavailable. Reconnect");

                    connect();

                    System.out.println("Reconnected");
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
            } catch (IOException e) {
                // e.printStackTrace();
            }
        };
    }

    private static void connect() throws IOException {
        final DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.setSoTimeout(3000);

        socket = new SocketUDPSocket<>(datagramSocket, PART_SIZE);

        do {
            try {
                socket.connect(address, CONNECT_DELAY);
            } catch (SocketTimeoutException e) {
                System.out.println("Server is unavailable. Retry");
            }
        } while (!socket.isConnected());
    }

    private static Invokable sendSimple(Class<? extends Packet> packetType) {
        return send(throwing().function(args -> packetType.getConstructor().newInstance()));
    }

    private static Invokable sendElement(Class<? extends Packet> packetType) {
        return send(throwing().function(args -> packetType.getConstructor(LivingObject.class)
                .newInstance(jsonToLivingObject(args[0].toString()))));
    }
}

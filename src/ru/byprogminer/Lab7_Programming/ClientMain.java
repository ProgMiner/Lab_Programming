package ru.byprogminer.Lab7_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab5_Programming.command.CallableCommandRunner;
import ru.byprogminer.Lab5_Programming.command.Console;
import ru.byprogminer.Lab5_Programming.command.ListCommandRunner.Invokable;
import ru.byprogminer.Lab5_Programming.csv.CsvReader;
import ru.byprogminer.Lab5_Programming.csv.CsvReaderWithHeader;
import ru.byprogminer.Lab6_Programming.Packet.Request;
import ru.byprogminer.Lab6_Programming.Packet.Response;
import ru.byprogminer.Lab6_Programming.udp.PacketUtils;
import ru.byprogminer.Lab6_Programming.udp.SocketUdpSocket;
import ru.byprogminer.Lab6_Programming.udp.UdpSocket;
import ru.byprogminer.Lab7_Programming.csv.CsvLivingObjectReader;
import ru.byprogminer.Lab7_Programming.logging.Loggers;
import ru.byprogminer.Lab7_Programming.renderers.ConsoleRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.byprogminer.Lab5_Programming.LabUtils.*;
import static ru.byprogminer.Lab7_Programming.frontends.RemoteFrontend.CLIENT_TIMEOUT;
import static ru.byprogminer.Lab7_Programming.frontends.RemoteFrontend.SO_TIMEOUT;

public class ClientMain {

    private static class Status {

        public static final int UNKNOWN_ERROR = -1;

        private static final int LOGGING_CONFIG_ERROR = 1;
        private static final int PORT_NOT_PROVIDED = 2;
        private static final int BAD_PORT_PROVIDED = 3;
        private static final int CONNECT_ERROR = 4;
    }

    private static final String USAGE = "" +
            "Usage: java -jar lab7_client.jar <port> [address]\n" +
            "  - port\n" +
            "    Port number of remote server\n" +
            "  - hostname\n" +
            "    Not required hostname of remote server";

    private static final int CONNECT_DELAY = 3000;
    public static final int SERVER_TIMEOUT = CLIENT_TIMEOUT / 10;

    private static final Logger log = Loggers.getClassLogger(ClientMain.class);

    private static final Renderer renderer;
    private static final Console console;

    private static final CurrentUser currentUser = new CurrentUser();
    private static SocketAddress address = null;
    private static UdpSocket<?> socket = null;

    static {
        final CallableCommandRunner commandRunner = new CallableCommandRunner();
        console = new Console(commandRunner);
        renderer = new ConsoleRenderer(console);

        commandRunner
                .command("help")
                .usage(Commands.Help.USAGE).description(Commands.Help.DESCRIPTION)
                .callable(arrayOf(String.class), args -> console.printHelp(arrayOf(args[0].toString())))
                .callable(arrayOf(), args -> console.printHelp(arrayOf())).save();

        commandRunner
                .command("exit").description(Commands.Exit.DESCRIPTION)
                .callable(arrayOf(), args -> console.quit()).save();

        commandRunner
                .command("add")
                .usage(Commands.Add.USAGE).description(Commands.Add.DESCRIPTION)
                .callable(arrayOf(String.class), sendElement(Request.Add::new)).save();

        commandRunner
                .command("remove")
                .usage(Commands.Remove.USAGE).description(Commands.Remove.DESCRIPTION)
                .callable(arrayOf(String.class), sendElement(Request.Remove::new)).save();

        commandRunner
                .command("rm")
                .usage(Commands.Rm.USAGE).description(Commands.Rm.DESCRIPTION)
                .callable(arrayOf(String.class), sendElement(Request.Remove::new)).save();

        commandRunner
                .command(Commands.RemoveLower.ALIAS)
                .usage(Commands.RemoveLower.USAGE).description(Commands.RemoveLower.DESCRIPTION)
                .callable(arrayOf(String.class), sendElement(Request.RemoveLower::new)).save();

        commandRunner
                .command(Commands.RemoveGreater.ALIAS)
                .usage(Commands.RemoveGreater.USAGE).description(Commands.RemoveGreater.DESCRIPTION)
                .callable(arrayOf(String.class), sendElement(Request.RemoveGreater::new)).save();

        commandRunner
                .command("info").description(Commands.Info.DESCRIPTION)
                .callable(arrayOf(), sendSimple(Request.Info::new)).save();

        commandRunner
                .command("show")
                .usage(Commands.Show.USAGE).description(Commands.Show.DESCRIPTION)
                .callable(arrayOf(), sendSimple(Request.ShowAll::new))
                .callable(arrayOf(String.class), sendString((countString, credentials) -> {
                    long count;

                    try {
                        count = Long.parseLong(countString);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("count has bad format", e);
                    }

                    return new Request.Show(count, credentials);
                })).save();

        commandRunner
                .command("ls").description(Commands.Ls.DESCRIPTION)
                .callable(arrayOf(), sendSimple(Request.ShowAll::new)).save();

        commandRunner
                .command("save")
                .usage(Commands.Save.USAGE).description(Commands.Save.DESCRIPTION)
                .callable(arrayOf(String.class), sendString(Request.Save::new)).save();

        commandRunner
                .command("load")
                .usage(Commands.Load.USAGE).description(Commands.Load.DESCRIPTION)
                .callable(arrayOf(String.class), sendString(Request.Load::new)).save();

        commandRunner
                .command(Commands.Import.ALIAS)
                .usage(Commands.Import.USAGE).description(Commands.Import.DESCRIPTION)
                .callable(arrayOf(String.class), sendString((filename, credentials) -> {
                    final Scanner scanner;

                    try {
                        scanner = new Scanner(new File(filename));
                    } catch (FileNotFoundException e) {
                        throw new IllegalArgumentException("file not found", e);
                    }

                    return new Request.Import(new CsvLivingObjectReader(new CsvReaderWithHeader(
                            new CsvReader(scanner))).getObjects(), credentials);
                })).save();

        commandRunner
                .command("su")
                .usage(Commands.Su.USAGE).description(Commands.Su.DESCRIPTION)
                .callable(arrayOf(), args -> {
                    final Credentials credentials = currentUser.reset();

                    console.printf("Current user set to %s\n", credentials == null ? "anonymous" : credentials.username);
                }).callable(arrayOf(String.class), args -> {
                    final String username = (String) args[0];

                    console.printf("Enter password of user %s: ", username);
                    final String password = console.getLine();

                    if (password == null) {
                        console.printWarning("Input cancelled");
                        return;
                    }

                    currentUser.set(new Credentials(username, password));
                    console.printf("Current user set to %s\n", username);
                }).save();

        commandRunner
                .command("passwd")
                .usage(Commands.Passwd.USAGE).description(Commands.Passwd.DESCRIPTION)
                .callable(arrayOf(), sendSimple(credentials -> new Request.ChangePassword(console
                        .requestInput("Enter new password: "), credentials)))
                .callable(arrayOf(String.class), sendString((username, credentials) ->
                        new Request.ChangePassword(username, console.requestInput(String
                                .format("Enter new password for user %s: ", username)), credentials))).save();

        commandRunner
                .command("register")
                .usage(Commands.Register.USAGE).description(Commands.Register.DESCRIPTION)
                .callable(arrayOf(String.class), sendString((username, credentials) ->
                        new Request.Register(username, console.requestInput(String
                                .format("Enter E-Mail for user %s: ", username)), credentials))).save();
    }

    public static void main(String[] args) {
        try {
            Loggers.configureLoggers();
        } catch (Throwable e) {
            System.err.println("Unable to start logging!");
            System.exit(Status.LOGGING_CONFIG_ERROR);
        }

        try {
            log.info("Start");
            final int result = throwingMain(parseArguments(args));
            log.info("Finish");

            System.exit(result);
        } catch (Throwable e) {
            System.err.println("An unknown error occurred. See logs for details or try again.");
            log.log(Level.SEVERE, "Unknown error", e);
            System.exit(Status.UNKNOWN_ERROR);
        }
    }

    private static Map<String, Object> parseArguments(String[] args) {
        final Map<String, Object> ret = new HashMap<>();

        if (args.length < 1) {
            log.log(Level.SEVERE, "port isn't provided");
            System.err.println("Port isn't provided.");
            System.err.println(USAGE);

            System.exit(Status.PORT_NOT_PROVIDED);
            return ret;
        }

        try {
            final int port = validatePort(args[0]);
            if (port == 0) {
                throw new IllegalArgumentException("port is not allowed");
            }

            ret.put("port", validatePort(args[0]));
        } catch (Throwable e) {
            log.log(Level.SEVERE, "bad port provided: " + args[0], e);
            System.err.printf("Bad port provided: %s.\n", args[0]);
            System.err.println(USAGE);

            System.exit(Status.BAD_PORT_PROVIDED);
            return ret;
        }

        if (args.length < 2) {
            return ret;
        }

        ret.put("hostname", args[1]);
        return ret;
    }

    private static int throwingMain(Map<String, Object> args) {
        final String hostname = (String) args.get("hostname");
        final int port = (int) args.get("port");

        if (hostname != null) {
            address = new InetSocketAddress(hostname, port);
        } else {
            address = new InetSocketAddress(port);
        }

        try {
            connect();
        } catch (Throwable e) {
            log.log(Level.SEVERE, "unable to connect to the server", e);
            System.err.println("Unable to connect to the server. Check logs for details or try again.");
            return Status.CONNECT_ERROR;
        }

        System.out.println("Successfully connected");

        try {
            console.exec();
        } finally {
            socket.close();
        }

        return 0;
    }

    private static void connect() throws IOException {
        final UdpSocket<?> oldSocket = socket;
        if (oldSocket != null) {
            oldSocket.close();
        }

        final DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.setSoTimeout(SO_TIMEOUT);

        socket = new SocketUdpSocket<>(datagramSocket, PacketUtils.OPTIMAL_PACKET_SIZE);

        do {
            try {
                socket.connect(address, CONNECT_DELAY);
            } catch (SocketTimeoutException e) {
                System.out.println("Server is unavailable. Retry");
            }
        } while (!socket.isConnected());
    }

    private static Invokable send(Function<Object[], Request> packetFunction) {
        return (args) -> {
            if (socket == null || !socket.isConnected() || socket.isClosed()) {
                log.info("illegal socket state");
                reconnect();
            }

            try {
                final Request request = packetFunction.apply(args);
                if (request == null) {
                    return;
                }

                socket.send(request, SERVER_TIMEOUT);
                while (!socket.isClosed()) {
                    try {
                        final Response response = socket.receive(Response.class, SERVER_TIMEOUT);

                        if (response instanceof Response.Done) {
                            final Response.Done doneResponse = (Response.Done) response;

                            renderer.render(doneResponse.view);
                            break;
                        }
                    } catch (InterruptedException e) {
                        log.log(Level.INFO, "exception thrown", e);
                    }
                }
            } catch (SocketTimeoutException e) {
                log.log(Level.INFO, "server is unavailable", e);

                reconnect();
            } catch (InterruptedException | IOException e) {
                log.log(Level.INFO, "exception thrown", e);
            }
        };
    }

    private static void reconnect() {
        try {
            System.out.println("It looks like the server is unavailable. Reconnect");

            connect();

            log.info("reconnected");
            System.out.println("Reconnected");
        } catch (IOException ex) {
            log.log(Level.INFO, "exception thrown", ex);
        }
    }

    private static Invokable sendSimple(Function<Credentials, Request> packetSupplier) {
        return send(args -> packetSupplier.apply(currentUser.get()));
    }

    private static Invokable sendString(BiFunction<String, Credentials, Request> packetFunction) {
        return send(args -> packetFunction.apply((String) args[0], currentUser.get()));
    }

    private static Invokable sendElement(BiFunction<LivingObject, Credentials, Request> packetFunction) {
        return sendString((elementJson, credentials) ->
                packetFunction.apply(jsonToLivingObject(elementJson), credentials));
    }
}

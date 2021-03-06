package ru.byprogminer.Lab7_Programming;

import ru.byprogminer.Lab6_Programming.udp.ChannelUdpServerSocket;
import ru.byprogminer.Lab6_Programming.udp.PacketUtils;
import ru.byprogminer.Lab6_Programming.udp.UdpServerSocket;
import ru.byprogminer.Lab7_Programming.controllers.CollectionController;
import ru.byprogminer.Lab7_Programming.controllers.UsersController;
import ru.byprogminer.Lab7_Programming.frontends.LocalFrontend;
import ru.byprogminer.Lab7_Programming.frontends.RemoteFrontend;
import ru.byprogminer.Lab7_Programming.logging.Loggers;
import ru.byprogminer.Lab7_Programming.models.CollectionModel;
import ru.byprogminer.Lab7_Programming.models.DatabaseCollectionModel;
import ru.byprogminer.Lab7_Programming.models.DatabaseUsersModel;
import ru.byprogminer.Lab7_Programming.models.UsersModel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerMain {

    private static final class Status {

        private static final int UNKNOWN_ERROR = -1;

        private static final int LOGGING_CONFIG_ERROR = 1;
        private static final int BAD_PORT_PROVIDED = 2;
        private static final int CANNOT_REQUEST_DATA_TO_CONNECT_TO_DB = 3;
        private static final int CANNOT_CONNECT_TO_DB = 4;
        private static final int USERS_MODEL_INIT_ERROR = 5;
        private static final int COLLECTION_MODEL_INIT_ERROR = 6;
    }

    private static final String USAGE = "" +
            "Usage: java -jar lab7_server.jar [port]\n" +
            "  - port\n" +
            "    Not required port number for opened server";

    private static final String DB_PROPERTIES = "/db.properties";

    private static final Scanner stdinScanner = new Scanner(System.in);
    private static final Logger log = Loggers.getClassLogger(ServerMain.class);

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

            if (result != 0) {
                System.exit(result);
            }
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Unknown error", e);
            System.err.println("An unknown error occurred. See logs for details or try again.");
            System.exit(Status.UNKNOWN_ERROR);
        }
    }

    private static Map<String, Object> parseArguments(String[] args) {
        final Map<String, Object> ret = new HashMap<>();

        if (args.length < 1) {
            return ret;
        }

        try {
            final Integer port = Integer.parseInt(args[0]);
            if (port > 65536) {
                throw new IllegalArgumentException("port out of range");
            }

            ret.put("port", port);
        } catch (Throwable e) {
            log.log(Level.SEVERE, "bad port provided: " + args[0], e);
            System.err.printf("Bad port provided: %s.\n", args[0]);
            System.err.println(USAGE);

            System.exit(Status.BAD_PORT_PROVIDED);
        }

        return ret;
    }

    private static int throwingMain(Map<String, Object> args) throws RuntimeException {
        final Properties dbProperties = new Properties();

        try {
            dbProperties.load(ServerMain.class.getResourceAsStream(DB_PROPERTIES));
            log.info(String.format("Database properties have been loaded from resource \"%s\"", DB_PROPERTIES));
        } catch (Throwable e) {
            log.log(Level.INFO, "Unable to load default database settings", e);
        }

        final String dbUrl, dbUser, dbPassword;
        try {
            dbUrl = getOrRequest("JDBC URL: ", dbProperties.getProperty("url"));
            dbUser = getOrRequest("JDBC user: ", dbProperties.getProperty("user"));
            dbPassword = getOrRequest("JDBC password: ", dbProperties.getProperty("password"));
        } catch (IllegalStateException e) {
            log.log(Level.SEVERE, "Input cancelled", e);
            return Status.CANNOT_REQUEST_DATA_TO_CONNECT_TO_DB;
        }

        final Connection dbConnection;
        try {
            dbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Unable to connect to database", e);
            System.err.println("Unable to connect to database. Check the URL, the username and the password and try again. Check logs for details.");
            return Status.CANNOT_CONNECT_TO_DB;
        }

        final UsersModel usersModel;
        try {
            usersModel = new DatabaseUsersModel(dbConnection);
        } catch (SQLException e) {
            log.log(Level.SEVERE, "An error occurred while users model initializing", e);
            System.err.println("Unable to setup database connection. Check logs for details or try again.");
            return Status.USERS_MODEL_INIT_ERROR;
        }

        final CollectionModel collectionModel;
        try {
            collectionModel = new DatabaseCollectionModel(dbConnection);
        } catch (SQLException e) {
            log.log(Level.SEVERE, "An error occurred while collection model initializing", e);
            System.err.println("Unable to setup database connection. Check logs for details or try again.");
            return Status.COLLECTION_MODEL_INIT_ERROR;
        }

        final UsersController usersController = new UsersController(usersModel);
        final CollectionController collectionController = new CollectionController(usersModel, collectionModel);

        final LocalFrontend localFrontend = new LocalFrontend(usersController, collectionController);
        final RemoteFrontend remoteFrontend;

        {
            RemoteFrontend tmpRemoteFrontend = null;

            try {
                final Integer port = (Integer) args.get("port");

                final DatagramChannel channel = DatagramChannel.open();
                if (port == null) {
                    channel.bind(null);
                } else {
                    channel.bind(new InetSocketAddress(port));
                }

                final UdpServerSocket<?> serverSocket = new ChannelUdpServerSocket<>(channel, PacketUtils.OPTIMAL_PACKET_SIZE);
                tmpRemoteFrontend = new RemoteFrontend(serverSocket, usersController, collectionController);

                final Thread remoteFrontendThread = new Thread(tmpRemoteFrontend::exec);
                remoteFrontendThread.setName("Remote frontend");
                remoteFrontendThread.start();

                while (remoteFrontendThread.getState() != Thread.State.RUNNABLE) {
                    Thread.yield();
                }

                final SocketAddress address = channel.getLocalAddress();
                if (address instanceof InetSocketAddress) {
                    final InetSocketAddress inetAddress = (InetSocketAddress) address;

                    System.out.printf("Server opened at local port :%d\n", inetAddress.getPort());
                }
            } catch (Throwable e) {
                log.log(Level.INFO, "an error occurred while server starting", e);
                System.out.println("An error occurred while server starting");
            }

            remoteFrontend = tmpRemoteFrontend;
        }

        localFrontend.exec();

        if (remoteFrontend != null) {
            remoteFrontend.stop();
        }

        return 0;
    }

    private static String getOrRequest(String prompt, String value) throws IllegalStateException {
        if (value != null) {
            return value;
        }

        System.out.print(prompt);
        if (stdinScanner.hasNextLine()) {
            return stdinScanner.nextLine();
        }

        throw new IllegalStateException("cannot request required data");
    }
}

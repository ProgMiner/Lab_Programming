package ru.byprogminer.Lab7_Programming;

import ru.byprogminer.Lab7_Programming.controllers.CollectionController;
import ru.byprogminer.Lab7_Programming.frontends.LocalFrontend;
import ru.byprogminer.Lab7_Programming.logging.Loggers;
import ru.byprogminer.Lab7_Programming.models.CollectionModel;
import ru.byprogminer.Lab7_Programming.models.DatabaseCollectionModel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerMain {

    private static final class Status {

        private static final int UNKNOWN_ERROR = -1;
        private static final int LOGGING_CONFIG_ERROR = 1;
        private static final int CANNOT_REQUEST_DATA_TO_CONNECT_TO_DB = 2;
        private static final int CANNOT_CONNECT_TO_DB = 3;
        private static final int COLLECTION_MODEL_INIT_ERROR = 4;
    }

    private static final String DB_PROPERTIES = "/db.properties";

    private static final Scanner stdinScanner = new Scanner(System.in);
    private static final Logger log = Loggers.getLogger(ServerMain.class.getName());

    public static void main(String[] args) {
        try {
            Loggers.configureLoggers();
        } catch (Throwable e) {
            System.err.println("Unable to start logging!");
            System.exit(Status.LOGGING_CONFIG_ERROR);
        }

        try {
            log.info("Start");
            final int result = throwingMain(args);
            log.info("Finish");

            if (result != 0) {
                System.exit(result);
            }
        } catch (Throwable e) {
            System.err.println("An unknown error occurred. See logs for details or try again.");
            log.log(Level.SEVERE, "Unknown error", e);
            System.exit(Status.UNKNOWN_ERROR);
        }
    }

    private static int throwingMain(String[] args) throws RuntimeException {
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
            System.err.println("Unable to connect to database. Check the URL, the username and the password and try again. Check logs for details.");
            log.log(Level.SEVERE, "Unable to connect to database", e);
            return Status.CANNOT_CONNECT_TO_DB;
        }

        final CollectionModel collectionModel;
        try {
            collectionModel = new DatabaseCollectionModel(dbConnection);
        } catch (SQLException e) {
            System.err.println("Unable to setup database connection. Check logs for details or try again.");
            log.log(Level.SEVERE, "An error occurred while collection model initializing", e);
            return Status.COLLECTION_MODEL_INIT_ERROR;
        }

        final CollectionController collectionController = new CollectionController(collectionModel);

        final LocalFrontend localFrontend = new LocalFrontend(collectionController);

        localFrontend.exec();

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

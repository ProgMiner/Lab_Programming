package ru.byprogminer.Lab7_Programming.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class Loggers {

    private static final String LOGGING_PROPERTIES = "/logging.properties";
    private static final Level DEFAULT_LOGLEVEL = Level.ALL;

    private static final LogManager logManager = LogManager.getLogManager();

    private Loggers() {}

    public static void configureLoggers() throws IOException, NullPointerException {
        configureLoggers(Loggers.class.getResourceAsStream(LOGGING_PROPERTIES));
    }

    public static void configureLoggers(InputStream is) throws IOException {
        logManager.readConfiguration(is);
    }

    @Deprecated
    public static Logger getLogger(String name) {
        return makeLogger(name);
    }

    private static Logger makeLogger(String name) {
        final Logger logger = Logger.getLogger(name);
        logger.setLevel(DEFAULT_LOGLEVEL);

        return logger;
    }

    public static Logger getClassLogger(Class<?> clazz) {
        return makeLogger(clazz.getName());
    }

    public static Logger getObjectLogger(Object object) {
        return makeLogger(object.getClass().getName() + "#" + System.identityHashCode(object));
    }
}

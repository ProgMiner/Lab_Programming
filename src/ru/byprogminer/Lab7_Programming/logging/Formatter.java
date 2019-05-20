package ru.byprogminer.Lab7_Programming.logging;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class Formatter extends java.util.logging.Formatter {

    // Format like in the Minecraft
    private static final String DEFAULT_FORMAT = "[%1$tH:%1$tM:%1$tS] [%3$s/%2$s] [%4$s] %5$s%6$s%n";

    private static final String format;

    static {
        final String propertiesFormat = System.getProperty(Formatter.class.getName() + ".format");

        if (propertiesFormat != null && !propertiesFormat.isEmpty()) {
            Throwable throwable = null;

            try {
                String.format(propertiesFormat, new Date(), "", "", Level.ALL, "", "");
            } catch (Throwable e) {
                throwable = e;
            }

            if (throwable == null) {
                format = propertiesFormat;
            } else {
                format = DEFAULT_FORMAT;
            }
        } else {
            format = DEFAULT_FORMAT;
        }
    }

    @Override
    public String format(LogRecord record) {
        final String thrown;

        if (record.getThrown() != null) {
            final CharArrayWriter writer = new CharArrayWriter();

            record.getThrown().printStackTrace(new PrintWriter(writer));
            thrown = '\n' + writer.toString().trim();
        } else {
            thrown = "";
        }

        return String.format(format,
                new Date(),
                record.getLoggerName(),
                Thread.currentThread().getName(),
                record.getLevel(),
                record.getMessage(),
                thrown);
    }
}

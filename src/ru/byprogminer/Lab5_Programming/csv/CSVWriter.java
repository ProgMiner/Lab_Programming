package ru.byprogminer.Lab5_Programming.csv;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

public class CSVWriter extends Writer {

    private final Writer writer;

    public CSVWriter(Writer writer) {
        this.writer = writer;
    }

    public void write(List<String> row) throws IOException {
        writer.append(row.parallelStream()
                .map(CSVWriter::prepareValue)
                .collect(Collectors.joining(",")))
                .append('\n');
        writer.flush();
    }

    private static String prepareValue(final String value) {
        StringBuilder ret = new StringBuilder();

        ret.append(value.replaceAll("\"", "\"\""));
        if (value.contains(" ") || value.contains(",") || value.contains("\"")) {
            return "\"" + ret.toString() + "\"";
        }

        return ret.toString();
    }

    @Override
    public void write(char[] chars, int offset, int length) throws IOException {
        writer.write(chars, offset, length);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    public Writer getWriter() {
        return writer;
    }
}

package ru.byprogminer.Lab5_Programming.csv;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CSVWriterWithHeader extends Writer {

    private final CSVWriter writer;
    private List<String> cols;

    public CSVWriterWithHeader(CSVWriter writer) {
        this.writer = writer;
    }

    public synchronized void write(Map<String, String> row) throws IOException {
        if (cols == null) {
            throw new IllegalStateException("columns are not written");
        }

        writer.write(cols
                .parallelStream().map(row::get)
                .collect(Collectors.toList()));
    }

    public void writeColumns(List<String> columns) throws IOException {
        if (cols != null) {
            throw new IllegalStateException("columns are written already");
        }

        cols = Objects.requireNonNull(columns);
        writer.write(cols);
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

    public CSVWriter getWriter() {
        return writer;
    }

    public List<String> getColumns() {
        return cols;
    }
}

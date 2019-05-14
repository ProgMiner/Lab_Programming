package ru.byprogminer.Lab5_Programming.csv;

import java.util.*;

public class CsvReaderWithHeader implements Iterable<Map<String, String>>, Iterator<Map<String, String>> {

    private final CsvReader reader;
    private List<String> cols = null;

    public CsvReaderWithHeader(CsvReader reader) {
        this.reader = reader;
    }

    @Override
    public synchronized Map<String, String> next() {
        final List<String> line = reader.next();

        if (cols == null) {
            cols = Collections.unmodifiableList(line);
            return next();
        }

        final Map<String, String> row = new HashMap<>();
        for (int i = 0; i < line.size(); i++) {
            row.put(cols.get(i), line.get(i));
        }

        return row;
    }

    @Override
    public synchronized boolean hasNext() {
        if (!reader.hasNext()) {
            return false;
        }

        if (cols == null) {
            cols = Collections.unmodifiableList(reader.next());
        }

        return reader.hasNext();
    }

    @Override
    public Iterator<Map<String, String>> iterator() {
        return this;
    }

    public CsvReader getReader() {
        return reader;
    }

    public List<String> getColumns() {
        return cols;
    }

    public void setColumns(List<String> columns) {
        cols = Objects.requireNonNull(columns);
    }
}

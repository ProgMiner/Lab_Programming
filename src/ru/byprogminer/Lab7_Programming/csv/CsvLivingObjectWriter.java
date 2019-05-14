package ru.byprogminer.Lab7_Programming.csv;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab3_Programming.Object;
import ru.byprogminer.Lab5_Programming.csv.CsvWriterWithHeader;
import ru.byprogminer.Lab7_Programming.LivingObjectWriter;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvLivingObjectWriter implements LivingObjectWriter<IOException>, Closeable {

    private static final List<String> CSV_HEADER;

    private final CsvWriterWithHeader writer;

    private final AtomicInteger currentObject = new AtomicInteger(0);
    private final AtomicInteger currentItem = new AtomicInteger(0);

    static {
        List<String> csvColumns = new ArrayList<>();
        csvColumns.add("type");
        csvColumns.add("id");
        csvColumns.add("key");
        csvColumns.add("value");

        CSV_HEADER = Collections.unmodifiableList(csvColumns);
    }

    public CsvLivingObjectWriter(CsvWriterWithHeader writer) throws IOException {
        this.writer = writer;

        if (writer.getColumns() == null) {
            writer.writeColumns(CSV_HEADER);
        }
    }

    @Override
    public void write(LivingObject livingObject) throws IOException {
        writeObject(livingObject);
    }

    @Override
    public void writeMetadata(String key, String value) throws IOException {
        writeRow("meta", 0, key, value);
    }

    private <T extends LivingObject> void writeObject(T object) throws IOException {
        int id = currentObject.getAndIncrement();

        writeObject(object, "object", id);
        writeRow("object", id, "lives", object.isLives());

        final Set<String> itemIds = new HashSet<>();
        for (Object item : object.getItems()) {
            itemIds.add(Integer.toString(writeItem(item)));
        }

        writeRow("object", id, "items", String.join(CsvLivingObjectReader.ARRAY_SPLITTER, itemIds));
    }

    private <T extends Object> int writeItem(T object) throws IOException {
        final int id = currentItem.getAndIncrement();

        writeObject(object, "item", id);
        return id;
    }

    private <T extends Object> void writeObject(T object, String type, int id) throws IOException {
        Objects.requireNonNull(type);

        writeRow(type, id, "name", object.getName());
        writeRow(type, id, "volume", object.getVolume());
        writeRow(type, id, "creatingTime", object.getCreatingTime().toString());
        writeRow(type, id, "x", object.getX());
        writeRow(type, id, "y", object.getY());
        writeRow(type, id, "z", object.getZ());
    }

    private <T> void writeRow(String type, int id, String key, T value) throws IOException {
        final Map<String, String> row = new HashMap<>();

        row.put("type", type);
        row.put("id", Integer.toString(id));
        row.put("key", key);
        row.put("value", value.toString());

        writer.write(row);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}

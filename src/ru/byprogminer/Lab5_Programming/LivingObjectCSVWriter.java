package ru.byprogminer.Lab5_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab3_Programming.Object;
import ru.byprogminer.Lab5_Programming.csv.CSVWriterWithHeader;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.byprogminer.Lab5_Programming.LabUtils.throwing;
import static ru.byprogminer.Lab5_Programming.LivingObjectCSVReader.OWNER_ID_FIELD_NAME;

public class LivingObjectCSVWriter {

    private final static List<String> CSV_HEADER;

    private final CSVWriterWithHeader writer;

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

    public LivingObjectCSVWriter(CSVWriterWithHeader writer) throws IOException {
        this.writer = writer;

        if (writer.getColumns() == null) {
            writer.writeColumns(CSV_HEADER);
        }
    }

    public <T extends LivingObject> void write(T livingObject) throws IOException {
        writeObject(livingObject);
    }

    public void writeMetadata(String key, String value) throws IOException {
        writeRow("meta", 0, key, value);
    }

    private <T extends LivingObject> void writeObject(T object) throws IOException {
        int id = currentObject.getAndIncrement();

        writeObject(object, "object", id, null);
        writeRow("object", id, "lives", object.isLives());

        object.getItems().parallelStream().forEachOrdered(
                throwing().consumer(item -> writeItem(item, id)));
    }

    private <T extends Object> void writeItem(T object, int ownerId) throws IOException {
        writeObject(object, "item", currentItem.getAndIncrement(), ownerId);
    }

    private <T extends Object> void writeObject(T object, String type, int id, Integer ownerId) throws IOException {
        Objects.requireNonNull(type);

        writeRow(type, id, "name", object.getName());
        writeRow(type, id, "volume", object.getVolume());
        writeRow(type, id, "creatingTime", object.getCreatingTime().toString());
        writeRow(type, id, "x", object.getX());
        writeRow(type, id, "y", object.getY());
        writeRow(type, id, "z", object.getZ());

        if (ownerId != null) {
            writeRow(type, id, OWNER_ID_FIELD_NAME, ownerId);
        }
    }

    private <T> void writeRow(String type, int id, String key, T value) throws IOException {
        final Map<String, String> row = new HashMap<>();

        row.put("type", type);
        row.put("id", Integer.toString(id));
        row.put("key", key);
        row.put("value", value.toString());

        writer.write(row);
    }
}

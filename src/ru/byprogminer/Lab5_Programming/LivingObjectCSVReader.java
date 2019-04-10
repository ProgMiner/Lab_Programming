package ru.byprogminer.Lab5_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab5_Programming.csv.CSVReaderWithHeader;

import java.util.*;

import static ru.byprogminer.Lab5_Programming.LabUtils.*;

public class LivingObjectCSVReader implements Iterable<LivingObject>, Iterator<LivingObject> {

    public static final String OWNER_ID_FIELD_NAME = "__ownerId";

    private final CSVReaderWithHeader reader;

    private final NavigableMap<String, String> metadata = new TreeMap<>();
    private final NavigableMap<Integer, Map<String, String>> objects = new TreeMap<>();
    private final Map<Integer, NavigableMap<Integer, Map<String, String>>> items = new TreeMap<>();

    private final NavigableMap<String, String> metadataImmutable =
            Collections.unmodifiableNavigableMap(metadata);

    public LivingObjectCSVReader(CSVReaderWithHeader reader) {
        this.reader = reader;
    }

    @Override
    public boolean hasNext() {
        readAll();

        return !objects.isEmpty();
    }

    @Override
    public LivingObject next() {
        readAll();

        final Integer livingObjectId = objects.firstKey();
        final Map<String, String> livingObjectProperties = objects.remove(livingObjectId);

        final LivingObject livingObject = mapToObject(livingObjectProperties, LabUtils::livingObjectConstructor);
        callIfNotNull(livingObjectProperties.get("lives"), s -> setLivingObjectLives(livingObject, Boolean.parseBoolean(s)));

        callIfNotNull(items.remove(livingObjectId), items -> items.values().parallelStream()
                .map(item -> mapToObject(item, LabUtils::objectConstructor))
                .forEach(livingObject.getItems()::add));

        return livingObject;
    }

    public NavigableMap<String, String> getMetadata() {
        readAll();

        return metadataImmutable;
    }

    private synchronized void readAll() {
        try {
            final SortedMap<Integer, Map<String, String>> items = new TreeMap<>();

            for (final Map<String, String> sourceRow: reader) {
                final TreeMap<String, String> row = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                row.putAll(sourceRow);

                final Integer id = Integer.parseInt(row.get("id"));
                final String key = Objects.requireNonNull(row.get("key"));
                final String type = Objects.requireNonNull(row.get("type"));
                final String value = Objects.requireNonNull(row.get("value"));
                switch (type) {
                    case "meta":
                        if (metadata.containsKey(key)) {
                            throw new IllegalArgumentException("duplicated key in metadata");
                        }

                        metadata.put(key, value);
                        break;
                    case "object":
                        objects.computeIfAbsent(id, integer ->
                                new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
                                .put(key, value);
                        break;
                    case "item":
                        items.computeIfAbsent(id, integer ->
                                new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
                                .put(key, value);
                }
            }

            for (final Map.Entry<Integer, Map<String, String>> item : items.entrySet()) {
                final Map<String, String> itemProperties = item.getValue();

                final Integer ownerId = Integer.parseInt(itemProperties.remove(OWNER_ID_FIELD_NAME));

                this.items.computeIfAbsent(ownerId, integer -> new TreeMap<>())
                        .putIfAbsent(item.getKey(), itemProperties);
            }
        } catch (IllegalStateException e) {
            return;
        }

        reader.getReader().getScanner().close();
    }

    @Override
    public Iterator<LivingObject> iterator() {
        return this;
    }
}

package ru.byprogminer.Lab7_Programming.csv;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab5_Programming.LabUtils;
import ru.byprogminer.Lab5_Programming.csv.CsvReaderWithHeader;
import ru.byprogminer.Lab7_Programming.LivingObjectReader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static ru.byprogminer.Lab5_Programming.LabUtils.*;

public class CsvLivingObjectReader implements LivingObjectReader {

    public static final String ARRAY_SPLITTER = ";";

    private final CsvReaderWithHeader reader;

    private final Set<LivingObject> objects = new ConcurrentSkipListSet<>();
    private final Set<LivingObject> objectsImmutable = Collections.unmodifiableSet(objects);

    private final Map<String, String> metadata = new ConcurrentHashMap<>();
    private final Map<String, String> metadataImmutable = Collections.unmodifiableMap(metadata);

    public CsvLivingObjectReader(CsvReaderWithHeader reader) {
        this.reader = reader;
    }

    @Override
    public Set<LivingObject> getObjects() {
        readAll();

        return objectsImmutable;
    }

    @Override
    public Map<String, String> getMetadata() {
        readAll();

        return metadataImmutable;
    }

    private synchronized void readAll() {
        final Map<Integer, Map<String, String>> objects = new HashMap<>();
        final Map<Integer, Map<String, String>> items = new HashMap<>();

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

        for (Map<String, String> properties : objects.values()) {
            final LivingObject livingObject = mapToObject(properties, LabUtils::livingObjectConstructor);

            callIfNotNull(properties.get("lives"), s -> setLivingObjectLives(livingObject, Boolean.parseBoolean(s)));

            callIfNotNull(properties.get("items"), s -> Arrays.stream(s.trim().split(ARRAY_SPLITTER))
                    .map(String::trim).filter(s1 -> !s1.isEmpty()).map(s1 -> {
                        try {
                            return Integer.parseInt(s1);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }).filter(Objects::nonNull).map(items::remove).filter(Objects::nonNull)
                    .map(map -> mapToObject(map, LabUtils::objectConstructor))
                    .forEach(livingObject.getItems()::add));

            this.objects.add(livingObject);
        }
    }
}

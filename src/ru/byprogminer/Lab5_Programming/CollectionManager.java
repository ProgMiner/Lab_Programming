package ru.byprogminer.Lab5_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab5_Programming.command.StatusPrinter;
import ru.byprogminer.Lab5_Programming.csv.CSVReader;
import ru.byprogminer.Lab5_Programming.csv.CSVReaderWithHeader;
import ru.byprogminer.Lab5_Programming.csv.CSVWriter;
import ru.byprogminer.Lab5_Programming.csv.CSVWriterWithHeader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static ru.byprogminer.Lab5_Programming.LabUtils.throwing;

public class CollectionManager {

    public final static String COLLECTION_TYPE = "HashSet";

    private final Map<String, String> metadata = Collections.synchronizedMap(new HashMap<>());
    private final Set<LivingObject> livingObjects = Collections.synchronizedSet(new HashSet<>());
    private final NavigableSet<LivingObject> sortedLivingObjects = Collections.synchronizedNavigableSet(new TreeSet<>());

    private final String filename;

    public CollectionManager(String filename) {
        this.filename = Objects.requireNonNull(filename);
    }

    {
        // Set initialize date to current by default
        metadata.put("Initialize date", new Date().toString());
    }

    /**
     * Adds element to collection
     */
    public void add(final LivingObject element, final StatusPrinter printer) {
        tryLoadCSV(printer);

        if (livingObjects.contains(element)) {
            printer.printWarning("specified element is already contains in collection");
            return;
        }

        livingObjects.add(element);
        sortedLivingObjects.add(element);
        trySaveCSV(printer);
    }

    /**
     * Removes all greater than provided element elements from collection
     */
    public void removeGreater(final LivingObject element, final StatusPrinter printer) {
        int counter = 0;

        LivingObject greater;
        while (true) {
            tryLoadCSV(printer);
            greater = sortedLivingObjects.higher(element);

            if (greater == null) {
                break;
            }

            ++counter;
            livingObjects.remove(greater);
            sortedLivingObjects.remove(greater);
            trySaveCSV(printer);
        }

        if (counter == 0) {
            printer.printWarning("no one elements have removed");
        } else {
            printer.printf("%d elements removed", counter);
        }
    }

    /**
     * Shows all elements in collection
     */
    public void show(final StatusPrinter printer) {
        tryLoadCSV(printer);

        livingObjects.parallelStream()
                .map(LivingObject::toString)
                .forEachOrdered(printer::println);
    }

    /**
     * Loads collection from file
     */
    public void load(final StatusPrinter printer) {
        try {
            loadCSV();

            printer.printf("Loaded from %s\n", filename);
        } catch (FileNotFoundException ignored) {
        } catch (Throwable e) {
            printer.printf("Unexpected error: %s\n", e.getMessage());
        }
    }

    /**
     * Saves collection to file
     */
    public void save(final StatusPrinter printer) {
        try {
            saveCSV();

            printer.printf("Saved in %s\n", filename);
        } catch (Throwable e) {
            printer.printf("Unexpected error: %s\n", e.getMessage());
        }
    }

    /**
     * Prints information about collection
     */
    public void info(final StatusPrinter printer) {
        tryLoadCSV(printer);

        printer.printf("Elements in collection: %d\n", livingObjects.size());

        metadata.entrySet().parallelStream().forEachOrdered(field ->
                printer.printf("%s: %s\n", field.getKey(), field.getValue()));
    }

    /**
     * Removes all lower than provided element elements from collection
     */
    public void removeLower(final LivingObject element, final StatusPrinter printer) {
        int counter = 0;

        LivingObject lower;
        while (true) {
            tryLoadCSV(printer);
            lower = sortedLivingObjects.lower(element);

            if (lower == null) {
                break;
            }

            ++counter;
            livingObjects.remove(lower);
            sortedLivingObjects.remove(lower);
            trySaveCSV(printer);
        }

        if (counter == 0) {
            printer.printWarning("no one elements have removed");
        } else {
            printer.printf("%d elements removed", counter);
        }
    }

    /**
     * Removes element from collection
     */
    public void remove(final LivingObject element, final StatusPrinter printer) {
        tryLoadCSV(printer);

        if (!livingObjects.contains(element)) {
            printer.printWarning("specified element isn't contains in collection");
            return;
        }

        livingObjects.remove(element);
        sortedLivingObjects.remove(element);
        trySaveCSV(printer);
    }

    public void loadCSV() throws FileNotFoundException {
        final File file = new File(Objects.requireNonNull(filename));

        if (!file.exists()) {
            throw new FileNotFoundException("file " + filename + " isn't exists");
        }

        String exception = "an error occurred while csv file reading";
        final Map<String, String> metadata = Collections.synchronizedMap(new HashMap<>());
        final Set<LivingObject> livingObjects = Collections.synchronizedSet(new HashSet<>());
        final NavigableSet<LivingObject> sortedLivingObjects = Collections.synchronizedNavigableSet(new TreeSet<>());
        try {
            final LivingObjectCSVReader reader =
                    new LivingObjectCSVReader(new CSVReaderWithHeader(new CSVReader(new Scanner(file))));

            metadata.putAll(reader.getMetadata());
            metadata.put("Collection type", COLLECTION_TYPE);

            for (LivingObject object: reader) {
                livingObjects.add(object);
                sortedLivingObjects.add(object);
            }

            this.metadata.clear();
            this.metadata.putAll(metadata);
            this.livingObjects.clear();
            this.livingObjects.addAll(livingObjects);
            this.sortedLivingObjects.clear();
            this.sortedLivingObjects.addAll(sortedLivingObjects);
        } catch (Throwable e) {
            if (e.getMessage() != null) {
                throw new IllegalArgumentException(exception + ", " + e.getMessage(), e);
            }

            throw new IllegalArgumentException(exception, e);
        }
    }

    public void saveCSV() throws IOException {
        final LivingObjectCSVWriter writer =
                new LivingObjectCSVWriter(new CSVWriterWithHeader(new CSVWriter(new FileWriter(filename))));

        metadata.entrySet().parallelStream().forEachOrdered(throwing().consumer(meta ->
                writer.writeMetadata(meta.getKey(), meta.getValue())));
        livingObjects.parallelStream().forEachOrdered(throwing().consumer(writer::write));
    }

    public String getFilename() {
        return filename;
    }

    private void tryLoadCSV(final StatusPrinter printer) {
        try {
            loadCSV();
        } catch (FileNotFoundException ignored) {
        } catch (Throwable e) {
            printer.printWarning("an error occurred while loading data from file");
        }
    }

    private void trySaveCSV(final StatusPrinter printer) {
        try {
            saveCSV();
        } catch (Throwable e) {
            printer.printWarning("an error occurred while saving data to file");
        }
    }
}

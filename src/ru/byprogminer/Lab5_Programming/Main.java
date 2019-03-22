package ru.byprogminer.Lab5_Programming;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab5_Programming.csv.CSVReader;
import ru.byprogminer.Lab5_Programming.csv.CSVReaderWithHeader;
import ru.byprogminer.Lab5_Programming.csv.CSVWriter;
import ru.byprogminer.Lab5_Programming.csv.CSVWriterWithHeader;
import ru.byprogminer.Lab6_Programming.Server;

import static ru.byprogminer.Lab5_Programming.LabUtils.*;
import static ru.byprogminer.Lab6_Programming.Main.PART_SIZE;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static final String USAGE = "Usage: java -jar lab6_server.jar <filename> [port]\n" +
            "  - filename\n" +
            "    Name of CSV file with LivingObject objects\n" +
            "  - port\n" +
            "    Not required port number for opened server";

    private final Map<String, String> metadata = Collections.synchronizedMap(new HashMap<>());
    private final Set<LivingObject> livingObjects = Collections.synchronizedSet(new HashSet<>());
    private final NavigableSet<LivingObject> sortedLivingObjects = Collections.synchronizedNavigableSet(new TreeSet<>());

    private String filename = null;

    public static void main(final String[] args) {
        try {
            ClassLoader.getSystemClassLoader().loadClass("com.alibaba.fastjson.JSON");
        } catch (ClassNotFoundException e) {
            // Try to load fastjson.jar if it isn't loaded already

            try {
                URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);

                File dir = new File(".");
                Arrays.stream(Objects.requireNonNull(dir.listFiles((file, s) ->
                        s.contains("fastjson") && s.endsWith(".jar")))).parallel()
                        .forEach(throwing().consumer(jar -> method.invoke(classLoader, jar.toURI().toURL())));
            } catch (Throwable ignored) {}

            try {
                ClassLoader.getSystemClassLoader().loadClass("com.alibaba.fastjson.JSON");
            } catch (ClassNotFoundException e1) {
                System.err.println("Cannot start without fastjson.jar");
                System.exit(3);
            }
        }

        // Check is argument provided
        if (args.length < 1) {
            System.err.println("Filename is not provided");
            System.err.println(USAGE);
            System.exit(1);
        }

        final Main main = new Main();
        main.filename = args[0];

        Short port = null;
        if (args.length > 1) {
            try {
                port = Short.parseShort(args[1]);
            } catch (Throwable e) {
                System.err.printf("Bad port provided, %s\n", e.getMessage());
                System.err.println(USAGE);
                System.exit(3);
            }
        }

        final Console console = new Console(CommandRunner.getCommandRunner(main));

        // Try to load file
        try {
            main.loadCSV();
        } catch (final FileNotFoundException e) {
            console.printWarning("file \"" + main.filename + "\" isn't exists. It will be created");
        } catch (final Throwable e) {
            System.err.printf("Execution error: %s\n", e.getMessage());
            System.err.println(USAGE);
            System.exit(2);
        }

        // Delegate control to console
        System.out.println("Lab6_Programming. Type `help` to get help");

        try {
            DatagramChannel serverChannel = DatagramChannel.open();
            if (port == null) {
                serverChannel.bind(null);
            } else {
                serverChannel.bind(new InetSocketAddress(port));
            }

            Server<DatagramChannel> server = new Server<>(main, serverChannel, PART_SIZE);
            Thread serverThread = new Thread(server);
            serverThread.start();

            while (serverThread.getState() != Thread.State.RUNNABLE) {
                System.out.print("");
            }

            SocketAddress address = serverChannel.getLocalAddress();
            if (address instanceof InetSocketAddress) {
                System.out.printf("Server opened at local port :%d\n", ((InetSocketAddress) address).getPort());
            }
        } catch (Throwable e) {
            System.err.printf("Execution error: an error occurred while server start, %s\n", e.getMessage());
            System.exit(3);
        }

        while (true) {
            console.exec();

            try {
                main.saveCSV();

                break;
            } catch (IOException e) {
                console.printError("an error occurred while file saving");
            }
        }

        System.exit(0);
    }

    {
        // Set initialize date to current by default
        metadata.put("Initialize date", new Date().toString());
    }

    private Main() {}

    public List<LivingObject> getLivingObjects() throws FileNotFoundException {
        try {
            loadCSV();
        } catch (Throwable ignored) {}

        return livingObjects.parallelStream().collect(Collectors.toList());
    }

    /**
     * Usage: <code>add &lt;element&gt;</code><br>
     * Adds element to collection.<br>
     * Element represents in JSON and must have a 'name' field
     *
     * @param elementString element in JSON
     * @param console console object
     */
    @CommandHandler(
            usage = "add <element>",
            description = "Adds element to collection.\n" +
                    "Element represents in JSON and must have a 'name' field"
    )
    public void add(final String elementString, final Console console) {
        final LivingObject element = jsonToLivingObject(elementString);

        tryLoadCSV(console);
        if (livingObjects.contains(element)) {
            console.printWarning("specified element is already contains in collection");
            return;
        }

        livingObjects.add(element);
        sortedLivingObjects.add(element);
        trySaveCSV(console);
    }

    /**
     * Usage: <code>remove_greater &lt;element&gt;</code><br>
     * Removes all greater than provided element elements from collection.<br>
     * Element represents in JSON and must have a 'name' field
     *
     * @param elementString element in JSON
     * @param console console object
     */
    @CommandHandler(
            usage = "remove_greater <element>",
            description = "Removes all greater than provided element elements from collection.\n" +
                    "Element represents in JSON and must have a 'name' field"
    )
    public void remove_greater(final String elementString, final Console console) {
        final LivingObject element = jsonToLivingObject(elementString);

        int counter = 0;
        LivingObject greater;
        while (true) {
            tryLoadCSV(console);
            greater = sortedLivingObjects.higher(element);

            if (greater == null) {
                break;
            }

            ++counter;
            livingObjects.remove(greater);
            sortedLivingObjects.remove(greater);
            trySaveCSV(console);
        }

        if (counter == 0) {
            console.printWarning("no one elements have removed");
        } else {
            System.out.printf("%d elements removed", counter);
        }
    }

    /**
     * Usage: <code>show</code><br>
     * Shows all elements in collection
     *
     * @param console console object
     */
    @CommandHandler(description = "Shows all elements in collection")
    public void show(final Console console) {
        tryLoadCSV(console);

        livingObjects.parallelStream().forEach(System.out::println);
    }

    /**
     * Usage: <code>ls</code><br>
     * Alias for <code>show</code>
     *
     * @param console console object
     */
    @CommandHandler(description = "Alias for `show`")
    public void ls(final Console console) {
        show(console);
    }

    /**
     * Usage: <code>save</code><br>
     * Saves collection to file
     */
    @CommandHandler(description = "Saves collection to file")
    public void save() {
        try {
            saveCSV();

            System.out.printf("Saved in %s\n", filename);
        } catch (Throwable e) {
            System.out.printf("Unexpected error: %s\n", e.getMessage());
        }
    }

    /**
     * Usage: <code>info</code><br>
     * Prints information about collection
     *
     * @param console console object
     */
    @CommandHandler(description = "Prints information about collection")
    public void info(final Console console) {
        tryLoadCSV(console);

        System.out.printf("Elements in collection: %d\n", livingObjects.size());

        metadata.entrySet().parallelStream()
                .forEach(field -> System.out.printf("%s: %s\n", field.getKey(), field.getValue()));
    }

    /**
     * Usage: <code>remove_lower &lt;element&gt;</code><br>
     * Removes all lower than provided element elements from collection.<br>
     * Element represents in JSON and must have a 'name' field
     *
     * @param elementString element in JSON
     * @param console console object
     */
    @CommandHandler(
            usage = "remove_lower <element>",
            description = "Removes all lower than provided element elements from collection.\n" +
                    "Element represents in JSON and must have a 'name' field"
    )
    public void remove_lower(final String elementString, final Console console) {
        final LivingObject element = jsonToLivingObject(elementString);

        int counter = 0;
        LivingObject lower;
        while (true) {
            tryLoadCSV(console);
            lower = sortedLivingObjects.lower(element);

            if (lower == null) {
                break;
            }

            ++counter;
            livingObjects.remove(lower);
            sortedLivingObjects.remove(lower);
            trySaveCSV(console);
        }

        if (counter == 0) {
            console.printWarning("no one elements have removed");
        } else {
            System.out.printf("%d elements removed", counter);
        }
    }

    /**
     * Usage: <code>remove &lt;element&gt;</code><br>
     * Removes element from collection.<br>
     * Element represents in JSON and must have a 'name' field
     *
     * @param elementString element in JSON
     * @param console console object
     */
    @CommandHandler(
            usage = "remove <element>",
            description = "Removes element from collection.\n" +
                    "Element represents in JSON and must have a 'name' field"
    )
    public void remove(final String elementString, final Console console) {
        final LivingObject element = jsonToLivingObject(elementString);

        tryLoadCSV(console);
        if (!livingObjects.contains(element)) {
            console.printWarning("specified element isn't contains in collection");
            return;
        }

        livingObjects.remove(element);
        sortedLivingObjects.remove(element);
        trySaveCSV(console);
    }

    /**
     * Usage: <code>help</code><br>
     * Shows available commands
     *
     * @param console console object
     */
    @CommandHandler(usage = "help [command]", description = "Shows available commands or description of provided command")
    public void help(final Console console) {
        console.printHelp(new String[0]);
    }

    /**
     * Usage: <code>help &lt;command&gt;</code><br>
     * Shows description of provided command
     *
     * @param command command
     * @param console console object
     */
    @CommandHandler
    public void help(final String command, final Console console) {
        console.printHelp(new String[] { command });
    }

    /**
     * Usage: <code>exit</code><br>
     * Exit
     *
     * @param console console object
     */
    @CommandHandler(description = "Exit")
    public void exit(final Console console) {
        console.quit();
    }

    private void tryLoadCSV(final Console console) {
        try {
            loadCSV();
        } catch (FileNotFoundException ignored) {
        } catch (Throwable e) {
            console.printWarning("an error occurred while loading data from file");
        }
    }

    private void trySaveCSV(final Console console) {
        try {
            saveCSV();
        } catch (Throwable e) {
            console.printWarning("an error occurred while saving data to file");
        }
    }

    private void loadCSV() throws FileNotFoundException {
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
            metadata.put("Collection type", "HashSet");

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

    private void saveCSV() throws IOException {
        final LivingObjectCSVWriter writer =
                new LivingObjectCSVWriter(new CSVWriterWithHeader(new CSVWriter(new FileWriter(filename))));

        metadata.entrySet().parallelStream().forEach(throwing().consumer(meta -> writer.writeMetadata(meta.getKey(), meta.getValue())));
        livingObjects.parallelStream().forEach(throwing().consumer(writer::write));
    }

    @SuppressWarnings("unchecked")
    private LivingObject jsonToLivingObject(String json) {
        String exception = "an error occurred while json reading";

        try {
            JSONObject jsonObject = JSON.parseObject(Objects.requireNonNull(json));

            exception = "an error occurred while living object constructing";
            LivingObject livingObject = mapToObject(jsonObject.entrySet().parallelStream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())),
                    LabUtils::livingObjectConstructor);

            callIf(jsonObject.get("lives"), Boolean.class::isInstance, lives ->
                    setLivingObjectLives(livingObject, (Boolean) lives));

            callIf(jsonObject.get("items"), Collection.class::isInstance, items -> ((Collection<?>) items).parallelStream()
                    .forEach(_item -> callIf(_item, JSONObject.class::isInstance, item -> livingObject.getItems()
                            .add(mapToObject(((Map<String, ?>) item).entrySet().parallelStream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())),
                                    LabUtils::objectConstructor)))));

            return livingObject;
        } catch (Throwable e) {
            if (e.getMessage() != null) {
                throw new IllegalArgumentException(exception + ", " + e.getMessage());
            }

            throw new IllegalArgumentException(exception);
        }
    }
}

package ru.byprogminer.Lab5_Programming;

import ru.byprogminer.Lab5_Programming.command.Console;
import ru.byprogminer.Lab5_Programming.command.ReflectionCommandRunner;
import ru.byprogminer.Lab5_Programming.command.ReflectionCommandRunner.CommandHandler;
import ru.byprogminer.Lab6_Programming.Server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;
import static ru.byprogminer.Lab5_Programming.LabUtils.jsonToLivingObject;
import static ru.byprogminer.Lab6_Programming.Main.PART_SIZE;

public class Main {

    private static final String USAGE = "Usage: java -jar lab6_server.jar <filename> [port]\n" +
            "  - filename\n" +
            "    Name of CSV file with LivingObject objects\n" +
            "  - port\n" +
            "    Not required port number for opened server";

    private final CollectionManager collection;

    public static void main(final String[] args) {
        // Check is argument provided

        if (args.length < 1) {
            System.err.println("Filename is not provided");
            System.err.println(USAGE);
            System.exit(1);
        }

        final Main main = new Main(new CollectionManager(args[0]));

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

        final Console console = new Console(ReflectionCommandRunner.make(main));

        // Try to load file
        try {
            main.collection.loadCSV();
        } catch (final FileNotFoundException e) {
            console.printWarning("file \"" + main.collection.getFilename() + "\" isn't exists. It will be created");
        } catch (final Throwable e) {
            System.err.printf("Execution error: %s\n", e.getMessage());
            System.err.println(USAGE);
            System.exit(2);
        }

        // Delegate control to console
        console.println("Lab6_Programming. Type `help` to get help");

        try {
            DatagramChannel serverChannel = DatagramChannel.open();
            if (port == null) {
                serverChannel.bind(null);
            } else {
                serverChannel.bind(new InetSocketAddress(port));
            }

            final Server<DatagramChannel> server = new Server<>(main.collection, serverChannel, PART_SIZE);
            final Thread serverThread = new Thread(server);
            serverThread.start();

            while (serverThread.getState() != Thread.State.RUNNABLE) {
                Thread.yield();
            }

            SocketAddress address = serverChannel.getLocalAddress();
            if (address instanceof InetSocketAddress) {
                console.printf("Server opened at local port :%d\n", ((InetSocketAddress) address).getPort());
            }
        } catch (Throwable e) {
            System.err.printf("Execution error: an error occurred while server start, %s\n", e.getMessage());
            System.exit(3);
        }

        while (true) {
            console.exec();

            try {
                main.collection.saveCSV();

                break;
            } catch (IOException e) {
                console.printError("an error occurred while file saving");
            }
        }

        System.exit(0);
    }

    private Main(CollectionManager collection) {
        this.collection = collection;
    }

    @CommandHandler(
            usage = "add <element>",
            description = "Adds element to collection.\n" +
                    "Element represents in JSON and must have a 'name' field"
    )
    public void add(final String elementString, final Console console) {
        collection.add(jsonToLivingObject(elementString), console);
    }

    @CommandHandler(
            usage = "remove_greater <element>",
            description = "Removes all greater than provided element elements from collection.\n" +
                    "Element represents in JSON and must have a 'name' field"
    )
    public void remove_greater(final String elementString, final Console console) {
        collection.removeGreater(jsonToLivingObject(elementString), console);
    }

    @CommandHandler(description = "Shows all elements from the collection")
    public void show(final Console console) {
        collection.show(console);
    }

    @CommandHandler(description = "Alias for `show`")
    public void ls(final Console console) {
        collection.show(console);
    }

    @CommandHandler(description = "Loads collection from file")
    public void load(final Console console) {
        collection.load(console);
    }

    @CommandHandler(description = "Saves collection to file")
    public void save(final Console console) {
        collection.save(console);
    }

    @CommandHandler(description = "Prints information about collection")
    public void info(final Console console) {
        collection.info(console);
    }

    @CommandHandler(
            usage = "remove_lower <element>",
            description = "Removes all lower than provided element elements from collection.\n" +
                    "Element represents in JSON and must have a 'name' field"
    )
    public void remove_lower(final String elementString, final Console console) {
        collection.removeLower(jsonToLivingObject(elementString), console);
    }

    @CommandHandler(
            usage = "remove <element>",
            description = "Removes element from collection.\n" +
                    "Element represents in JSON and must have a 'name' field"
    )
    public void remove(final String elementString, final Console console) {
        collection.remove(jsonToLivingObject(elementString), console);
    }

    @CommandHandler(usage = "help [command]", description = "Shows available commands or description of provided command")
    public void help(final Console console) {
        console.printHelp(new String[0]);
    }

    @CommandHandler
    public void help(final String command, final Console console) {
        console.printHelp(arrayOf(command));
    }

    @CommandHandler(description = "Exit")
    public void exit(final Console console) {
        console.quit();
    }
}

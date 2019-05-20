package ru.byprogminer.Lab7_Programming.frontends;

import ru.byprogminer.Lab5_Programming.command.CommandRunner;
import ru.byprogminer.Lab5_Programming.command.Console;
import ru.byprogminer.Lab5_Programming.command.ReflectionCommandRunner;
import ru.byprogminer.Lab5_Programming.command.ReflectionCommandRunner.CommandHandler;
import ru.byprogminer.Lab5_Programming.csv.CsvReader;
import ru.byprogminer.Lab5_Programming.csv.CsvReaderWithHeader;
import ru.byprogminer.Lab7_Programming.Commands;
import ru.byprogminer.Lab7_Programming.Frontend;
import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.controllers.CollectionController;
import ru.byprogminer.Lab7_Programming.csv.CsvLivingObjectReader;
import ru.byprogminer.Lab7_Programming.logging.Loggers;
import ru.byprogminer.Lab7_Programming.renderers.ConsoleRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Logger;

import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;
import static ru.byprogminer.Lab5_Programming.LabUtils.jsonToLivingObject;

public class LocalFrontend implements Frontend {

    private class CommandListener {

        //=== General commands ===//

        @CommandHandler(
                usage = Commands.Help.USAGE,
                description = Commands.Help.DESCRIPTION
        )
        public void help() {
            console.printHelp(arrayOf());
        }

        @CommandHandler
        public void help(String command) {
            console.printHelp(arrayOf(command));
        }

        @CommandHandler(description = Commands.Exit.DESCRIPTION)
        public void exit() {
            console.quit();
        }

        //=== Collection commands ===//

        @CommandHandler(
                usage = Commands.Add.USAGE,
                description = Commands.Add.DESCRIPTION
        )
        public void add(String elementJson) {
            renderer.render(collectionController.add(jsonToLivingObject(elementJson)));
        }

        @CommandHandler(
                usage = Commands.Remove.USAGE,
                description = Commands.Remove.DESCRIPTION
        )
        public void remove(String elementJson) {
            renderer.render(collectionController.remove(jsonToLivingObject(elementJson)));
        }

        @CommandHandler(
                alias = Commands.RemoveLower.ALIAS,
                usage = Commands.RemoveLower.USAGE,
                description = Commands.RemoveLower.DESCRIPTION
        )
        public void removeLower(String elementJson) {
            renderer.render(collectionController.removeLower(jsonToLivingObject(elementJson)));
        }

        @CommandHandler(
                alias = Commands.RemoveGreater.ALIAS,
                usage = Commands.RemoveGreater.USAGE,
                description = Commands.RemoveGreater.DESCRIPTION
        )
        public void removeGreater(String elementJson) {
            renderer.render(collectionController.removeGreater(jsonToLivingObject(elementJson)));
        }

        @CommandHandler(description = Commands.Info.DESCRIPTION)
        public void info() {
            renderer.render(collectionController.info());
        }

        @CommandHandler(
                usage = Commands.Show.USAGE,
                description = Commands.Show.DESCRIPTION
        )
        public void show() {
            renderer.render(collectionController.show());
        }

        @CommandHandler
        public void show(String countString) {
            final long count;

            try {
                count = Long.parseLong(countString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("count has bad format", e);
            }

            renderer.render(collectionController.show(count));
        }

        @CommandHandler(description = Commands.Ls.DESCRIPTION)
        public void ls() {
            show();
        }

        //=== Import/export commands ===//

        @CommandHandler(
                usage = Commands.Save.USAGE,
                description = Commands.Save.DESCRIPTION
        )
        public void save(String filename) {
            renderer.render(collectionController.save(filename));
        }

        @CommandHandler(
                usage = Commands.Load.USAGE,
                description = Commands.Load.DESCRIPTION
        )
        public void load(String filename) {
            renderer.render(collectionController.load(filename));
        }

        @CommandHandler(
                alias = Commands.Import.ALIAS,
                usage = Commands.Import.USAGE,
                description = Commands.Import.DESCRIPTION
        )
        public void _import(String filename) {
            final Scanner scanner;

            try {
                scanner = new Scanner(new File(filename));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("file not found", e);
            }

            renderer.render(collectionController.importObjects(new CsvLivingObjectReader(new CsvReaderWithHeader(
                    new CsvReader(scanner))).getObjects()));
        }
    }

    private final Logger log = Loggers.getObjectLogger(this);

    private final CommandRunner commandRunner = ReflectionCommandRunner.make(new CommandListener());
    private final Console console = new Console(commandRunner);
    private final Renderer renderer = new ConsoleRenderer(console);

    private final CollectionController collectionController;

    public LocalFrontend(CollectionController collectionController) {
        this.collectionController = collectionController;
    }

    @Override
    public void exec() throws IllegalStateException {
        log.info("execute frontend");

        console.println("Lab7_Programming. Type `help` to get help");
        console.exec();
    }

    @Override
    public void stop() {
        log.info("stop frontend");
        console.quit();
    }
}

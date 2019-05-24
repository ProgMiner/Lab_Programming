package ru.byprogminer.Lab7_Programming.frontends;

import ru.byprogminer.Lab5_Programming.command.CommandRunner;
import ru.byprogminer.Lab5_Programming.command.Console;
import ru.byprogminer.Lab5_Programming.command.ReflectionCommandRunner;
import ru.byprogminer.Lab5_Programming.command.ReflectionCommandRunner.CommandHandler;
import ru.byprogminer.Lab5_Programming.csv.CsvReader;
import ru.byprogminer.Lab5_Programming.csv.CsvReaderWithHeader;
import ru.byprogminer.Lab7_Programming.*;
import ru.byprogminer.Lab7_Programming.controllers.CollectionController;
import ru.byprogminer.Lab7_Programming.controllers.UsersController;
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
            renderer.render(collectionController.add(jsonToLivingObject(elementJson), currentUser.get()));
        }

        @CommandHandler(
                usage = Commands.Remove.USAGE,
                description = Commands.Remove.DESCRIPTION
        )
        public void remove(String elementJson) {
            renderer.render(collectionController.remove(jsonToLivingObject(elementJson), currentUser.get()));
        }

        @CommandHandler(
                usage = Commands.Rm.USAGE,
                description = Commands.Rm.DESCRIPTION
        )
        public void rm(String elementJson) {
            renderer.render(collectionController.remove(jsonToLivingObject(elementJson), currentUser.get()));
        }

        @CommandHandler(
                alias = Commands.RemoveLower.ALIAS,
                usage = Commands.RemoveLower.USAGE,
                description = Commands.RemoveLower.DESCRIPTION
        )
        public void removeLower(String elementJson) {
            renderer.render(collectionController.removeLower(jsonToLivingObject(elementJson), currentUser.get()));
        }

        @CommandHandler(
                alias = Commands.RemoveGreater.ALIAS,
                usage = Commands.RemoveGreater.USAGE,
                description = Commands.RemoveGreater.DESCRIPTION
        )
        public void removeGreater(String elementJson) {
            renderer.render(collectionController.removeGreater(jsonToLivingObject(elementJson), currentUser.get()));
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
            renderer.render(collectionController.save(filename, currentUser.get()));
        }

        @CommandHandler(
                usage = Commands.Load.USAGE,
                description = Commands.Load.DESCRIPTION
        )
        public void load(String filename) {
            renderer.render(collectionController.load(filename, currentUser.get()));
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
                    new CsvReader(scanner))).getObjects(), currentUser.get()));
        }

        //=== Authorization commands ===//

        @CommandHandler(usage = Commands.Su.USAGE, description = Commands.Su.DESCRIPTION)
        public void su() {
            final Credentials credentials = currentUser.reset();

            console.printf("Current user set to %s\n", credentials == null ? "anonymous" : credentials.username);
        }

        @CommandHandler
        public void su(String username) {
            currentUser.set(new Credentials(username,
                    console.requestInput(String.format("Enter password of user %s: ", username))));
            console.printf("Current user set to %s\n", username);
        }

        //=== Users management commands ===//

        @CommandHandler(usage = Commands.Passwd.USAGE, description = Commands.Passwd.DESCRIPTION)
        public void passwd() {
            renderer.render(usersController.changePassword(console
                    .requestInput("Enter new password: "), currentUser.get()));
        }

        @CommandHandler
        public void passwd(String username) {
            renderer.render(usersController.changePassword(username, console
                    .requestInput(String.format("Enter new password for user %s: ", username)), currentUser.get()));
        }

        @CommandHandler(usage = Commands.Register.USAGE, description = Commands.Register.DESCRIPTION)
        public void register(String username) {
            renderer.render(usersController.register(username, console
                    .requestInput(String.format("Enter E-Mail for user %s: ", username)), currentUser.get()));
        }
    }

    private final Logger log = Loggers.getObjectLogger(this);

    private final CommandRunner commandRunner = ReflectionCommandRunner.make(new CommandListener());
    private final Console console = new Console(commandRunner);
    private final Renderer renderer = new ConsoleRenderer(console);

    private final CurrentUser currentUser = new CurrentUser();
    private final CollectionController collectionController;
    private final UsersController usersController;

    public LocalFrontend(UsersController usersController, CollectionController collectionController) {
        this.collectionController = collectionController;
        this.usersController = usersController;
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

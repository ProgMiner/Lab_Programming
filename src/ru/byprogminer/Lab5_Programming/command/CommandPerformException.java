package ru.byprogminer.Lab5_Programming.command;

import java.util.Arrays;
import java.util.Objects;

public class CommandPerformException extends Exception {

    private final String commandName;
    private final String[] commandArgs;

    public CommandPerformException(
            String commandName,
            String[] commandArgs,
            Throwable cause
    ) throws NullPointerException {
        super(Objects.requireNonNull(cause));

        this.commandName = Objects.requireNonNull(commandName);
        this.commandArgs = Objects.requireNonNull(commandArgs);
    }

    public String getCommandName() {
        return commandName;
    }

    public String[] getCommandArgs() {
        return Arrays.copyOf(commandArgs, commandArgs.length);
    }
}

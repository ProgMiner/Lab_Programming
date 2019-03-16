package ru.byprogminer.Lab5_Programming;

import java.util.Arrays;

public class CommandPerformException extends Exception {

    private final String commandName;
    private final String[] commandArgs;

    public CommandPerformException(String commandName, String[] commandArgs, Throwable cause) {
        super(cause);

        this.commandName = commandName;
        this.commandArgs = commandArgs;
    }

    public String getCommandName() {
        return commandName;
    }

    public String[] getCommandArgs() {
        return Arrays.copyOf(commandArgs, commandArgs.length);
    }
}

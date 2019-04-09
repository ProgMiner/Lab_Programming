package ru.byprogminer.Lab5_Programming;

import java.util.*;

public abstract class CommandRunner {

    public final void performCommand(final String commandLine) throws CommandPerformException {
        final List<String> args = parseCommand(Objects.requireNonNull(commandLine));

        if (args.size() < 1) {
            return;
        }

        final String command = args.get(0).toLowerCase();
        args.remove(0);

        performCommand(command, args);
    }

    private List<String> parseCommand(String commandLine) {
        commandLine = commandLine.trim();

        Character quote = null;
        StringBuilder current = new StringBuilder();
        final Stack<Character> brace = new Stack<>();
        final List<String> command = new ArrayList<>();
        for (int i = 0; i < commandLine.length(); ++i) {
            if (quote != null) {
                current.append(commandLine.charAt(i));

                if (commandLine.charAt(i) == quote) {
                    quote = null;
                }

                continue;
            }

            if (!brace.empty()) {
                current.append(commandLine.charAt(i));

                if (commandLine.charAt(i) == brace.peek()) {
                    brace.pop();
                    continue;
                }
            } else if (commandLine.charAt(i) == ' ') {
                if (current.length() > 0) {
                    command.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(commandLine.charAt(i));
            }

            switch (commandLine.charAt(i)) {
                case '{':
                    brace.push('}');
                    break;
                case '[':
                    brace.push(']');
                    break;
                case '"':
                    quote = '"';
                    break;
                case '\'':
                    quote = '\'';
                    break;
            }
        }

        if (current.length() > 0) {
            command.add(current.toString());
        }

        return command;
    }

    public abstract Set<String> getCommands();
    public abstract String getUsage(String name);
    public abstract String getDescription(String name);
    public abstract Integer[] getArgumentsCount(String name);
    public abstract Map<Class, Object> getSpecialParameterTypes();
    protected abstract void performCommand(String command, List<String> args) throws CommandPerformException;
}

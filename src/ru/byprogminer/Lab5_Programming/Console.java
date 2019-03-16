package ru.byprogminer.Lab5_Programming;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Stream;

public class Console {

    private final CommandRunner runner;
    private final Scanner scanner = new Scanner(System.in);

    private volatile boolean running = false;

    private long maxMistakeCount = 3;

    @SuppressWarnings("unchecked")
    private Map<String, String> translator =
            (Map<String, String>) Proxy.newProxyInstance(
                    HashMap.class.getClassLoader(),
                    HashMap.class.getInterfaces(),
                    new InvocationHandler() {
                        final Map<String, String> backingField = Collections.synchronizedMap(new HashMap<>());

                        @Override
                        public Object invoke(
                                final Object ignored,
                                final Method method,
                                final Object[] args
                        ) throws Throwable {
                            final Object ret = method.invoke(backingField, args);

                            if (method.getName().equals("get") && ret == null && args.length > 0 && args[0] != null) {
                                return args[0].toString() + '\n';
                            }

                            return ret;
                        }
                    }
            );

    {
        translator.put("prompt", "> ");
        translator.put("message.error", "Error: %s.\n");
        translator.put("message.warning", "Warning: %s.\n");
        translator.put("message.usage", "Usage: %s\n");
        translator.put("help.try", "Try to call `help`\n");
        translator.put("help.try.command", "Try to call `help %s`\n");
        translator.put("commands.title.help", "Available commands:\n");
        translator.put("commands.title.spelling", "Maybe you mean:\n");
        translator.put("commands.item", "  - %s\n");
    }

    public Console(final CommandRunner runner) {
        this.runner = Objects.requireNonNull(runner);

        runner.getSpecialParameterTypes().put(Console.class, this);
    }

    public void exec() {
        if (running) {
            throw new RuntimeException("this console is running already");
        }

        running = true;
        synchronized (scanner) {
            while (running) {
                System.out.print(translator.get("prompt"));

                if (!scanner.hasNextLine()) {
                    break;
                }

                CommandPerformException cpe = null;
                try {
                    try {
                        runner.performCommand(scanner.nextLine());
                    } catch (final CommandPerformException e) {
                        final Throwable throwable = e.getCause();
                        cpe = e;

                        if (throwable != null) {
                            throw throwable;
                        }
                    }
                } catch (final IllegalArgumentException | UnsupportedOperationException e) {
                    printError(e.getMessage());

                    if (cpe != null) {
                        final String usage = runner.getUsage(cpe.getCommandName());

                        if (usage != null) {
                            System.out.printf(translator.get("message.usage"), usage);
                        }

                        final Set<String> commands = runner.getCommands();
                        if (commands.contains(cpe.getCommandName())) {
                            if (commands.contains("help")) {
                                System.out.printf(translator.get("help.try.command"), cpe.getCommandName());
                            }
                        } else {
                            if (commands.contains("help")) {
                                System.out.print(translator.get("help.try"));
                            }

                            checkSpelling(cpe);
                        }
                    }
                } catch (final Throwable e) {
                    printWarning("an error occurred while command performing");

                    // TODO Logging
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
    }

    public void quit() {
        running = false;
    }

    public void printHelp(final String[] args) {
        final Set<String> commands = runner.getCommands();

        if (args.length == 0) {
            System.out.print(translator.get("commands.title.help"));

            commands.parallelStream().forEach(command ->
                    printHelpForCommand(command, false));

            return;
        }

        final String command = args[0];
        if (commands.contains(command)) {
            printHelpForCommand(command, true);
            return;
        }

        throw new IllegalArgumentException("unknown command " + command);
    }

    public void printError(final String msg) {
        System.out.printf(translator.get("message.error"), msg);
    }

    public void printWarning(final String msg) {
        System.out.printf(translator.get("message.warning"), msg);
    }

    protected void checkSpelling(final CommandPerformException cpe) {
        if (maxMistakeCount <= 0) {
             return;
        }

        final SortedMap<Long, Set<String>> commands = new TreeMap<>();
        for (final String command: runner.getCommands()) {
            final long[][] dynamic = new long[command.length() + 1][cpe.getCommandName().length() + 1];

            for (int i = 0; i <= command.length(); ++i) {
                dynamic[i][0] = i;
            }

            for (int i = 1; i <= cpe.getCommandName().length(); ++i) {
                dynamic[0][i] = i;
            }

            for (int i = 1; i <= command.length(); ++i) {
                for (int j = 1; j <= cpe.getCommandName().length(); ++j) {
                    dynamic[i][j] = Math.min(
                            Math.min(dynamic[i - 1][j] + (command.charAt(i - 1) == '_' ? 0 : 1), dynamic[i][j - 1] + 1),
                            dynamic[i - 1][j - 1] + (command.charAt(i - 1) == cpe.getCommandName().charAt(j - 1) ? 0 : 1)
                    );

                    if (i >= 2 && j >= 2) {
                        dynamic[i][j] = Math.min(dynamic[i][j], dynamic[i - 2][j - 2] +
                                (command.charAt(i - 2) == cpe.getCommandName().charAt(j - 1) ? 0 : 1) +
                                (command.charAt(i - 1) == cpe.getCommandName().charAt(j - 2) ? 0 : 1));
                    }
                }
            }

            final long nameDistance = dynamic[command.length()][cpe.getCommandName().length()] * 2;

            long argsDistance = Long.MAX_VALUE;
            for (final Integer argumentsCount: runner.getArgumentsCount(command)) {
                final long current = Math.abs(argumentsCount - cpe.getCommandArgs().length);

                if (argsDistance > current) {
                    argsDistance = current;
                }
            }

            final long distance = nameDistance + argsDistance;
            if (distance <= maxMistakeCount) {
                commands.computeIfAbsent(distance, k -> new HashSet<>())
                        .add(command);
            }
        }
        
        if (commands.isEmpty()) {
            return;
        }
        
        System.out.print(translator.get("commands.title.spelling"));
        commands.forEach((k, strings) ->
                strings.forEach(command ->
                        System.out.printf(translator.get("commands.item"), getUsage(command))
                )
        );
    }

    protected void printHelpForCommand(final String command, final boolean single) {
        final String usage = getUsage(command);

        if (single) {
            final String description = runner.getDescription(command);

            System.out.printf(translator.get("message.usage"), usage);
            if (description != null) {
                Stream.of(description.split("\n")).forEach(System.out::println);
            } else {
                System.out.println("Description isn't provided");
            }
        } else {
            System.out.printf(translator.get("commands.item"), usage);
        }
    }

    protected String getUsage(final String command) {
        final String usage = runner.getUsage(command);

        if (usage == null) {
            return makeUsage(command);
        }

        return usage;
    }

    protected String makeUsage(final String command) {
        final StringBuilder newUsage = new StringBuilder(command);

        final Integer[] argsCounts = runner.getArgumentsCount(command);
        if (argsCounts.length == 1) {
            Integer argsCount = argsCounts[0];

            if (argsCount == 1) {
                newUsage.append(" <").append(argsCount).append(" argument>");
            } else if (argsCount > 1) {
                newUsage.append(" <").append(argsCount).append(" arguments>");
            }
        } else {
            newUsage.append(" <");

            int counter = 0;
            for (Integer argsCount: argsCounts) {
                if (counter == 0) {
                    newUsage.append(argsCount);
                } else {
                    newUsage.append("/").append(argsCount);
                }

                ++counter;
            }

            newUsage.append(" arguments>");
        }

        return newUsage.toString();
    }

    public long getMaxMistakeCount() {
        return maxMistakeCount;
    }

    public void setMaxMistakeCount(final long maxMistakeCount) {
        this.maxMistakeCount = maxMistakeCount;
    }

    public Map<String, String> getTranslator() {
        return translator;
    }

    public boolean isRunning() {
        return running;
    }
}

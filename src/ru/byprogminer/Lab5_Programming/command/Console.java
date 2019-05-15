package ru.byprogminer.Lab5_Programming.command;

import ru.byprogminer.Lab7_Programming.RunMutex;
import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Console implements StatusPrinter {

    private final CommandRunner runner;
    private final PrintStream printer;
    private final Scanner scanner;

    private final RunMutex runMutex = new RunMutex();

    private volatile long maxMistakeCount = 3;

    private final Logger log = Loggers.getLogger(Console.class.getName());

    @SuppressWarnings("unchecked")
    private final Map<String, String> translator =
            (Map<String, String>) Proxy.newProxyInstance(
                    HashMap.class.getClassLoader(),
                    HashMap.class.getInterfaces(),
                    new InvocationHandler() {
                        final Map<String, String> backingField = new ConcurrentHashMap<>();

                        @Override
                        public Object invoke(
                                final Object ignored,
                                final Method method,
                                final Object[] args
                        ) throws Throwable {
                            final Object ret = method.invoke(backingField, args);

                            if (method.getName().equals("get") && ret == null && args.length > 0 && args[0] != null) {
                                log.info(String.format("Called undefined translation for %s", args[0]));
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
        this(runner, System.in, System.out);
    }

    public Console(final CommandRunner runner, final InputStream in, final PrintStream out) {
        this.runner = Objects.requireNonNull(runner);
        scanner = new Scanner(in);
        printer = out;

        runner.getSpecialParameterTypes().put(Console.class, this);
    }

    public void exec() throws IllegalStateException {
        if (!runMutex.tryRun()) {
            log.warning("Trying to execute running console");
            throw new IllegalStateException("this console is running already");
        }

        synchronized (scanner) {
            while (runMutex.isRunning()) {
                print(translator.get("prompt"));

                if (!scanner.hasNextLine()) {
                    break;
                }

                CommandPerformException cpe = null;
                final String command = scanner.nextLine();
                try {
                    try {
                        runner.performCommand(command);
                    } catch (final CommandPerformException e) {
                        final Throwable throwable = e.getCause();
                        cpe = e;

                        log.log(Level.INFO, "An exception thrown while command performing", e);
                        if (throwable != null) {
                            throw throwable;
                        }
                    }
                } catch (final IllegalArgumentException e) {
                    printError(e.getMessage());

                    if (cpe != null) {
                        final String commandName = cpe.getCommandName();

                        final String usage = runner.getUsage(commandName);
                        if (usage != null) {
                            printf(translator.get("message.usage"), usage);
                        }

                        String[] commandArgs = cpe.getCommandArgs();
                        if (!"help".equals(commandName) || commandArgs.length != 1) {
                            final Set<String> commands = runner.getCommands();

                            if (commands != null && commands.contains(commandName)) {
                                if (commands.contains("help")) {
                                    printf(translator.get("help.try.command"), commandName);
                                }
                            } else {
                                if (commands != null && commands.contains("help")) {
                                    print(translator.get("help.try"));
                                }

                                checkSpelling(commandName, commandArgs.length);
                            }
                        } else {
                            checkSpelling(commandArgs[0], -1);
                        }
                    }
                } catch (final Throwable e) {
                    log.log(Level.WARNING, String.format("An unknown error occurred while command \"%s\" performing", command), e);
                    printWarning("an error occurred while command performing");
                }
            }

            runMutex.end();
        }
    }

    public void quit() {
        runMutex.stop();
    }

    public void printHelp(final String[] args) {
        final Set<String> commands = runner.getCommands();

        if (args.length == 0) {
            print(translator.get("commands.title.help"));

            commands.parallelStream().forEachOrdered(command ->
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

    @Override
    public void print(final Object msg) {
        log.info(String.format("print: %s", msg.toString()));
        printer.print(msg);
    }

    @Override
    public void println(final Object msg) {
        log.info(String.format("println: %s", msg.toString()));
        printer.println(msg);
    }

    @Override
    public void printf(final String format, final Object... args) {
        log.info(String.format("printf: %s", String.format(format, args)));
        printer.printf(format, (Object[]) args);
    }

    @Override
    public void printError(final Object msg) {
        printf(translator.get("message.error"), msg);
    }

    @Override
    public void printWarning(final Object msg) {
        printf(translator.get("message.warning"), msg);
    }

    protected void checkSpelling(final String commandName, int commandArgsCount) {
        if (maxMistakeCount <= 0) {
             return;
        }

        final SortedMap<Long, Set<String>> commands = new TreeMap<>();
        for (final String command: runner.getCommands()) {
            final long[][] dynamic = new long[command.length() + 1][commandName.length() + 1];

            for (int i = 0; i <= command.length(); ++i) {
                dynamic[i][0] = i;
            }

            for (int i = 1; i <= commandName.length(); ++i) {
                dynamic[0][i] = i;
            }

            for (int i = 1; i <= command.length(); ++i) {
                for (int j = 1; j <= commandName.length(); ++j) {
                    dynamic[i][j] = Math.min(
                            Math.min(dynamic[i - 1][j] + (command.charAt(i - 1) == '_' ? 0 : 1), dynamic[i][j - 1] + 1),
                            dynamic[i - 1][j - 1] + (command.charAt(i - 1) == commandName.charAt(j - 1) ? 0 : 1)
                    );

                    if (i >= 2 && j >= 2) {
                        dynamic[i][j] = Math.min(dynamic[i][j], dynamic[i - 2][j - 2] +
                                (command.charAt(i - 2) == commandName.charAt(j - 1) ? 0 : 1) +
                                (command.charAt(i - 1) == commandName.charAt(j - 2) ? 0 : 1));
                    }
                }
            }

            final long nameDistance = dynamic[command.length()][commandName.length()] * 2;

            long argsDistance = Long.MAX_VALUE;
            if (commandArgsCount >= 0) {
                for (final Integer argumentsCount : runner.getArgumentsCount(command)) {
                    final long current = Math.abs(argumentsCount - commandArgsCount);

                    if (argsDistance > current) {
                        argsDistance = current;
                    }
                }
            } else {
                argsDistance = 0;
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
        
        print(translator.get("commands.title.spelling"));
        commands.forEach((k, strings) ->
                strings.forEach(command ->
                        printf(translator.get("commands.item"), getUsage(command))
                )
        );
    }

    protected void printHelpForCommand(final String command, final boolean single) {
        final String usage = getUsage(command);

        if (single) {
            final String description = runner.getDescription(command);

            printf(translator.get("message.usage"), usage);
            if (description != null) {
                Stream.of(description.split("\n")).forEach(this::println);
            } else {
                println("Description isn't provided");
            }
        } else {
            printf(translator.get("commands.item"), usage);
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
}

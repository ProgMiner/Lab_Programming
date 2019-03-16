package ru.byprogminer.Lab5_Programming;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class CommandRunner {

    private interface Invokable<T> {

        T invoke(Object thiz, Object... args) throws IllegalAccessException, InvocationTargetException;
    }

    private final Object handler;

    private final Map<Class, Object> specialParameterTypes = new HashMap<>();
    private final Map<String, Map<Integer, Invokable>> commands = new HashMap<>();
    private final Map<String, String> descriptions = new HashMap<>();
    private final Map<String, String> usages = new HashMap<>();

    private final Set<String> commandNames = Collections.unmodifiableSet(commands.keySet());

    @SuppressWarnings("unchecked")
    private final Map<Class, Object> specialParameterTypesProxy =
            (Map<Class, Object>) Proxy.newProxyInstance(
                    HashMap.class.getClassLoader(),
                    HashMap.class.getInterfaces(),
                    (object, method, args) -> {
                        final int sptCount = specialParameterTypes.size();

                        final Object ret = method.invoke(specialParameterTypes, args);

                        if (sptCount != specialParameterTypes.size()) {
                            regenerateCommands();
                        }

                        return ret;
                    }
            );

    public static CommandRunner getCommandRunner(final Object handler, Map<Class, Object> specialParameterTypes) {
        final CommandRunner ret = new CommandRunner(handler);

        if (specialParameterTypes != null) {
            ret.specialParameterTypes.putAll(specialParameterTypes);
        }

        ret.specialParameterTypes.put(CommandRunner.class, ret);
        ret.regenerateCommands();
        return ret;
    }

    public static CommandRunner getCommandRunner(final Object handler) {
        return getCommandRunner(handler, null);
    }

    private CommandRunner(final Object handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    public Set<String> getCommands() {
        return commandNames;
    }

    public String getDescription(final String name) {
        return descriptions.get(Objects.requireNonNull(name));
    }

    public String getUsage(final String name) {
        return usages.get(Objects.requireNonNull(name));
    }

    public Integer[] getArgumentsCount(final String name) {
        return commands.get(Objects.requireNonNull(name))
                .keySet().toArray(new Integer[0]);
    }

    public Map<Class, Object> getSpecialParameterTypes() {
        return specialParameterTypesProxy;
    }

    public void performCommand(final String commandLine) throws CommandPerformException {
        final List<String> args = parseCommand(Objects.requireNonNull(commandLine));

        if (args.size() < 1) {
            return;
        }

        final String command = args.get(0).toLowerCase();
        args.remove(0);

        final Map<Integer, Invokable> c = commands.get(command);
        if (c == null) {
            throw new CommandPerformException(command, args.toArray(new String[0]), new UnsupportedOperationException("unknown command " + command));
        }

        Invokable invokable = null;
        for (final Map.Entry<Integer, Invokable> cc: c.entrySet()) {
            // Search at least one command

            invokable = cc.getValue();

            if (cc.getKey().equals(args.size())) {
                // Full match
                break;
            }
        }

        if (invokable == null) {
            return;
        }

        try {
            synchronized (handler) {
                invokable.invoke(handler, args.toArray());
            }
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (final InvocationTargetException e) {
            throw new CommandPerformException(command, args.toArray(new String[0]), e.getCause());
        } catch (final Throwable e) {
            throw new CommandPerformException(command, args.toArray(new String[0]), e);
        }
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

    private synchronized void regenerateCommands() {
        commands.clear();
        descriptions.clear();
        usages.clear();

        for (final Method m: handler.getClass().getMethods()) {
            if (!m.isAnnotationPresent(CommandHandler.class)) {
                continue;
            }

            final String commandName = m.getName().toLowerCase();

            final String description = m.getAnnotation(CommandHandler.class).description();
            descriptions.putIfAbsent(commandName, description.equals(CommandHandler.NULL) ? null : description);

            final String usage = m.getAnnotation(CommandHandler.class).usage();
            usages.putIfAbsent(commandName, usage.equals(CommandHandler.NULL) ? null : usage);

            int specialParameterCount = 0;
            Invokable commandInvokable = m::invoke;
            final int parameterCount = m.getParameterCount();
            if (parameterCount > 0) {
                int current = 0;

                final Object[] pattern = new Object[parameterCount];
                for (Class type: m.getParameterTypes()) {
                    final Object specialParameterValue = specialParameterTypes.get(type);

                    if (specialParameterValue != null) {
                        pattern[current] = specialParameterValue;
                        ++specialParameterCount;
                    }

                    ++current;
                }

                if (specialParameterCount > 0) {
                    final int finalSpecialParameterCount = specialParameterCount;
                    final Invokable finalCommandInvokable = commandInvokable;

                    commandInvokable = (thiz, sourceArgs) -> {
                        Object[] args = new Object[sourceArgs.length + finalSpecialParameterCount];

                        for (int i = 0, j = 0; i < args.length; ++i) {
                            if (i < pattern.length && pattern[i] != null) {
                                args[i] = pattern[i];
                            } else if (j < sourceArgs.length) {
                                args[i] = sourceArgs[j++];
                            }
                        }

                        return finalCommandInvokable.invoke(thiz, args);
                    };
                }
            }

            commands.computeIfAbsent(commandName, k -> new HashMap<>())
                    .put(parameterCount - specialParameterCount, commandInvokable);
        }
    }
}

package ru.byprogminer.Lab5_Programming;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class ReflectionCommandRunner extends CommandRunner {

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

    public static ReflectionCommandRunner make(final Object handler, Map<Class, Object> specialParameterTypes) {
        final ReflectionCommandRunner ret = new ReflectionCommandRunner(handler);

        if (specialParameterTypes != null) {
            ret.specialParameterTypes.putAll(specialParameterTypes);
        }

        ret.specialParameterTypes.put(CommandRunner.class, ret);
        ret.regenerateCommands();
        return ret;
    }

    public static ReflectionCommandRunner make(final Object handler) {
        return make(handler, null);
    }

    private ReflectionCommandRunner(final Object handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public Set<String> getCommands() {
        return commandNames;
    }

    @Override
    public String getDescription(final String name) {
        return descriptions.get(Objects.requireNonNull(name));
    }

    @Override
    public String getUsage(final String name) {
        return usages.get(Objects.requireNonNull(name));
    }

    @Override
    public Integer[] getArgumentsCount(final String name) {
        return commands.get(Objects.requireNonNull(name))
                .keySet().toArray(new Integer[0]);
    }

    @Override
    public Map<Class, Object> getSpecialParameterTypes() {
        return specialParameterTypesProxy;
    }

    @Override
    protected void performCommand(final String command, final List<String> args) throws CommandPerformException {
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

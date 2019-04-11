package ru.byprogminer.Lab5_Programming.command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.*;

import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;

public abstract class ListCommandRunner extends CommandRunner {

    public interface Invokable {

        void invoke(Object... args) throws IllegalAccessException, InvocationTargetException;
    }

    protected final Map<Class, Object> specialParameterTypes = new HashMap<>();
    protected final Map<String, Map<Integer, Invokable>> commands = new HashMap<>();
    protected final Map<String, String> descriptions = new HashMap<>();
    protected final Map<String, String> usages = new HashMap<>();

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

    @Override
    public final Set<String> getCommands() {
        return commandNames;
    }

    @Override
    public final String getDescription(final String name) {
        return descriptions.get(Objects.requireNonNull(name));
    }

    @Override
    public final String getUsage(final String name) {
        return usages.get(Objects.requireNonNull(name));
    }

    @Override
    public final Integer[] getArgumentsCount(final String name) {
        return commands.get(Objects.requireNonNull(name))
                .keySet().toArray(new Integer[0]);
    }

    @Override
    public final Map<Class, Object> getSpecialParameterTypes() {
        return specialParameterTypesProxy;
    }

    @Override
    protected final void performCommand(final String command, final List<String> args) throws CommandPerformException {
        final Map<Integer, Invokable> c = commands.get(command);

        if (c == null) {
            throw new CommandPerformException(command, args.toArray(arrayOf()), new UnsupportedOperationException("unknown command " + command));
        }

        Invokable invokable = c.get(args.size());
        if (invokable == null) {
            throw new CommandPerformException(command, args.toArray(arrayOf()), new UnsupportedOperationException("illegal count of arguments"));
        }

        try {
            invokable.invoke(args.toArray());
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (final InvocationTargetException e) {
            throw new CommandPerformException(command, args.toArray(new String[0]), e.getCause());
        } catch (final Throwable e) {
            throw new CommandPerformException(command, args.toArray(new String[0]), e);
        }
    }

    protected abstract void regenerateCommands();
}

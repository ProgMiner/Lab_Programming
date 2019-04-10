package ru.byprogminer.Lab5_Programming.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.*;

public class ReflectionCommandRunner extends ListCommandRunner {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CommandHandler {

        String NULL = "\0";

        String alias() default NULL;
        String description() default NULL;
        String usage() default NULL;
    }

    private final Object handler;

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
    protected synchronized void regenerateCommands() {
        commands.clear();
        descriptions.clear();
        usages.clear();

        for (final Method m: handler.getClass().getMethods()) {
            if (!m.isAnnotationPresent(CommandHandler.class)) {
                continue;
            }

            final String alias = m.getAnnotation(CommandHandler.class).alias();
            final String commandName = alias.equals(CommandHandler.NULL) ? m.getName().toLowerCase() : alias;

            final String description = m.getAnnotation(CommandHandler.class).description();
            descriptions.putIfAbsent(commandName, description.equals(CommandHandler.NULL) ? null : description);

            final String usage = m.getAnnotation(CommandHandler.class).usage();
            usages.putIfAbsent(commandName, usage.equals(CommandHandler.NULL) ? null : usage);

            int specialParameterCount = 0;
            Invokable commandInvokable = (args) -> m.invoke(handler, args);
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

                    commandInvokable = (sourceArgs) -> {
                        Object[] args = new Object[sourceArgs.length + finalSpecialParameterCount];

                        for (int i = 0, j = 0; i < args.length; ++i) {
                            if (i < pattern.length && pattern[i] != null) {
                                args[i] = pattern[i];
                            } else if (j < sourceArgs.length) {
                                args[i] = sourceArgs[j++];
                            }
                        }

                        finalCommandInvokable.invoke(args);
                    };
                }
            }

            commands.computeIfAbsent(commandName, k -> new HashMap<>())
                    .put(parameterCount - specialParameterCount, commandInvokable);
        }
    }
}

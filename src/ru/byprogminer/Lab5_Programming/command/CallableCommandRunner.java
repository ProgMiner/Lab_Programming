package ru.byprogminer.Lab5_Programming.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CallableCommandRunner extends ListCommandRunner {

    public class CommandBuilder {

        private final String name;

        private volatile String usage = null;
        private volatile String description = null;

        private final Map<Class<?>[], Invokable> callables = new HashMap<>();

        private CommandBuilder(String name) {
            this.name = name;
        }

        public CommandBuilder usage(String usage) {
            this.usage = usage;
            return this;
        }

        public CommandBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CommandBuilder callable(Class<?>[] arguments, Invokable callable) {
            callables.put(arguments, callable);
            return this;
        }

        public CallableCommandRunner save() {
            builders.add(this);

            regenerateCommands();
            return CallableCommandRunner.this;
        }
    }

    private final Set<CommandBuilder> builders = new HashSet<>();

    public CommandBuilder command(String name) {
        return new CommandBuilder(name);
    }

    @Override
    protected void regenerateCommands() {
        commands.clear();
        descriptions.clear();
        usages.clear();

        for (CommandBuilder builder: builders) {
            usages.putIfAbsent(builder.name, builder.usage);
            descriptions.putIfAbsent(builder.name, builder.description);

            final Map<Integer, Invokable> callables = new HashMap<>();
            for (Map.Entry<Class<?>[], Invokable> callable: builder.callables.entrySet()) {
                int specialParameterCount = 0;
                Invokable commandInvokable = callable.getValue();
                final int parameterCount = callable.getKey().length;
                if (parameterCount > 0) {
                    int current = 0;

                    final Object[] pattern = new Object[parameterCount];
                    for (Class type: callable.getKey()) {
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

                callables.put(parameterCount - specialParameterCount, commandInvokable);
            }

            commands.put(builder.name, callables);
        }
    }
}

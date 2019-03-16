package ru.byprogminer.Lab5_Programming.throwing;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> extends Consumer<T> {

    @Override
    default void accept(T value) {
        try {
            throwingAccept(value);
        } catch (Throwable exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    void throwingAccept(T value) throws E;
}

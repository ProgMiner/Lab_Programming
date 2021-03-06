package ru.byprogminer.Lab5_Programming.throwing;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> extends Consumer<T> {

    @Override
    default void accept(T value) {
        try {
            throwingAccept(value);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }

            throw new LambdaException(exception);
        }
    }

    void throwingAccept(T value) throws E;
}

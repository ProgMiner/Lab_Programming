package ru.byprogminer.Lab5_Programming.throwing;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface ThrowingBiConsumer<A, B, E extends Exception> extends BiConsumer<A, B> {

    @Override
    default void accept(A a, B b) {
        try {
            throwingAccept(a, b);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }

            throw new LambdaException(exception);
        }
    }

    void throwingAccept(A a, B b) throws E;
}

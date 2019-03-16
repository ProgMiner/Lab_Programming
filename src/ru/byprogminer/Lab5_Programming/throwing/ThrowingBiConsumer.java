package ru.byprogminer.Lab5_Programming.throwing;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface ThrowingBiConsumer<A, B, E extends Throwable> extends BiConsumer<A, B> {

    @Override
    default void accept(A a, B b) {
        try {
            throwingAccept(a, b);
        } catch (Throwable exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    void throwingAccept(A a, B b) throws E;
}

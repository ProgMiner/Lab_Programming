package ru.byprogminer.Lab5_Programming.throwing;

import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> extends Supplier<T> {

    @Override
    default T get() {
        try {
            return throwingGet();
        } catch (Throwable exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    T throwingGet() throws E;
}

package ru.byprogminer.Lab5_Programming.throwing;

import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> extends Supplier<T> {

    @Override
    default T get() {
        try {
            return throwingGet();
        } catch (Exception exception) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }

            throw new LambdaException(exception);
        }
    }

    T throwingGet() throws E;
}

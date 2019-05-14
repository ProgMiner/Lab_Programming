package ru.byprogminer.Lab5_Programming.throwing;

import java.util.function.Function;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> extends Function<T, R> {

    @Override
    default R apply(T value) {
        try {
            return throwingApply(value);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }

            throw new LambdaException(exception);
        }
    }

    R throwingApply(T value) throws E;
}

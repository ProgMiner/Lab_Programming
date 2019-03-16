package ru.byprogminer.Lab5_Programming.throwing;

import java.util.function.Function;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Throwable> extends Function<T, R> {

    @Override
    default R apply(T value) {
        try {
            return throwingApply(value);
        } catch (Throwable exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    R throwingApply(T value) throws E;
}

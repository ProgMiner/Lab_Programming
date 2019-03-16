package ru.byprogminer.Lab5_Programming.throwing;

import java.util.function.BiFunction;

@FunctionalInterface
public interface ThrowingBiFunction<A, B, R, E extends Throwable> extends BiFunction<A, B, R> {

    @Override
    default R apply(A a, B b) {
        try {
            return throwingApply(a, b);
        } catch (Throwable exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    R throwingApply(A a, B b) throws E;
}

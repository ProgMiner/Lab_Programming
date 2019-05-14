package ru.byprogminer.Lab5_Programming.throwing;

import java.util.function.BiFunction;

@FunctionalInterface
public interface ThrowingBiFunction<A, B, R, E extends Exception> extends BiFunction<A, B, R> {

    @Override
    default R apply(A a, B b) {
        try {
            return throwingApply(a, b);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }

            throw new LambdaException(exception);
        }
    }

    R throwingApply(A a, B b) throws E;
}

package ru.byprogminer.Lab5_Programming.throwing;

import java.util.function.Supplier;

public final class Throwing {

    private static final Throwing throwing = new Throwing();

    private Throwing() {}

    public static Throwing getThrowing() {
        return throwing;
    }

    public <E extends Exception>
    ThrowingRunnable<E> runnable(ThrowingRunnable<E> runnable) {
        return runnable;
    }

    public <T, E extends Exception>
    ThrowingConsumer<T, E> consumer(ThrowingConsumer<T, E> consumer) {
        return consumer;
    }

    public <A, B, E extends Exception>
    ThrowingBiConsumer<A, B, E> consumer(ThrowingBiConsumer<A, B, E> consumer) {
        return consumer;
    }

    public <T, E extends Exception>
    ThrowingSupplier<T, E> supplier(ThrowingSupplier<T, E> supplier) {
        return supplier;
    }

    public <T, R, E extends Exception>
    ThrowingFunction<T, R, E> function(ThrowingFunction<T, R, E> function) {
        return function;
    }

    public <A, B, R, E extends Exception>
    ThrowingBiFunction<A, B, R, E> function(ThrowingBiFunction<A, B, R, E> function) {
        return function;
    }

    public <E extends Exception> void unwrap(Class<E> exception, Runnable code) throws E {
        try {
            code.run();
        } catch (LambdaException e) {
            final Throwable cause = e.getCause();

            if (exception != null && exception.isInstance(cause)) {
                throw (E) cause;
            }

            throw e;
        }
    }

    public <T, E extends Exception> T unwrap(Class<E> exception, Supplier<T> code) throws E {
        try {
            return code.get();
        } catch (LambdaException e) {
            final Throwable cause = e.getCause();

            if (exception != null && exception.isInstance(cause)) {
                throw (E) cause;
            }

            throw e;
        }
    }
}

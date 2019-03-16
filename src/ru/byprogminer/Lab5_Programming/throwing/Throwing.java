package ru.byprogminer.Lab5_Programming.throwing;

public final class Throwing {

    private final static Throwing throwing = new Throwing();

    private Throwing() {}

    public static Throwing getThrowing() {
        return throwing;
    }

    public <T, E extends Throwable>
    ThrowingConsumer<T, E> consumer(ThrowingConsumer<T, E> consumer) {
        return consumer;
    }

    public <A, B, E extends Throwable>
    ThrowingBiConsumer<A, B, E> consumer(ThrowingBiConsumer<A, B, E> consumer) {
        return consumer;
    }

    public <T, E extends Throwable>
    ThrowingSupplier<T, E> supplier(ThrowingSupplier<T, E> supplier) {
        return supplier;
    }

    public <T, R, E extends Throwable>
    ThrowingFunction<T, R, E> function(ThrowingFunction<T, R, E> function) {
        return function;
    }

    public <A, B, R, E extends Throwable>
    ThrowingBiFunction<A, B, R, E> function(ThrowingBiFunction<A, B, R, E> function) {
        return function;
    }
}

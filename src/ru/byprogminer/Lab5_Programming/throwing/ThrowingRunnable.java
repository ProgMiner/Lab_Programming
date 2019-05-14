package ru.byprogminer.Lab5_Programming.throwing;

@FunctionalInterface
public interface ThrowingRunnable<E extends Exception> extends Runnable {

    @Override
    default void run() {
        try {
            throwingRun();
        } catch (Exception exception) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }

            throw new LambdaException(exception);
        }
    }

    void throwingRun() throws E;
}

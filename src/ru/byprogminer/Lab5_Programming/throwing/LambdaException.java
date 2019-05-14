package ru.byprogminer.Lab5_Programming.throwing;

public class LambdaException extends RuntimeException {

    public LambdaException(Throwable cause) {
        super("exception thrown in a throwing lambda expression", cause);
    }
}

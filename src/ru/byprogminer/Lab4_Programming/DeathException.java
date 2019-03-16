package ru.byprogminer.Lab4_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;

import java.util.Formatter;
import java.util.Objects;

public class DeathException extends RuntimeException {

    private LivingObject diedObject;

    public DeathException(LivingObject diedObject) {
        this.diedObject = Objects.requireNonNull(diedObject);
    }

    public LivingObject getDiedObject() {
        return diedObject;
    }

    @Override
    public String getMessage() {
        return new Formatter()
                .format("Object %s is died", diedObject.getName())
                .toString();
    }

    @Override
    public String getLocalizedMessage() {
        return new Formatter()
                .format("Объект %s мёртв", diedObject.getName())
                .toString();
    }
}

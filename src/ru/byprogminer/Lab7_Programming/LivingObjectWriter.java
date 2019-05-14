package ru.byprogminer.Lab7_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;

import java.io.Flushable;

public interface LivingObjectWriter<E extends Exception> extends Flushable {

    void write(LivingObject livingObject) throws E;
    void writeMetadata(String key, String value) throws E;
}

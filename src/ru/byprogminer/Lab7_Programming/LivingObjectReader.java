package ru.byprogminer.Lab7_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;

import java.util.Collection;
import java.util.Map;

public interface LivingObjectReader {

    Collection<LivingObject> getObjects();
    Map<String, String> getMetadata();
}

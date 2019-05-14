package ru.byprogminer.Lab7_Programming.models;

import ru.byprogminer.Lab3_Programming.LivingObject;

import java.util.Collection;
import java.util.Map;

public interface CollectionModel {

    int add(LivingObject livingObject);
    int addAll(Collection<LivingObject> livingObjects);

    int remove(LivingObject livingObject);
    int removeLower(LivingObject livingObject);
    int removeGreater(LivingObject livingObject);

    Map<String, String> getMetadata();

    Collection<LivingObject> get();
    Collection<LivingObject> get(long count);

    void load(Collection<LivingObject> livingObjects, Map<String, String> metadata);
}

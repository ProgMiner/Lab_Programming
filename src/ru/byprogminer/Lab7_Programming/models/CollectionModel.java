package ru.byprogminer.Lab7_Programming.models;

import ru.byprogminer.Lab3_Programming.LivingObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Methods behaviour:
 *   - add(LivingObject) - add an element as owned by nobody
 *   - add(LivingObject, String) - add an element as owned by provided user
 *
 *   - addAll(Collection&lt;LivingObject&gt;) - add elements as owned by nobody
 *   - addAll(Map&lt;LivingObject, String&gt;) - add elements as owned by associated users
 *   - addAll(Collection&lt;LivingObject&gt;, String) - add elements as owned by specified user
 *
 *   - remove(LivingObject) - remove element
 *   - remove(LivingObject, String) - remove element if it is owned by provided user
 *
 *   - removeLower(LivingObject) - remove each element that is lower than the provided
 *   - removeLower(LivingObject, String) - remove each element that is lower than the provided
 *                                         and owned by provided user
 *
 *   - removeGreater(LivingObject) - remove each element that is greater than the provided
 *   - removeGreater(LivingObject, String) - remove each element that is greater than the provided
 *                                           and owned by provided user
 *
 *   - getMetadata() - get metadata
 *
 *   - get() - get all elements
 *   - get(long) - get some provided number of elements
 *
 *   - load(Map&lt;LivingObject, String&gt;, Map&lt;String, String&gt;) - load collection elements and metadata
 *                                                                        instead of current
 *
 * Each element can has only one or zero owners.
 * If somebody try to add an element that is already has in collection, nothing does.
 */
public interface CollectionModel {

    default int add(LivingObject livingObject) {
        return add(livingObject, null);
    }

    default int add(LivingObject livingObject, String username) {
        final Map<LivingObject, String> livingObjects = new HashMap<>();
        livingObjects.put(livingObject, username);

        return addAll(livingObjects);
    }

    default int addAll(Collection<LivingObject> livingObjects) {
        return addAll(livingObjects, null);
    }

    default int addAll(Collection<LivingObject> livingObjects, String username) {
        final Map<LivingObject, String> livingObjectsMap = new HashMap<>();
        for (LivingObject livingObject : livingObjects) {
            livingObjectsMap.put(livingObject, username);
        }

        return addAll(livingObjectsMap);
    }

    int addAll(Map<LivingObject, String> livingObjects);

    default int remove(LivingObject livingObject) {
        return remove(livingObject, null);
    }

    int remove(LivingObject livingObject, String username);

    default int removeLower(LivingObject livingObject) {
        return removeLower(livingObject, null);
    }

    int removeLower(LivingObject livingObject, String username);

    default int removeGreater(LivingObject livingObject) {
        return removeGreater(livingObject, null);
    }

    int removeGreater(LivingObject livingObject, String username);

    default int replaceElement(LivingObject oldElement, LivingObject newElement) {
        return replaceElement(oldElement, newElement, null);
    }

    int replaceElement(LivingObject oldElement, LivingObject newElement, String username);

    Map<String, String> getMetadata();

    Collection<LivingObject> get();
    Collection<LivingObject> get(long count);

    void load(Map<LivingObject, String> livingObjects, Map<String, String> metadata);
}

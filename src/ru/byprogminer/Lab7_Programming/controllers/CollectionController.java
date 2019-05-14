package ru.byprogminer.Lab7_Programming.controllers;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab7_Programming.LivingObjectReader;
import ru.byprogminer.Lab7_Programming.LivingObjectWriter;
import ru.byprogminer.Lab7_Programming.models.CollectionModel;
import ru.byprogminer.Lab7_Programming.views.*;

import java.util.*;

import static ru.byprogminer.Lab5_Programming.LabUtils.throwing;

public class CollectionController {

    private final CollectionModel collectionModel;

    public CollectionController(CollectionModel collectionModel) {
        this.collectionModel = collectionModel;
    }

    public AddView add(LivingObject livingObject) {
        try {
            return new AddView(collectionModel.add(livingObject));
        } catch (Throwable e) {
            return new AddView(e.getLocalizedMessage());
        }
    }

    public RemoveView remove(LivingObject livingObject) {
        try {
            return new RemoveView(collectionModel.remove(livingObject));
        } catch (Throwable e) {
            return new RemoveView(e.getLocalizedMessage());
        }
    }

    public RemoveView removeLower(LivingObject livingObject) {
        try {
            return new RemoveView(collectionModel.removeLower(livingObject));
        } catch (Throwable e) {
            return new RemoveView(e.getLocalizedMessage());
        }
    }

    public RemoveView removeGreater(LivingObject livingObject) {
        try {
            return new RemoveView(collectionModel.removeGreater(livingObject));
        } catch (Throwable e) {
            return new RemoveView(e.getLocalizedMessage());
        }
    }

    public InfoView info() {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("Collection type", "HashSet");

        try {
            metadata.putAll(collectionModel.getMetadata());
            return new InfoView(metadata);
        } catch (Throwable e) {
            return new InfoView(metadata, e.getLocalizedMessage());
        }
    }

    public ShowView show() {
        try {
            return new ShowView(collectionModel.get());
        } catch (Throwable e) {
            return new ShowView(e.getLocalizedMessage());
        }
    }

    public ShowView show(long count) {
        try {
            return new ShowView(collectionModel.get(count));
        } catch (Throwable e) {
            return new ShowView(e.getLocalizedMessage());
        }
    }

    public <E extends Exception> SaveView save(LivingObjectWriter<E> to, String destination) {
        try {
            final SortedMap<String, String> metadata = new TreeMap<>(collectionModel.getMetadata());
            metadata.forEach(throwing().consumer(to::writeMetadata));

            final SortedSet<LivingObject> livingObjects = new TreeSet<>(collectionModel.get());
            livingObjects.forEach(throwing().consumer(to::write));

            to.flush();
            return new SaveView(destination);
        } catch (Throwable e) {
            return new SaveView(destination, e.getLocalizedMessage());
        }
    }

    public LoadView load(LivingObjectReader from, String source) {
        try {
            collectionModel.load(from.getObjects(), from.getMetadata());

            return new LoadView(source);
        } catch (Throwable e) {
            return new LoadView(source, e.getLocalizedMessage());
        }
    }

    public ImportView importObjects(Collection<LivingObject> livingObjects) {
        try {
            return new ImportView(collectionModel.addAll(livingObjects));
        } catch (Throwable e) {
            return new ImportView(e.getLocalizedMessage());
        }
    }
}

package ru.byprogminer.Lab7_Programming.controllers;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab5_Programming.csv.CsvReader;
import ru.byprogminer.Lab5_Programming.csv.CsvReaderWithHeader;
import ru.byprogminer.Lab5_Programming.csv.CsvWriter;
import ru.byprogminer.Lab5_Programming.csv.CsvWriterWithHeader;
import ru.byprogminer.Lab7_Programming.Credentials;
import ru.byprogminer.Lab7_Programming.LivingObjectReader;
import ru.byprogminer.Lab7_Programming.LivingObjectWriter;
import ru.byprogminer.Lab7_Programming.View;
import ru.byprogminer.Lab7_Programming.csv.CsvLivingObjectReader;
import ru.byprogminer.Lab7_Programming.csv.CsvLivingObjectWriter;
import ru.byprogminer.Lab7_Programming.models.CollectionModel;
import ru.byprogminer.Lab7_Programming.models.UsersModel;
import ru.byprogminer.Lab7_Programming.views.*;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.function.Supplier;

import static ru.byprogminer.Lab5_Programming.LabUtils.setOf;
import static ru.byprogminer.Lab5_Programming.LabUtils.throwing;

public class CollectionController {

    private final CollectionModel collectionModel;
    private final UsersModel usersModel;

    public CollectionController(UsersModel usersModel, CollectionModel collectionModel) {
        this.collectionModel = collectionModel;
        this.usersModel = usersModel;
    }

    public View add(LivingObject livingObject, Credentials credentials) {
        return permissionTemplate(credentials, "collection.add", () -> {
            try {
                return new AddView(collectionModel.add(livingObject, credentials.username));
            } catch (Throwable e) {
                return new AddView(e.getLocalizedMessage());
            }
        });
    }

    public View remove(LivingObject livingObject, Credentials credentials) {
        return permissionTemplate(credentials, setOf("collection.remove.all", "collection.remove.own"), () -> {
            try {
                if (usersModel.hasPermission(credentials.username, "collection.remove.all")) {
                    return new RemoveView(collectionModel.remove(livingObject));
                }

                return new RemoveView(collectionModel.remove(livingObject, credentials.username));
            } catch (Throwable e) {
                return new RemoveView(e.getLocalizedMessage());
            }
        });
    }

    public View removeLower(LivingObject livingObject, Credentials credentials) {
        return permissionTemplate(credentials, setOf("collection.removeLower.all", "collection.removeLower.own"), () -> {
            try {
                if (usersModel.hasPermission(credentials.username, "collection.removeLower.all")) {
                    return new RemoveView(collectionModel.removeLower(livingObject));
                }

                return new RemoveView(collectionModel.removeLower(livingObject, credentials.username));
            } catch (Throwable e) {
                return new RemoveView(e.getLocalizedMessage());
            }
        });
    }

    public View removeGreater(LivingObject livingObject, Credentials credentials) {
        return permissionTemplate(credentials, setOf("collection.removeGreater.all", "collection.removeGreater.own"), () -> {
            try {
                return new RemoveView(collectionModel.removeGreater(livingObject, credentials.username));
            } catch (Throwable e) {
                return new RemoveView(e.getLocalizedMessage());
            }
        });
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

    public View save(String filename, Credentials credentials) {
        return permissionTemplate(credentials, "collection.save", () -> {
            try {
                final LivingObjectWriter<?> writer = new CsvLivingObjectWriter(new CsvWriterWithHeader(
                        new CsvWriter(new FileWriter(filename))));

                final SortedMap<String, String> metadata = new TreeMap<>(collectionModel.getMetadata());
                metadata.forEach(throwing().consumer(writer::writeMetadata));

                final SortedSet<LivingObject> livingObjects = new TreeSet<>(collectionModel.get());
                livingObjects.forEach(throwing().consumer(writer::write));

                writer.flush();
                return new SaveView(filename);
            } catch (Throwable e) {
                return new SaveView(filename, e.getLocalizedMessage());
            }
        });
    }

    public View load(String filename, Credentials credentials) {
        return permissionTemplate(credentials, "collection.load", () -> {
            try {
                final LivingObjectReader reader = new CsvLivingObjectReader(new CsvReaderWithHeader(
                        new CsvReader(new Scanner(new File(filename)))));

                // TODO add to living object reader users
                final Map<LivingObject, String> livingObjects = new HashMap<>();
                for (LivingObject livingObject : reader.getObjects()) {
                    livingObjects.put(livingObject, null);
                }

                collectionModel.load(livingObjects, reader.getMetadata());
                return new LoadView(filename);
            } catch (Throwable e) {
                return new LoadView(filename, e.getLocalizedMessage());
            }
        });
    }

    public View importObjects(Collection<LivingObject> livingObjects, Credentials credentials) {
        return permissionTemplate(credentials, "collection.importObjects", () -> {
            try {
                return new ImportView(collectionModel.addAll(livingObjects, credentials.username));
            } catch (Throwable e) {
                return new ImportView(e.getLocalizedMessage());
            }
        });
    }

    private View authorizedTemplate(Credentials credentials, Supplier<View> code) {
        if (credentials == null) {
            return new NotLoggedView();
        }

        if (!usersModel.check(credentials)) {
            return new WrongCredentialsView(credentials);
        }

        return code.get();
    }

    private View permissionTemplate(Credentials credentials, String permission, Supplier<View> code) {
        return authorizedTemplate(credentials, () -> {
            if (!usersModel.hasPermission(credentials.username, permission)) {
                return new NotPermittedView();
            }

            return code.get();
        });
    }

    private View permissionTemplate(Credentials credentials, Set<String> permissions, Supplier<View> code) {
        return authorizedTemplate(credentials, () -> {
            if (!usersModel.hasPermission(credentials.username, permissions)) {
                return new NotPermittedView();
            }

            return code.get();
        });
    }
}

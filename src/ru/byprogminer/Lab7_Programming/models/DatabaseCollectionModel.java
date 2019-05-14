package ru.byprogminer.Lab7_Programming.models;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab3_Programming.Object;
import ru.byprogminer.Lab5_Programming.throwing.ThrowingSupplier;
import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.byprogminer.Lab5_Programming.LabUtils.*;

/**
 * metadata:
 * +-------+------+-------------+
 * |  key  | text | PRIMARY KEY |
 * | value | text |  NOT NULL   |
 * +-------+------+-------------+
 *
 * living_objects:
 * +---------------+---------------+--------------------------------+-----------------------------------------+
 * |   hash_code   |    integer    |           PRIMARY KEY          |                                         |
 * |      name     |     text      | NOT NULL UNIQUE(name,          |                                         |
 * |     volume    |    numeric    | NOT NULL        volume,        |                                         |
 * | creating_time |  timestamptz  | NOT NULL        creating_time, |                                         |
 * |       x       |    numeric    | NOT NULL        x,             |                                         |
 * |       y       |    numeric    | NOT NULL        y,             |                                         |
 * |       z       |    numeric    | NOT NULL        z,             |                                         |
 * |     lives     |    boolean    | NOT NULL        lives,         |                                         |
 * |     items     |    integer    | NOT NULL        items)         | FOREIGN KEY REFERENCES items_hcs(items) |
 * +---------------+---------------+--------------------------------+-----------------------------------------+
 *
 * items_hcs:
 * +-------+---------+-------------+
 * | items | integer | PRIMARY KEY |
 * +-------+---------+-------------+
 *
 * items_items:
 * +-----------+---------+------------------------+-----------------------------------------+
 * |   items   | integer | PRIMARY KEY(items,     | FOREIGN KEY REFERENCES items_hcs(items) |
 * | hash_code | integer |             hash_code) | FOREIGN KEY REFERENCES items(hash_code) |
 * +-----------+---------+------------------------+-----------------------------------------+
 *
 * items:
 * +---------------+-------------+--------------------------------+
 * |   hash_code   |   integer   |           PRIMARY KEY          |
 * |      name     |    text     | NOT NULL UNIQUE(name,          |
 * |     volume    |   numeric   | NOT NULL        volume,        |
 * | creating_time | timestamptz | NOT NULL        creating_time, |
 * |       x       |   numeric   | NOT NULL        x,             |
 * |       y       |   numeric   | NOT NULL        y,             |
 * |       z       |   numeric   | NOT NULL        z)             |
 * +---------------+-------------+--------------------------------+
 */
public class DatabaseCollectionModel implements CollectionModel {

    private static final class Query {

        private static final class ValidateDatabase {

            private static final String CREATE_METADATA_TABLE = "" +
                    "CREATE TABLE IF NOT EXISTS \"%1$s\"(\n" +
                    "    key   text PRIMARY KEY,\n" +
                    "    value text NOT NULL\n" +
                    ")";

            private static final String CREATE_ITEMS_HCS_TABLE = "" +
                    "CREATE TABLE IF NOT EXISTS \"%1$s\"(\n" +
                    "    items integer PRIMARY KEY\n" +
                    ")";

            private static final String CREATE_LIVING_OBJECTS_TABLE = "" +
                    "CREATE TABLE IF NOT EXISTS \"%1$s\"(\n" +
                    "    hash_code     integer     PRIMARY KEY,\n" +
                    "    name          text        NOT NULL,\n" +
                    "    volume        numeric     NOT NULL,\n" +
                    "    creating_time timestamptz NOT NULL,\n" +
                    "    x             numeric     NOT NULL,\n" +
                    "    y             numeric     NOT NULL,\n" +
                    "    z             numeric     NOT NULL,\n" +
                    "    lives         boolean     NOT NULL,\n" +
                    "    items         integer     NOT NULL,\n" +
                    "    UNIQUE(name, volume, creating_time,\n" +
                    "           x, y, z, lives, items),\n" +
                    "    FOREIGN KEY(items) REFERENCES \"%2$s\"(items)\n" +
                    "        ON DELETE RESTRICT\n" +
                    ")";

            private static final String CREATE_ITEMS_TABLE = "" +
                    "CREATE TABLE IF NOT EXISTS \"%1$s\"(\n" +
                    "    hash_code     integer     PRIMARY KEY,\n" +
                    "    name          text        NOT NULL,\n" +
                    "    volume        numeric     NOT NULL,\n" +
                    "    creating_time timestamptz NOT NULL,\n" +
                    "    x             numeric     NOT NULL,\n" +
                    "    y             numeric     NOT NULL,\n" +
                    "    z             numeric     NOT NULL,\n" +
                    "    UNIQUE(name, volume, creating_time, x, y, z)\n" +
                    ")";

            private static final String CREATE_ITEMS_ITEMS_TABLE = "" +
                    "CREATE TABLE IF NOT EXISTS \"%1$s\"(\n" +
                    "    items     integer,\n" +
                    "    hash_code integer,\n" +
                    "    PRIMARY KEY(items, hash_code),\n" +
                    "    FOREIGN KEY(items) REFERENCES \"%2$s\"(items)\n" +
                    "        ON DELETE CASCADE,\n" +
                    "    FOREIGN KEY(hash_code) REFERENCES \"%3$s\"(hash_code)\n" +
                    "        ON DELETE RESTRICT\n" +
                    ")";

            private static final String INSERT_CREATING_TIME = "" +
                    "INSERT INTO \"%1$s\"(key, value) VALUES('Creating time', ?)\n" +
                    "ON CONFLICT(key) DO NOTHING";
        }

        private static class Get {

            private static class LivingObjects {

                private static final String ALL = "" +
                        "SELECT name, volume, creating_time, x, y, z, lives, items FROM \"%1$s\"";

                private static final String LIMITED = "" +
                        "SELECT name, volume, creating_time, x, y, z, lives, items FROM \"%1$s\" LIMIT ?";
            }

            private static final String ITEMS = "" +
                    "SELECT items, name, volume, creating_time, x, y, z\n" +
                    "FROM \"%1$s\" INNER JOIN \"%2$s\" USING(hash_code)\n" +
                    "WHERE \"%1$s\".items = ANY(?)";
        }

        private static class AddAll {

            private static final String ITEMS_HC = "" +
                    "INSERT INTO \"%1$s\"(items) VALUES(?) ON CONFLICT DO NOTHING";

            private static final String LIVING_OBJECT = "" +
                    "INSERT INTO \"%1$s\"(hash_code, name, volume, creating_time, x, y, z, lives, items)\n" +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

            private static final String ITEM = "" +
                    "INSERT INTO \"%1$s\"(hash_code, name, volume, creating_time, x, y, z)\n" +
                    "VALUES(?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

            private static final String ITEMS_ITEM = "" +
                    "INSERT INTO \"%1$s\"(items, hash_code) VALUES(?, ?) ON CONFLICT DO NOTHING";
        }

        private static class Remove {

            private static class LivingObject {

                private static final String EQUAL = "DELETE FROM \"%1$s\" WHERE hash_code = ?";

                private static final String LOWER = "" +
                        "WITH items_metrics AS (\n" +
                        "    SELECT items, SUM(volume) AS volume\n" +
                        "    FROM \"%2$s\" INNER JOIN \"%3$s\" USING(hash_code)\n" +
                        "    GROUP BY items\n" +
                        ")\n" +
                        "DELETE FROM \"%1$s\"\n" +
                        "WHERE (name < ?\n" +
                        "   OR (name = ?\n" +
                        "  AND (volume < ?\n" +
                        "   OR (volume = ?\n" +
                        "  AND (creating_time < ?\n" +
                        "   OR (creating_time = ?\n" +
                        "  AND (x < ?\n" +
                        "   OR (x = ?\n" +
                        "  AND (y < ?\n" +
                        "   OR (y = ?\n" +
                        "  AND (z < ?\n" +
                        "   OR (z = ?\n" +
                        "  AND (lives < ?\n" +
                        "   OR (lives = ?\n" +
                        "  AND ((SELECT volume FROM items_metrics WHERE items = items) < ?\n" +
                        "      )))))))))))))))\n" +
                        "RETURNING items";

                private static final String GREATER = "" +
                        "WITH items_metrics AS (\n" +
                        "    SELECT items, SUM(volume) AS volume\n" +
                        "    FROM \"%2$s\" INNER JOIN \"%3$s\" USING(hash_code)\n" +
                        "    GROUP BY items\n" +
                        ")\n" +
                        "DELETE FROM \"%1$s\"\n" +
                        "WHERE (name > ?\n" +
                        "   OR (name = ?\n" +
                        "  AND (volume > ?\n" +
                        "   OR (volume = ?\n" +
                        "  AND (creating_time > ?\n" +
                        "   OR (creating_time = ?\n" +
                        "  AND (x > ?\n" +
                        "   OR (x = ?\n" +
                        "  AND (y > ?\n" +
                        "   OR (y = ?\n" +
                        "  AND (z > ?\n" +
                        "   OR (z = ?\n" +
                        "  AND (lives > ?\n" +
                        "   OR (lives = ?\n" +
                        "  AND ((SELECT volume FROM items_metrics WHERE items = items) > ?\n" +
                        "      )))))))))))))))\n" +
                        "RETURNING items";
            }

            private static final String ITEMS_HCS = "" +
                    "DELETE FROM \"%1$s\" WHERE items NOT IN (SELECT DISTINCT items FROM \"%2$s\") AND items = ANY(?)";

            private static final String ITEMS = "" +
                    "DELETE FROM \"%1$s\" WHERE hash_code NOT IN (SELECT DISTINCT hash_code FROM \"%2$s\")";
        }

        private static class Load {

            private static final String CLEAN = "" +
                    "TRUNCATE \"%1$s\", \"%2$s\", \"%3$s\", \"%4$s\", \"%5$s\"\n" +
                    "    RESTART IDENTITY CASCADE";

            private static final String METADATA = "" +
                    "INSERT INTO \"%1$s\"(key, value) VALUES(?, ?)\n" +
                    "ON CONFLICT(key) DO UPDATE SET value = EXCLUDED.value";
        }

        private static final String GET_METADATA = "" +
                "SELECT key, value FROM \"%1$s\"\n" +
                "    WHERE key <> ALL (ARRAY['Collection size', 'Collection type'])\n" +
                "UNION SELECT 'Collection size', COUNT(*)::text FROM \"%2$s\"\n" +
                "UNION SELECT 'Collection type', 'HashSet'";
    }

    private static final String DEFAULT_METADATA_TABLE_NAME = "metadata";
    private static final String DEFAULT_LIVING_OBJECTS_TABLE_NAME = "living_objects";
    private static final String DEFAULT_ITEMS_HCS_TABLE_NAME = "items_hcs";
    private static final String DEFAULT_ITEMS_ITEMS_TABLE_NAME = "items_items";
    private static final String DEFAULT_ITEMS_TABLE_NAME = "items";

    private static final Logger log = Loggers.getLogger(DatabaseCollectionModel.class.getName());

    private final Connection connection;
    private final String metadataTableName;
    private final String livingObjectsTableName;
    private final String itemsHcsTableName;
    private final String itemsItemsTableName;
    private final String itemsTableName;

    private final Map<String, PreparedStatement> statements = new ConcurrentHashMap<>();

    public DatabaseCollectionModel(Connection connection) throws SQLException {
        this(connection,
                DEFAULT_METADATA_TABLE_NAME,
                DEFAULT_LIVING_OBJECTS_TABLE_NAME,
                DEFAULT_ITEMS_HCS_TABLE_NAME,
                DEFAULT_ITEMS_ITEMS_TABLE_NAME,
                DEFAULT_ITEMS_TABLE_NAME
        );
    }

    public DatabaseCollectionModel(
            Connection connection,
            String metadataTableName,
            String livingObjectsTableName,
            String itemsHcsTableName,
            String itemsItemsTableName,
            String itemsTableName
    ) throws SQLException {
        this.connection = connection;

        this.metadataTableName = metadataTableName;
        this.livingObjectsTableName = livingObjectsTableName;
        this.itemsHcsTableName = itemsHcsTableName;
        this.itemsItemsTableName = itemsItemsTableName;
        this.itemsTableName = itemsTableName;

        validateDatabase();
    }

    @Override
    public int add(LivingObject livingObject) {
        return addAll(Collections.singleton(livingObject));
    }

    public int addAll(Collection<LivingObject> livingObjects) {
        return templateTransaction(() -> doAddAll(livingObjects));
    }

    private int doAddAll(Collection<LivingObject> livingObjects) throws SQLException {
        final PreparedStatement itemsHcStatement = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("doAddAll.itemsHc", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.AddAll.ITEMS_HC,
                                itemsHcsTableName)))));

        final PreparedStatement livingObjectStatement = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("doAddAll.livingObject", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.AddAll.LIVING_OBJECT,
                                livingObjectsTableName)))));

        final PreparedStatement itemStatement = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("doAddAll.item", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.AddAll.ITEM,
                                itemsTableName)))));

        final PreparedStatement itemsItemsStatement = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("doAddAll.itemsItem", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.AddAll.ITEMS_ITEM,
                                itemsItemsTableName)))));

        int ret = 0;
        for (LivingObject livingObject : livingObjects) {
            final int itemsHashCode = livingObject.getItems().hashCode();

            int pointer = 0;
            itemsHcStatement.setInt(++pointer, itemsHashCode);
            itemsHcStatement.executeUpdate();

            pointer = 0;
            livingObjectStatement.setInt(++pointer, livingObject.hashCode());
            livingObjectStatement.setString(++pointer, livingObject.getName());
            livingObjectStatement.setDouble(++pointer, livingObject.getVolume());
            livingObjectStatement.setTimestamp(++pointer, Timestamp.valueOf(livingObject.getCreatingTime()));
            livingObjectStatement.setDouble(++pointer, livingObject.getX());
            livingObjectStatement.setDouble(++pointer, livingObject.getY());
            livingObjectStatement.setDouble(++pointer, livingObject.getZ());
            livingObjectStatement.setBoolean(++pointer, livingObject.isLives());
            livingObjectStatement.setInt(++pointer, itemsHashCode);
            ret += livingObjectStatement.executeUpdate();

            if (ret > 0) {
                for (Object item : livingObject.getItems()) {
                    final int itemHashCode = item.hashCode();

                    pointer = 0;
                    itemStatement.setInt(++pointer, itemHashCode);
                    itemStatement.setString(++pointer, item.getName());
                    itemStatement.setDouble(++pointer, item.getVolume());
                    itemStatement.setTimestamp(++pointer, Timestamp.valueOf(item.getCreatingTime()));
                    itemStatement.setDouble(++pointer, item.getX());
                    itemStatement.setDouble(++pointer, item.getY());
                    itemStatement.setDouble(++pointer, item.getZ());
                    itemStatement.executeUpdate();

                    pointer = 0;
                    itemsItemsStatement.setInt(++pointer, itemsHashCode);
                    itemsItemsStatement.setInt(++pointer, itemHashCode);
                    itemsItemsStatement.executeUpdate();
                }
            }
        }

        return ret;
    }

    @Override
    public int remove(LivingObject livingObject) {
        return templateTransaction(() -> {
            final PreparedStatement livingObjectStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("remove.livingObject.equal", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.Remove.LivingObject.EQUAL,
                                    livingObjectsTableName)))));

            livingObjectStatement.setInt(1, livingObject.hashCode());
            final int ret = livingObjectStatement.executeUpdate();

            removeItems(Collections.singleton(livingObject.getItems().hashCode()));
            return ret;
        });
    }

    @Override
    public int removeLower(LivingObject livingObject) {
        return templateTransaction(() -> {
            final PreparedStatement livingObjectStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("remove.livingObject.lower", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.Remove.LivingObject.LOWER,
                                    livingObjectsTableName, itemsItemsTableName, itemsTableName)))));

            int pointer = 0;
            livingObjectStatement.setString(++pointer, livingObject.getName());
            livingObjectStatement.setString(++pointer, livingObject.getName());
            livingObjectStatement.setDouble(++pointer, livingObject.getVolume());
            livingObjectStatement.setDouble(++pointer, livingObject.getVolume());
            livingObjectStatement.setTimestamp(++pointer, Timestamp.valueOf(livingObject.getCreatingTime()));
            livingObjectStatement.setTimestamp(++pointer, Timestamp.valueOf(livingObject.getCreatingTime()));
            livingObjectStatement.setDouble(++pointer, livingObject.getX());
            livingObjectStatement.setDouble(++pointer, livingObject.getX());
            livingObjectStatement.setDouble(++pointer, livingObject.getY());
            livingObjectStatement.setDouble(++pointer, livingObject.getY());
            livingObjectStatement.setDouble(++pointer, livingObject.getZ());
            livingObjectStatement.setDouble(++pointer, livingObject.getZ());
            livingObjectStatement.setBoolean(++pointer, livingObject.isLives());
            livingObjectStatement.setBoolean(++pointer, livingObject.isLives());
            livingObjectStatement.setDouble(++pointer, livingObject.getItems()
                    .stream().mapToDouble(Object::getVolume).sum());

            final List<Integer> hashCodes = mapResultSet(livingObjectStatement.executeQuery(), row ->
                    row.getInt(1)).collect(Collectors.toList());

            removeItems(new HashSet<>(hashCodes));
            return hashCodes.size();
        });
    }

    @Override
    public int removeGreater(LivingObject livingObject) {
        return templateTransaction(() -> {
            final PreparedStatement livingObjectStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("remove.livingObject.greater", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.Remove.LivingObject.GREATER,
                                    livingObjectsTableName, itemsItemsTableName, itemsTableName)))));

            int pointer = 0;
            livingObjectStatement.setString(++pointer, livingObject.getName());
            livingObjectStatement.setString(++pointer, livingObject.getName());
            livingObjectStatement.setDouble(++pointer, livingObject.getVolume());
            livingObjectStatement.setDouble(++pointer, livingObject.getVolume());
            livingObjectStatement.setTimestamp(++pointer, Timestamp.valueOf(livingObject.getCreatingTime()));
            livingObjectStatement.setTimestamp(++pointer, Timestamp.valueOf(livingObject.getCreatingTime()));
            livingObjectStatement.setDouble(++pointer, livingObject.getX());
            livingObjectStatement.setDouble(++pointer, livingObject.getX());
            livingObjectStatement.setDouble(++pointer, livingObject.getY());
            livingObjectStatement.setDouble(++pointer, livingObject.getY());
            livingObjectStatement.setDouble(++pointer, livingObject.getZ());
            livingObjectStatement.setDouble(++pointer, livingObject.getZ());
            livingObjectStatement.setBoolean(++pointer, livingObject.isLives());
            livingObjectStatement.setBoolean(++pointer, livingObject.isLives());
            livingObjectStatement.setDouble(++pointer, livingObject.getItems()
                    .stream().mapToDouble(Object::getVolume).sum());

            final List<Integer> hashCodes = mapResultSet(livingObjectStatement.executeQuery(), row ->
                    row.getInt(1)).collect(Collectors.toList());

            removeItems(new HashSet<>(hashCodes));
            return hashCodes.size();
        });
    }

    private void removeItems(Set<Integer> hashCodes) throws SQLException {
        if (hashCodes.isEmpty()) {
            return;
        }

        final PreparedStatement itemsHcStatement = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("remove.itemsHcs", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.Remove.ITEMS_HCS,
                                itemsHcsTableName, livingObjectsTableName)))));

        final PreparedStatement itemStatement = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("remove.items", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.Remove.ITEMS,
                                itemsTableName, itemsItemsTableName)))));

        itemsHcStatement.setArray(1, connection.createArrayOf("integer", hashCodes.toArray(arrayOf())));
        itemsHcStatement.executeUpdate();

        itemStatement.executeUpdate();
    }

    @Override
    public Map<String, String> getMetadata() {
        return template(() -> {
            final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("getMetadata", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.GET_METADATA,
                                    metadataTableName, livingObjectsTableName)))));

            return mapResultSet(preparedStatement.executeQuery(), row ->
                    new HashMap.SimpleImmutableEntry<>(row.getString(1), row.getString(2)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        });
    }

    @Override
    public Set<LivingObject> get() {
        return template(() -> {
            final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("get.livingObjects.all", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.Get.LivingObjects.ALL,
                                    livingObjectsTableName)))));

            return get(preparedStatement.executeQuery());
        });
    }

    @Override
    public Set<LivingObject> get(long count) {
        return template(() -> {
            final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("get.livingObjects.limited", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.Get.LivingObjects.LIMITED,
                                    livingObjectsTableName)))));

            preparedStatement.setLong(1, count);

            return get(preparedStatement.executeQuery());
        });
    }

    private Set<LivingObject> get(ResultSet livingObjectsResult) throws SQLException {
        final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("get.items", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.Get.ITEMS,
                                itemsItemsTableName, itemsTableName)))));

        final Map<LivingObject, Integer> livingObjects = new HashMap<>();
        while (livingObjectsResult.next()) {
            int pointer = 0;

            final String name = livingObjectsResult.getString(++pointer);
            final double volume = livingObjectsResult.getDouble(++pointer);
            final Timestamp creatingTime = livingObjectsResult.getTimestamp(++pointer);
            final double x = livingObjectsResult.getDouble(++pointer);
            final double y = livingObjectsResult.getDouble(++pointer);
            final double z = livingObjectsResult.getDouble(++pointer);
            final boolean lives = livingObjectsResult.getBoolean(++pointer);
            final Integer items = livingObjectsResult.getInt(++pointer);

            final LivingObject livingObject = new LivingObject(name) {{
                setVolume(volume);
                setCreatingTime(creatingTime.toLocalDateTime());
                setX(x);
                setY(y);
                setZ(z);
                setLivingObjectLives(this, lives);
            }};

            livingObjects.put(livingObject, items);
        }

        livingObjectsResult.close();
        preparedStatement.setArray(1, connection.createArrayOf("integer", livingObjects.values().toArray(arrayOf())));
        final ResultSet itemsResult = preparedStatement.executeQuery();

        final Map<Integer, Set<Object>> items = new HashMap<>();
        while (itemsResult.next()) {
            int pointer = 0;

            final Integer hashCode = itemsResult.getInt(++pointer);
            final String name = itemsResult.getString(++pointer);
            final double volume = itemsResult.getDouble(++pointer);
            final Timestamp creatingTime = itemsResult.getTimestamp(++pointer);
            final double x = itemsResult.getDouble(++pointer);
            final double y = itemsResult.getDouble(++pointer);
            final double z = itemsResult.getDouble(++pointer);

            items.computeIfAbsent(hashCode, integer ->
                    new HashSet<>()).add(new Object(name, volume,
                    creatingTime.toLocalDateTime(), x, y, z) {});
        }

        itemsResult.close();
        final Set<LivingObject> ret = new HashSet<>();
        for (LivingObject livingObject : livingObjects.keySet()) {
            final Integer hashCode = livingObjects.get(livingObject);

            final Set<Object> livingObjectItems = items.get(hashCode);
            if (livingObjectItems != null && !livingObjectItems.isEmpty()) {
                livingObject.getItems().addAll(livingObjectItems);
            }

            ret.add(livingObject);
        }

        return Collections.unmodifiableSet(ret);
    }

    public void load(Collection<LivingObject> livingObjects, Map<String, String> metadata) {
        templateTransaction(() -> {
            final PreparedStatement cleanStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("load.clean", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.Load.CLEAN, metadataTableName,
                                    livingObjectsTableName, itemsHcsTableName, itemsItemsTableName, itemsTableName)))));

            final PreparedStatement metadataStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("load.metadata", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.Load.METADATA,
                                    metadataTableName)))));

            cleanStatement.executeUpdate();
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                int pointer = 0;

                metadataStatement.setString(++pointer, entry.getKey());
                metadataStatement.setString(++pointer, entry.getValue());
                metadataStatement.executeUpdate();
            }

            doAddAll(livingObjects);
            return null;
        });
    }

    private <T> T templateTransaction(ThrowingSupplier<T, SQLException> code) {
        return template(() -> {
            synchronized (connection) {
                final boolean oldAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                try {
                    final T ret = code.throwingGet();
                    connection.commit();

                    return ret;
                } catch (Throwable e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(oldAutoCommit);
                }
            }
        });
    }

    private <T> T template(ThrowingSupplier<T, SQLException> code) {
        try {
            validateDatabase();

            return code.throwingGet();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "An error occurred while an SQL query performing. Is table structure correct?", e);
            throw new RuntimeException("an error occurred while an SQL query performing", e);
        } catch (Throwable e) {
            log.log(Level.SEVERE, "An exception thrown from template method", e);
            throw e;
        }
    }

    private void validateDatabase() throws SQLException {
        final PreparedStatement createMetadataTable = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("validateDatabase.createMetadataTable", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.ValidateDatabase.CREATE_METADATA_TABLE,
                                metadataTableName)))));

        final PreparedStatement createItemsHcsTable = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("validateDatabase.createItemsHcsTable", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.ValidateDatabase.CREATE_ITEMS_HCS_TABLE,
                                itemsHcsTableName)))));

        final PreparedStatement createLivingObjectsTable = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("validateDatabase.createLivingObjectsTable", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.ValidateDatabase.CREATE_LIVING_OBJECTS_TABLE,
                                livingObjectsTableName, itemsHcsTableName)))));

        final PreparedStatement createItemsTable = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("validateDatabase.createItemsTable", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.ValidateDatabase.CREATE_ITEMS_TABLE,
                                itemsTableName)))));

        final PreparedStatement createItemsItemsTable = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("validateDatabase.createItemsItemsTable", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.ValidateDatabase.CREATE_ITEMS_ITEMS_TABLE,
                                itemsItemsTableName, itemsHcsTableName, itemsTableName)))));

        final PreparedStatement insertCreatingTime = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("validateDatabase.insertCreatingTime", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.ValidateDatabase.INSERT_CREATING_TIME,
                                metadataTableName)))));

        createMetadataTable.executeUpdate();
        createItemsHcsTable.executeUpdate();
        createLivingObjectsTable.executeUpdate();
        createItemsTable.executeUpdate();
        createItemsItemsTable.executeUpdate();

        insertCreatingTime.setString(1, LocalDateTime.now().format(Object.DATE_TIME_FORMATTER));
        insertCreatingTime.executeUpdate();
    }
}

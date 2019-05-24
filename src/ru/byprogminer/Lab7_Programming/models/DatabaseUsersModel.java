package ru.byprogminer.Lab7_Programming.models;

import ru.byprogminer.Lab5_Programming.throwing.ThrowingSupplier;
import ru.byprogminer.Lab7_Programming.Credentials;
import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.byprogminer.Lab5_Programming.LabUtils.*;

/**
 * users:
 * +----------+-----------+-----------------+
 * |    id    | bigserial |   PRIMARY KEY   |
 * | username |   text    | NOT NULL UNIQUE |
 * |  email   |   text    | NOT NULL UNIQUE |
 * | password | char(40)  |     NOT NULL    |
 * |   salt   | char(12)  |     NOT NULL    |
 * +----------+-----------+-----------------+
 *
 * users_permissions:
 * +------------+--------+-------------------------+----------------------------------+
 * |  user_id   | bigint | PRIMARY KEY(user_id,    | FOREIGN KEY REFERENCES users(id) |
 * | permission |  text  |             permission) |                                  |
 * +------------+--------+-------------------------+----------------------------------+
 */
public class DatabaseUsersModel implements UsersModel {

    private static final class Query {

        private static final class ValidateDatabase {

            private static final String CREATE_USERS_TABLE = "" +
                    "CREATE TABLE IF NOT EXISTS \"%1$s\"(\n" +
                    "    id       bigserial PRIMARY KEY,\n" +
                    "    username text      NOT NULL UNIQUE,\n" +
                    "    email    text      NOT NULL UNIQUE,\n" +
                    "    password char(40)  NOT NULL,\n" +
                    "    salt     char(12)  NOT NULL\n" +
                    ")";

            private static final String CREATE_PERMISSIONS_TABLE = "" +
                    "CREATE TABLE IF NOT EXISTS \"%1$s\"(\n" +
                    "    user_id    bigint,\n" +
                    "    permission text,\n" +
                    "    PRIMARY KEY(user_id, permission),\n" +
                    "    FOREIGN KEY(user_id) REFERENCES \"%2$s\"(id)\n" +
                    "        ON DELETE CASCADE\n" +
                    ")";
        }

        private static final String ADD = "" +
                "INSERT INTO \"%1$s\"(username, email, password, salt) VALUES(?, ?, ?, ?)\n" +
                "    ON CONFLICT DO NOTHING RETURNING id";

        private static final String CHECK = "SELECT password, salt FROM \"%1$s\" WHERE username = ?";

        private static final String REMOVE = "DELETE FROM \"%1$s\" WHERE username = ?";

        private static final String SET_PASSWORD = "UPDATE \"%1$s\" SET password = ?, salt = ? WHERE username = ?";

        private static final String SET_USERNAME = "UPDATE \"%1$s\" SET username = ? WHERE username = ?";

        private static final String GET = "SELECT username, email FROM \"%1$s\"";

        private static final String GET_EMAIL = "SELECT email FROM \"%1$s\" WHERE username = ?";

        private static final String GET_BY_EMAIL = "SELECT username FROM \"%1$s\" WHERE email = ?";

        private static final String HAS_PERMISSION = "" +
                "SELECT true FROM \"%1$s\" WHERE user_id = (SELECT id FROM \"%2$s\" WHERE username = ?)\n" +
                "    AND permission = ANY(?)";

        private static final String GIVE_PERMISSION = "" +
                "INSERT INTO \"%1$s\"(user_id, permission) VALUES((SELECT id FROM \"%2$s\" WHERE username = ?), ?)\n" +
                "    ON CONFLICT DO NOTHING";

        private static final String TAKE_PERMISSION = "" +
                "DELETE FROM \"%1$s\" WHERE user_id = (SELECT id FROM \"%2$s\" WHERE username = ?) AND permission = ?\n";

        private static final String GET_PERMISSIONS = "" +
                "SELECT permission FROM \"%1$s\" WHERE user_id = (SELECT id FROM \"%2$s\" WHERE username = ?)";
    }

    private static final String PASSWORD_HASH_ALGORITHM = "SHA-1";
    private static final byte[] PASSWORD_PEPPER = "CUL/{)Rv9O1S".getBytes();

    private static final int PASSWORD_SALT_LENGTH = 12;
    private static final String PASSWORD_SALT_ALPHABET = PASSWORD_ALPHABET;

    private static final String DEFAULT_USERS_TABLE_NAME = "users";
    private static final String DEFAULT_PERMISSIONS_TABLE_NAME = "permissions";

    private final Logger log = Loggers.getObjectLogger(this);

    private final Connection connection;
    private final String usersTableName;
    private final String permissionsTableName;

    private final Map<String, PreparedStatement> statements = new ConcurrentHashMap<>();
    private final SecureRandom saltRandom = new SecureRandom();

    public DatabaseUsersModel(Connection connection) throws SQLException {
        this(connection, DEFAULT_USERS_TABLE_NAME, DEFAULT_PERMISSIONS_TABLE_NAME);
    }

    public DatabaseUsersModel(
            Connection connection,
            String usersTableName,
            String permissionsTableName
    ) throws SQLException {
        this.connection = connection;

        this.usersTableName = usersTableName;
        this.permissionsTableName = permissionsTableName;

        validateDatabase();
    }

    @Override
    public boolean add(Credentials credentials, String email) {
        return template(() -> {
            final boolean ret = doAdd(credentials, email) != null;

            for (String permission : doGetPermissions(DEFAULT_USER)) {
                doGivePermission(credentials.username, permission);
            }

            return ret;
        });
    }

    private Long doAdd(Credentials credentials, String email) throws SQLException {
        final String salt = generateSalt();
        final String hash = hashPassword(credentials.password, salt);

        return doAdd(credentials.username, email, hash, salt);
    }

    private Long doAdd(String username, String email, String passwordHash, String salt) throws SQLException {
        final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("add", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.ADD,
                                usersTableName)))));

        int pointer = 0;
        preparedStatement.setString(++pointer, username);
        preparedStatement.setString(++pointer, email);
        preparedStatement.setString(++pointer, passwordHash);
        preparedStatement.setString(++pointer, salt);
        return mapResultSet(preparedStatement.executeQuery(), row ->
                row.getLong(1)).findAny().orElse(null);
    }

    @Override
    public boolean check(Credentials credentials) {
        if (DEFAULT_USER.equals(credentials.username)) {
            return false;
        }

        return template(() -> {
            final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("check", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.CHECK,
                                    usersTableName)))));

            preparedStatement.setString(1, credentials.username);
            return mapResultSet(preparedStatement.executeQuery(), row ->
                    hashPassword(credentials.password, row.getString(2)).equals(row.getString(1)))
                    .findAny().orElse(false);
        });
    }

    @Override
    public boolean remove(String username) {
        return template(() -> {
            final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("remove", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.REMOVE,
                                    usersTableName)))));

            preparedStatement.setString(1, username);
            return preparedStatement.executeUpdate() > 0;
        });
    }

    @Override
    public boolean setPassword(String username, String password) {
        return templateTransaction(() -> {
            final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("setPassword", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.SET_PASSWORD,
                                    usersTableName)))));

            int pointer = 0;
            final String salt;
            preparedStatement.setString(++pointer, hashPassword(password, salt = generateSalt()));
            preparedStatement.setString(++pointer, salt);
            preparedStatement.setString(++pointer, username);
            return preparedStatement.executeUpdate() > 0;
        });
    }

    @Override
    public boolean setUsername(String username, String newUsername) {
        return templateTransaction(() -> {
            final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("setUsername", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.SET_USERNAME,
                                    usersTableName)))));


            int pointer = 0;
            preparedStatement.setString(++pointer, newUsername);
            preparedStatement.setString(++pointer, username);
            return preparedStatement.executeUpdate() > 0;
        });
    }

    @Override
    public Map<String, String> get() {
        return template(() -> {
            final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("get", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.GET,
                                    usersTableName)))));

            return mapResultSet(preparedStatement.executeQuery(), row ->
                    new HashMap.SimpleImmutableEntry<>(row.getString(1), row.getString(2)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        });
    }

    @Override
    public String getEmail(String username) {
        return template(() -> {
            final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("getEmail", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.GET_EMAIL,
                                    usersTableName)))));

            preparedStatement.setString(1, username);
            return mapResultSet(preparedStatement.executeQuery(), row ->
                    row.getString(1)).findAny().orElse(null);
        });
    }

    @Override
    public String getByEmail(String email) {
        return template(() -> {
            final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("getByEmail", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.GET_BY_EMAIL,
                                    usersTableName)))));

            preparedStatement.setString(1, email);
            return mapResultSet(preparedStatement.executeQuery(), row ->
                    row.getString(1)).findAny().orElse(null);
        });
    }

    @Override
    public boolean hasPermission(String username, Set<String> permissions) {
        return template(() -> doHasPermission(username, permissions));
    }

    private boolean doHasPermission(String username, Set<String> primaryPermissions) throws SQLException {
        final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("hasPermission", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.HAS_PERMISSION,
                                permissionsTableName, usersTableName)))));

        final Set<String> permissions = new HashSet<>();
        for (String permission : primaryPermissions) {
            permissions.addAll(UsersModel.getAllPermissionLevels(permission));
        }

        int pointer = 0;
        preparedStatement.setString(++pointer, username);
        preparedStatement.setArray(++pointer, connection.createArrayOf("text", permissions.toArray(arrayOf())));
        return mapResultSet(preparedStatement.executeQuery(), row -> row.getBoolean(1)).findAny().orElse(false);
    }

    @Override
    public void givePermission(String username, String permission) {
        template(supplier(() -> doGivePermission(username, permission)));
    }

    private void doGivePermission(String username, String permission) throws SQLException {
        if (doHasPermission(username, Collections.singleton(permission))) {
            return;
        }

        final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("givePermission", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.GIVE_PERMISSION,
                                permissionsTableName, usersTableName)))));

        int pointer = 0;
        preparedStatement.setString(++pointer, username);
        preparedStatement.setString(++pointer, permission);
        preparedStatement.executeUpdate();
    }

    @Override
    public void takePermission(String username, String permission) {
        template(supplier(() -> {
            final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                    statements.computeIfAbsent("takePermission", throwing().function(s ->
                            connection.prepareStatement(String.format(Query.TAKE_PERMISSION,
                                    permissionsTableName, usersTableName)))));

            int pointer = 0;
            preparedStatement.setString(++pointer, username);
            preparedStatement.setString(++pointer, permission);
            preparedStatement.executeUpdate();
        }));
    }

    @Override
    public Set<String> getPermissions(String username) {
        return template(() -> doGetPermissions(username));
    }

    private Set<String> doGetPermissions(String username) throws SQLException {
        final PreparedStatement preparedStatement = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("getPermissions", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.GET_PERMISSIONS,
                                permissionsTableName, usersTableName)))));

        preparedStatement.setString(1, username);
        return mapResultSet(preparedStatement.executeQuery(), row ->
                row.getString(1)).collect(Collectors.toSet());
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
        final PreparedStatement createUsersTable = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("validateDatabase.createUsersTable", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.ValidateDatabase.CREATE_USERS_TABLE,
                                usersTableName)))));

        final PreparedStatement createPermissionsTable = throwing().unwrap(SQLException.class, () ->
                statements.computeIfAbsent("validateDatabase.createPermissionsTable", throwing().function(s ->
                        connection.prepareStatement(String.format(Query.ValidateDatabase.CREATE_PERMISSIONS_TABLE,
                                permissionsTableName, usersTableName)))));

        createUsersTable.executeUpdate();
        createPermissionsTable.executeUpdate();

        if (doAdd(DEFAULT_USER, DEFAULT_USER, "", "") != null) {
            for (String permission : DEFAULT_USER_PERMISSIONS) {
                doGivePermission(DEFAULT_USER, permission);
            }
        }

        if (doAdd(new Credentials(SUPERUSER, ""), SUPERUSER) != null) {
            doGivePermission(SUPERUSER, "*");
        }
    }

    private String generateSalt() {
        final char[] saltChars = new char[PASSWORD_SALT_LENGTH];

        for (int i = 0; i < saltChars.length; ++i) {
            saltChars[i] = PASSWORD_SALT_ALPHABET.charAt(saltRandom.nextInt(PASSWORD_SALT_ALPHABET.length()));
        }

        return new String(saltChars);
    }

    private String hashPassword(String password, String salt) {
        final MessageDigest messageDigest;

        try {
            messageDigest = MessageDigest.getInstance(PASSWORD_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.SEVERE, "no such password hash algorithm " + PASSWORD_HASH_ALGORITHM, e);
            throw new RuntimeException("no such password hash algorithm " + PASSWORD_HASH_ALGORITHM, e);
        }

        messageDigest.update(PASSWORD_PEPPER);
        messageDigest.update(password.getBytes());
        messageDigest.update(salt.getBytes());
        final byte[] digest = messageDigest.digest();

        final StringBuilder hash = new StringBuilder();
        for (byte b : digest) {
            hash.append(String.format("%02x", b));
        }

        return hash.toString();
    }
}

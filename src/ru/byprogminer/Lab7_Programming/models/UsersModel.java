package ru.byprogminer.Lab7_Programming.models;

import ru.byprogminer.Lab7_Programming.Credentials;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;

/**
 * Authorize as default user isn't allowed.
 *
 * All permissions represents in hierarchic dot-separated format.
 * If user has permission "a.b.c" or "a.b.c.*", he has all permissions that starts with "a.b.c.".
 *
 * Default superuser has permission "*".
 * All newly created users get all permissions that has default user.
 */
public interface UsersModel {

    String PERMISSIONS_SEPARATOR = ".";
    String PERMISSIONS_WILDCARD = "*";

    String SUPERUSER = "root";
    String DEFAULT_USER = "default";
    String[] DEFAULT_USER_PERMISSIONS = arrayOf(
            "collection.add",
            "collection.remove.own",
            "collection.removeLower.own",
            "collection.removeGreater.own",
            "collection.importObjects"
    );

    boolean add(Credentials credentials, String email);
    boolean check(Credentials credentials);

    boolean remove(String username);
    boolean setPassword(String username, String password);
    boolean setUsername(String username, String newUsername);

    Map<String, String> get();
    String getEmail(String username);
    String getByEmail(String email);

    // TODO load

    default boolean hasPermission(String username, String permission) {
        return hasPermission(username, Collections.singleton(permission));
    }

    boolean hasPermission(String username, Set<String> permissions);
    void givePermission(String username, String permission);
    void takePermission(String username, String permission);
    Set<String> getPermissions(String username);

    static Set<String> getAllPermissionLevels(String permission) {
        final Set<String> permissions = new HashSet<>();

        final StringBuilder builder = new StringBuilder();
        for (int i = permission.indexOf(PERMISSIONS_SEPARATOR), j = 0; true; j = i + 1, i = permission.indexOf(PERMISSIONS_SEPARATOR, i + 1)) {
            if (i == -1) {
                i = permission.length();
            }

            permissions.add(builder.toString() + PERMISSIONS_WILDCARD);

            builder.append(permission, j, i);
            permissions.add(builder.toString());

            if (i < permission.length()) {
                builder.append(PERMISSIONS_SEPARATOR);
            } else {
                break;
            }
        }

        return permissions;
    }
}

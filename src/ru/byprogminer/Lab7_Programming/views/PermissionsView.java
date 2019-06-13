package ru.byprogminer.Lab7_Programming.views;

import ru.byprogminer.Lab7_Programming.View;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PermissionsView extends View {

    public final Set<String> permissions;

    public PermissionsView(String error) {
        this(new HashSet<>(), error);
    }

    public PermissionsView(Set<String> permissions) {
        this(permissions, null);
    }

    public PermissionsView(Set<String> permissions, String error) {
        super(error);

        this.permissions = Collections.unmodifiableSet(new HashSet<>(permissions));
    }
}

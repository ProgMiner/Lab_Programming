package ru.byprogminer.Lab7_Programming.views;

import ru.byprogminer.Lab7_Programming.View;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UsersView extends View {

    public final Set<String> users;

    public UsersView(String error) {
        this(new HashSet<>(), error);
    }

    public UsersView(Set<String> users) {
        this(users, null);
    }

    public UsersView(Set<String> users, String error) {
        super(error);

        this.users = Collections.unmodifiableSet(new HashSet<>(users));
    }
}

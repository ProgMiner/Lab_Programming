package ru.byprogminer.Lab7_Programming;

import java.io.Serializable;
import java.util.Objects;

public class Credentials implements Serializable {

    public final String username;
    public final String password;

    public Credentials(String username, String password) {
        this.username = Objects.requireNonNull(username);
        this.password = Objects.requireNonNull(password);
    }
}

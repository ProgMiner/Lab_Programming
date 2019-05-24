package ru.byprogminer.Lab7_Programming.views;

import java.util.Objects;

public class RegisterView extends OkView {

    public final String username;
    public final String email;
    public final String password;

    public RegisterView(String error) {
        super(error);

        this.username = null;
        this.email = null;
        this.password = null;
    }

    public RegisterView(boolean ok, String username, String email, String password) {
        super(ok);

        this.username = Objects.requireNonNull(username);
        this.email = Objects.requireNonNull(email);
        this.password = password;
    }
}

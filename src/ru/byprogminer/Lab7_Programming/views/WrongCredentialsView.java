package ru.byprogminer.Lab7_Programming.views;

import ru.byprogminer.Lab7_Programming.Credentials;
import ru.byprogminer.Lab7_Programming.View;

public class WrongCredentialsView extends View {

    public final Credentials credentials;

    public WrongCredentialsView(Credentials credentials) {
        super(null);

        this.credentials = credentials;
    }
}

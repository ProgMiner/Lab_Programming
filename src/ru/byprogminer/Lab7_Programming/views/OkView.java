package ru.byprogminer.Lab7_Programming.views;

import ru.byprogminer.Lab7_Programming.View;

public abstract class OkView extends View {

    final public boolean ok;

    public OkView(String error) {
        this(false, error);
    }

    public OkView(boolean ok) {
        this(ok, null);
    }

    public OkView(boolean ok, String error) {
        super(error);

        this.ok = ok;
    }
}

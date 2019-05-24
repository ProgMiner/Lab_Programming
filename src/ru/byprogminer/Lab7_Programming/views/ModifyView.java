package ru.byprogminer.Lab7_Programming.views;

import ru.byprogminer.Lab7_Programming.View;

public abstract class ModifyView extends View {

    final public int affectedRows;

    public ModifyView(String error) {
        this(0, error);
    }

    public ModifyView(int affectedRows) {
        this(affectedRows, null);
    }

    public ModifyView(int affectedRows, String error) {
        super(error);

        this.affectedRows = affectedRows;
    }
}

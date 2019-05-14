package ru.byprogminer.Lab7_Programming.views;

import ru.byprogminer.Lab7_Programming.View;

public abstract class FileView extends View {

    public final String filename;

    public FileView(String filename) {
        super(null);

        this.filename = filename;
    }

    public FileView(String filename, String error) {
        super(error);

        this.filename = filename;
    }
}

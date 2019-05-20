package ru.byprogminer.Lab7_Programming;

import java.io.Serializable;

public abstract class View implements Serializable {

    public final String error;

    public View(String error) {
        this.error = error;
    }
}

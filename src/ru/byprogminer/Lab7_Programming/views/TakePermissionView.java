package ru.byprogminer.Lab7_Programming.views;

public class TakePermissionView extends OkView {

    public TakePermissionView(boolean ok) {
        super(ok);
    }

    public TakePermissionView(String error) {
        super(error);
    }
}

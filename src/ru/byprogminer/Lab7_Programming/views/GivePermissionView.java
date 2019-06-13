package ru.byprogminer.Lab7_Programming.views;

public class GivePermissionView extends OkView {

    public GivePermissionView(boolean ok) {
        super(ok);
    }

    public GivePermissionView(String error) {
        super(error);
    }
}

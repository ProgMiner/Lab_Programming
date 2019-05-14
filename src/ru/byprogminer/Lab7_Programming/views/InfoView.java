package ru.byprogminer.Lab7_Programming.views;

import ru.byprogminer.Lab7_Programming.View;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InfoView extends View {

    public final Map<String, String> metadata;

    public InfoView(Map<String, String> metadata) {
        this(metadata, null);
    }

    public InfoView(Map<String, String> metadata, String error) {
        super(error);

        this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
    }
}

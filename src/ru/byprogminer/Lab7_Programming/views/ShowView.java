package ru.byprogminer.Lab7_Programming.views;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab7_Programming.View;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ShowView extends View {

    public final Set<LivingObject> elements;

    public ShowView(String error) {
        this(new HashSet<>(), error);
    }

    public ShowView(Set<LivingObject> elements) {
        this(elements, null);
    }

    public ShowView(Set<LivingObject> elements, String error) {
        super(error);

        this.elements = Collections.unmodifiableSet(new HashSet<>(elements));
    }
}

package ru.byprogminer.Lab7_Programming.views;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab7_Programming.View;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

public class ShowView extends View {

    public final Collection<LivingObject> elements;

    public ShowView(String error) {
        this(new HashSet<>(), error);
    }

    public ShowView(Collection<LivingObject> elements) {
        this(elements, null);
    }

    public ShowView(Collection<LivingObject> elements, String error) {
        super(error);

        this.elements = Collections.unmodifiableList(new LinkedList<>(elements));
    }
}

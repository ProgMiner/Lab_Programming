package ru.byprogminer.Lab3_Programming;

import ru.byprogminer.Lab4_Programming.NotFoundException;

public interface Picker {

    void pickUp(Object thing);

    void lose(Object thing) throws NotFoundException;
}

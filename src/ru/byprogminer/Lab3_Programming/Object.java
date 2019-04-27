package ru.byprogminer.Lab3_Programming;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;

public abstract class Object implements Serializable {

    protected final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
    private final static String STRING_FORMAT = "Object %s with volume %s at (%s, %s, %s) created %s";

    private final String name;

    private double volume, x, y, z;

    private LocalDateTime creatingTime;

    public Object(final String name) {
        this(name, 0, 0D, 0D, 0D, LocalDateTime.now());
    }

    public Object(
            final String name,
            final double volume,
            final LocalDateTime creatingTime
    ) {
        this(name, volume, 0D, 0D, 0D, creatingTime);
    }

    public Object(
            final String name,
            final double volume,
            final double x,
            final double y,
            final double z,
            final LocalDateTime creatingTime
    ) {
        this.name = name;
        this.volume = volume;
        this.x = x;
        this.y = y;
        this.z = z;
        this.creatingTime = creatingTime;
    }

    public String getName() {
        return name;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public LocalDateTime getCreatingTime() {
        return creatingTime;
    }

    public void setCreatingTime(LocalDateTime creatingTime) {
        this.creatingTime = Objects.requireNonNull(creatingTime);
    }

    @Override
    public String toString() {
        return String.format(STRING_FORMAT,
                name, volume, x, y, z, creatingTime.format(DATE_TIME_FORMATTER));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(final java.lang.Object that) {
        if (!(that instanceof Object)) {
            return false;
        }

        Object obj = (Object) that;
        return name.equals(obj.name) &&
                volume == obj.volume &&
                creatingTime.equals(obj.creatingTime) &&
                x == obj.x && y == obj.y && z == obj.z;
    }
}

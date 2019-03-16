package ru.byprogminer.Lab3_Programming;

import java.io.Serializable;
import java.util.Date;

public abstract class Object implements Serializable {

    private final String name;

    private double volume, x, y, z;

    private final Date creatingTime;

    public Object(
            final String name,
            final double volume,
            final Date creatingTime
    ) {
        this(name, volume, 0D, 0D, 0D, creatingTime);
    }

    public Object(
            final String name,
            final double volume,
            final double x,
            final double y,
            final double z,
            final Date creatingTime
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

    public void setVolume(final double volume) {
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

    public Date getCreatingTime() {
        return creatingTime;
    }

    @Override
    public String toString() {
        return "Object " + name + ' ' +
                "at (" + x + ", "
                + y + ", "
                + z + ") " +
                "with volume " + volume + ", " +
                "created at " + creatingTime;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(final java.lang.Object that) {
        return that instanceof Object && ((Object) that).name.equals(name);
    }
}

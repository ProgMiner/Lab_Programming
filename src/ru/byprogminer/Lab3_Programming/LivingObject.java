package ru.byprogminer.Lab3_Programming;

import ru.byprogminer.Lab4_Programming.DeathException;
import ru.byprogminer.Lab4_Programming.NotFoundException;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class LivingObject extends Object implements Hitter, Picker, Moveable, Thinkable, Comparable<LivingObject> {

    private Set<Object> items = new HashSet<>();

    private boolean lives = true;

    public LivingObject(
            final String name,
            final double volume,
            final Date creatingTime
    ) {
        super(name, volume, creatingTime);
    }

    public Set<Object> getItems() {
        return items;
    }

    public void die() {
        lives = false;

        System.out.printf("%s умер.\n", getName());
    }

    public boolean isLives() {
        return lives;
    }

    protected void checkLives() {
        if (!lives) {
            throw new DeathException(this);
        }
    }

    @Override
    public void moveTo(Object target, Move move) {
        checkLives();

        System.out.printf("%s %s к объекту %s.\n", getName(), move.getActionName(), target.getName());
    }

    @Override
    public void moveTo(String target, Move move) {
        checkLives();

        System.out.printf("%s %s к %s.\n", getName(), move.getActionName(), target);
    }

    @Override
    public void moveFor(Object target, Move move) {
        checkLives();

        System.out.printf("%s %s за %s.\n", getName(), move.getActionName(), target.getName());
    }

    @Override
    public void moveFrom(Object enemy, Move move) {
        checkLives();

        System.out.printf("%s %s от %s.\n", getName(), move.getActionName(), enemy.getName());
    }

    @Override
    public void moveFrom(String enemy, Move move) {
        checkLives();

        System.out.printf("%s %s от %s.\n", getName(), move.getActionName(), enemy);
    }

    @Override
    public void hit(Object target) {
        checkLives();

        System.out.printf("%s бьёт %s.\n", getName(), target.getName());
    }

    @Override
    public void hit(Object target, Object by) {
        checkLives();

        System.out.printf("%s бьёт %s с помощью объекта %s.\n", getName(), target.getName(), by.getName());
    }

    @Override
    public void pickUp(Object thing) {
        checkLives();

        if (items.contains(thing)) {
            return;
        }

        items.add(thing);
        System.out.printf("%s взял объект %s.\n", getName(), thing.getName());
    }

    @Override
    public void lose(Object thing) throws NotFoundException {
        if (!items.contains(thing)) {
            throw new NotFoundException(thing);
        }

        items.remove(thing);
        System.out.printf("%s потерял объект %s.\n", getName(), thing.getName());
    }

    @Override
    public void thinkAbout(Object thing) {
        checkLives();

        System.out.printf("%s думает об объекте %s.\n", getName(), thing.getName());
    }

    @Override
    public void think(String thought) {
        checkLives();

        System.out.printf("%s думает %s.\n", getName(), thought);
    }

    @Override
    public int compareTo(LivingObject that) {
        int nameCompare = this.getName().compareTo(that.getName());

        if (nameCompare != 0) {
            return nameCompare;
        }

        if (this.lives != that.lives) {
            return Boolean.compare(this.lives, that.lives);
        }

        if (this.getItems().size() != that.getItems().size()) {
            return this.getItems().size() - that.getItems().size();
        }

        return this.hashCode() - that.hashCode();
    }

    @Override
    public int hashCode() {
        int code = getName().hashCode();

        for (Object o: items) {
            code <<= 1;
            code += o.hashCode();
        }

        code <<= 1;
        code += lives ? 1 : 0;

        return code;
    }

    @Override
    public boolean equals(java.lang.Object object) {
        if (!(object instanceof LivingObject)) {
            return false;
        }

        LivingObject obj = (LivingObject) object;

        if (!obj.getName().equals(this.getName())) {
            return false;
        }

        if (obj.lives != this.lives) {
            return false;
        }

        if (obj.items.size() != this.items.size()) {
            return false;
        }

        return this.items.containsAll(obj.items);
    }

    @Override
    public String toString() {
        return "LivingObject " + getName() + ' ' +
                "at (" + getX() + ", "
                + getY() + ", "
                + getZ() + ") " +
                "with volume " + getVolume() + ", " +
                "created at " + getCreatingTime() + ", " +
                "that is currently " + (lives ? "lives" : "dead") + ", " +
                "with following items: " + items.parallelStream().map(Object::toString)
                .collect(Collectors.joining(", "));
    }
}

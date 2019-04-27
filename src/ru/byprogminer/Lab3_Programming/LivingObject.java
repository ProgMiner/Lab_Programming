package ru.byprogminer.Lab3_Programming;

import ru.byprogminer.Lab4_Programming.DeathException;
import ru.byprogminer.Lab4_Programming.NotFoundException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class LivingObject extends Object implements Hitter, Picker, Movable, Thinkable, Comparable<LivingObject> {

    private final static String STRING_FORMAT = "LivingObject %s with volume %s at (%s, %s, %s) created %s, currently %s, %s";

    private final Set<Object> items = new HashSet<>();

    private volatile boolean lives = true;

    public LivingObject(final String name) {
        super(name);
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

    protected void assertLives() {
        if (!lives) {
            throw new DeathException(this);
        }
    }

    @Override
    public void moveTo(Object target, Move move) {
        assertLives();

        System.out.printf("%s %s к объекту %s.\n", getName(), move.getActionName(), target.getName());
    }

    @Override
    public void moveTo(String target, Move move) {
        assertLives();

        System.out.printf("%s %s к %s.\n", getName(), move.getActionName(), target);
    }

    @Override
    public void moveFor(Object target, Move move) {
        assertLives();

        System.out.printf("%s %s за %s.\n", getName(), move.getActionName(), target.getName());
    }

    @Override
    public void moveFrom(Object enemy, Move move) {
        assertLives();

        System.out.printf("%s %s от %s.\n", getName(), move.getActionName(), enemy.getName());
    }

    @Override
    public void moveFrom(String enemy, Move move) {
        assertLives();

        System.out.printf("%s %s от %s.\n", getName(), move.getActionName(), enemy);
    }

    @Override
    public void hit(Object target) {
        assertLives();

        System.out.printf("%s бьёт %s.\n", getName(), target.getName());
    }

    @Override
    public void hit(Object target, Object by) {
        assertLives();

        System.out.printf("%s бьёт %s с помощью объекта %s.\n", getName(), target.getName(), by.getName());
    }

    @Override
    public void pickUp(Object thing) {
        assertLives();

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
        assertLives();

        System.out.printf("%s думает об объекте %s.\n", getName(), thing.getName());
    }

    @Override
    public void think(String thought) {
        assertLives();

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
    public boolean equals(java.lang.Object that) {
        if (!(that instanceof LivingObject)) {
            return false;
        }

        LivingObject obj = (LivingObject) that;
        return super.equals(obj) && lives == obj.lives && items.equals(obj.items);
    }

    @Override
    public String toString() {
        return String.format(STRING_FORMAT,
                getName(), getVolume(), getX(), getY(), getZ(),
                getCreatingTime().format(DATE_TIME_FORMATTER),
                lives ? "lives" : "dead", items.isEmpty() ? "without items" :
                        "with items:\n    " + items.parallelStream().map(Object::toString)
                                .collect(Collectors.joining("\n    ")));
    }
}

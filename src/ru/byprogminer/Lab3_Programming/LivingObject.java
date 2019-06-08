package ru.byprogminer.Lab3_Programming;

import ru.byprogminer.Lab4_Programming.DeathException;
import ru.byprogminer.Lab4_Programming.NotFoundException;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;

public class LivingObject extends Object implements Hitter, Picker, Movable, Thinkable, Comparable<LivingObject> {

    private static final String STRING_FORMAT = "LivingObject %s with volume %s at (%s, %s, %s) created %s, currently %s, %s";

    private volatile boolean lives = true;

    private volatile BufferedImage image = null;

    private final Set<Object> items = new HashSet<>();

    public LivingObject(String name) {
        super(name);
    }

    public LivingObject(String name, double volume, LocalDateTime creatingTime) {
        super(name, volume, creatingTime);
    }

    public LivingObject(String name, double volume, LocalDateTime creatingTime, double x, double y, double z) {
        super(name, volume, creatingTime, x, y, z);
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

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
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
        for (Comparator<LivingObject> comparator : arrayOf(
                Comparator.comparing(LivingObject::getName),
                Comparator.comparingDouble(LivingObject::getVolume),
                Comparator.comparing(LivingObject::getCreatingTime),
                Comparator.comparingDouble(LivingObject::getX),
                Comparator.comparingDouble(LivingObject::getY),
                Comparator.comparingDouble(LivingObject::getZ),
                Comparator.comparing(LivingObject::isLives),
                Comparator.comparingDouble((ToDoubleFunction<LivingObject>) (o ->
                        o.items.stream().mapToDouble(Object::getVolume).sum()))
        )) {
            final int result = comparator.compare(this, that);

            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(arrayOf(super.hashCode(), lives, items));
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

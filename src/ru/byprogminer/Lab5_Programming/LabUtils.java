package ru.byprogminer.Lab5_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab3_Programming.Object;
import ru.byprogminer.Lab5_Programming.throwing.Throwing;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class LabUtils {

    interface ObjectConstructor <T extends Object> {

        T construct(String name, Double volume, Date creatingTime);
    }

    private LabUtils() {}

    public static <T extends Object> T mapToObject(Map<String, String> map, ObjectConstructor<T> constructor) {
        BiFunction<String, Throwable, ? extends RuntimeException> exceptionConstructor = IllegalArgumentException::new;
        String cause = "an error occurred while object constructing";

        try {
            cause = "object name is not provided";
            final String name = Objects.requireNonNull(map.get("name"));

            cause = "object volume is not provided or have bad format";
            final Double volume = Double.parseDouble(map.get("volume"));

            cause = "object creating time is not provided or have bad format";
            final Date creatingTime = new Date(Long.parseLong(map.get("creatingTime")));

            cause = "an error occurred while object constructing";
            final T object = constructor.construct(name, volume, creatingTime);

            cause = "object x have bad format";
            callIfNotNull(map.get("x"), s -> object.setX(Double.parseDouble(s)));

            cause = "object y have bad format";
            callIfNotNull(map.get("y"), s -> object.setX(Double.parseDouble(s)));

            cause = "object z have bad format";
            callIfNotNull(map.get("z"), s -> object.setX(Double.parseDouble(s)));

            return object;
        } catch (Throwable e) {
            throw exceptionConstructor.apply(cause, e);
        }
    }

    public static Object objectConstructor(String name, Double volume, Date creatingTime) {
        return new Object(name, volume, creatingTime) {};
    }

    public static LivingObject livingObjectConstructor(String name, Double volume, Date creatingTime) {
        return new LivingObject(name, volume, creatingTime) {};
    }

    public static void setLivingObjectLives(final LivingObject object, final Boolean lives) {
        try {
            final Field livesField = LivingObject.class.getDeclaredField("lives");
            livesField.setAccessible(true);
            livesField.set(object, lives);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            // TODO Logging
        }
    }

    public static <T> void callIf(T value, Predicate<T> cond, Consumer<T> func) {
        if (cond.test(value)) {
            func.accept(value);
        }
    }

    public static <T, R> R applyIf(T value, Predicate<T> cond, Function<T, R> func) {
        if (cond.test(value)) {
            return func.apply(value);
        }

        return null;
    }

    public static <T> void callIfNotNull(T value, Consumer<T> func) {
        callIf(value, Objects::nonNull, func);
    }

    public static <T, R> R applyIfNotNull(T value, Function<T, R> func) {
        return applyIf(value, Objects::nonNull, func);
    }

    public static Throwing throwing() {
        return Throwing.getThrowing();
    }
}

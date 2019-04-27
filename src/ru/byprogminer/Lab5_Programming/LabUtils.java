package ru.byprogminer.Lab5_Programming;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab3_Programming.Object;
import ru.byprogminer.Lab5_Programming.throwing.Throwing;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class LabUtils {

    interface ObjectConstructor <T extends Object> {

        T construct(String name);
    }

    private LabUtils() {}

    public static <T extends Object> T mapToObject(Map<String, String> map, ObjectConstructor<T> constructor) {
        BiFunction<String, Throwable, ? extends RuntimeException> exceptionConstructor = IllegalArgumentException::new;
        String cause = "an error occurred while object constructing";

        try {
            cause = "object's name is not provided";
            final String name = Objects.requireNonNull(map.get("name"));

            cause = "an error occurred while object constructing";
            final T object = constructor.construct(name);

            cause = "object's volume has bad format";
            callIfNotNull(map.get("volume"), s -> object.setVolume(Double.parseDouble(s)));

            cause = "object's creating time has bad format";
            callIfNotNull(map.get("creatingTime"), s -> {
                LocalDateTime creatingTime;

                try {
                    creatingTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(s)), ZoneId.systemDefault());
                } catch (Throwable e) {
                    creatingTime = LocalDateTime.parse(s);
                }

                callIfNotNull(creatingTime, object::setCreatingTime);
            });

            cause = "object's x has bad format";
            callIfNotNull(map.get("x"), s -> object.setX(Double.parseDouble(s)));

            cause = "object's y has bad format";
            callIfNotNull(map.get("y"), s -> object.setY(Double.parseDouble(s)));

            cause = "object's z has bad format";
            callIfNotNull(map.get("z"), s -> object.setZ(Double.parseDouble(s)));

            return object;
        } catch (Throwable e) {
            throw exceptionConstructor.apply(cause, e);
        }
    }

    public static Object objectConstructor(String name) {
        return new Object(name) {};
    }

    public static LivingObject livingObjectConstructor(String name) {
        return new LivingObject(name) {};
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

    @SafeVarargs
    public static <T> T[] arrayOf(T... elements) {
        return elements;
    }

    @SuppressWarnings("unchecked")
    public static LivingObject jsonToLivingObject(String json) {
        String exception = "an error occurred while json reading";

        try {
            JSONObject jsonObject = JSON.parseObject(Objects.requireNonNull(json));

            exception = "an error occurred while living object constructing";
            LivingObject livingObject = mapToObject(jsonObject.entrySet().parallelStream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())),
                    LabUtils::livingObjectConstructor);

            callIf(jsonObject.get("lives"), Boolean.class::isInstance, lives ->
                    setLivingObjectLives(livingObject, (Boolean) lives));

            callIf(jsonObject.get("items"), Collection.class::isInstance, items -> ((Collection<?>) items).parallelStream()
                    .forEach(_item -> callIf(_item, JSONObject.class::isInstance, item -> livingObject.getItems()
                            .add(mapToObject(((Map<String, ?>) item).entrySet().parallelStream()
                                            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())),
                                    LabUtils::objectConstructor)))));

            return livingObject;
        } catch (Throwable e) {
            if (e.getMessage() != null) {
                throw new IllegalArgumentException(exception + ", " + e.getMessage());
            }

            throw new IllegalArgumentException(exception);
        }
    }
}

package ru.byprogminer.Lab5_Programming;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab3_Programming.Object;
import ru.byprogminer.Lab5_Programming.throwing.Throwing;
import ru.byprogminer.Lab5_Programming.throwing.ThrowingFunction;
import ru.byprogminer.Lab5_Programming.throwing.ThrowingRunnable;
import ru.byprogminer.Lab5_Programming.throwing.ThrowingSupplier;
import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LabUtils {

    public interface ObjectConstructor <T extends Object> {

        T construct(String name);
    }

    public static final String PASSWORD_ALPHABET = "" +
            "0123456789abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ`~!@#$%^&*()-+[]{}\\|,.<>/?";

    private static Logger log = Loggers.getClassLogger(LabUtils.class);

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
            throwing().unwrap(Exception.class, () -> callIfNotNull(map.get("creatingTime"), throwing().consumer(s -> {
                LocalDateTime creatingTime = null;
                Exception exception = null;

                try {
                    creatingTime = LocalDateTime.parse(s, Object.DATE_TIME_FORMATTER);
                } catch (Exception e) {
                    exception = e;
                }

                if (creatingTime == null) {
                    try {
                        creatingTime = LocalDateTime.parse(s);
                    } catch (Throwable ignored) {}
                }

                if (creatingTime == null) {
                    try {
                        creatingTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(s)), ZoneId.systemDefault());
                    } catch (Throwable ignored) {}
                }

                callIfNotNull(creatingTime, object::setCreatingTime);
                if (creatingTime == null) {
                    throw exception;
                }
            })));

            cause = "object's x has bad format";
            callIfNotNull(map.get("x"), s -> object.setX(Double.parseDouble(s)));

            cause = "object's y has bad format";
            callIfNotNull(map.get("y"), s -> object.setY(Double.parseDouble(s)));

            cause = "object's z has bad format";
            callIfNotNull(map.get("z"), s -> object.setZ(Double.parseDouble(s)));

            return object;
        } catch (Throwable e) {
            log.log(Level.WARNING, cause, e);
            throw exceptionConstructor.apply(cause, e);
        }
    }

    public static void setLivingObjectLives(final LivingObject object, final Boolean lives) {
        try {
            final Field livesField = LivingObject.class.getDeclaredField("lives");
            livesField.setAccessible(true);
            livesField.set(object, lives);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log.log(Level.SEVERE, "exception thrown while setting living object lives", e);
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

    public static Map<String, String> propertiesToMap(Properties properties) {
        return new HashMap<>(properties).entrySet().stream()
                .map(entry -> new HashMap.SimpleImmutableEntry<>(entry.getKey().toString(), entry.getValue().toString()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <T> Stream<T> mapResultSet(
            ResultSet resultSet,
            ThrowingFunction<ResultSet, T, SQLException> function
    ) throws SQLException {
        if (resultSet == null) {
            return Stream.empty();
        }

        final List<T> ret = new LinkedList<>();
        while (resultSet.next()) {
            final T value = function.throwingApply(resultSet);

            if (value != null) {
                ret.add(value);
            }
        }

        resultSet.close();
        return ret.stream();
    }

    @SafeVarargs
    public static <T> T[] arrayOf(T... elements) {
        return elements;
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    public static int validatePort(String stringPort) throws IllegalArgumentException {
        stringPort = stringPort.trim();

        if (stringPort.isEmpty()) {
            return 0;
        }

        final int port = Integer.parseInt(stringPort);

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port out of range");
        }

        return port;
    }

    public static <T, E extends Exception> ThrowingSupplier<T, E> supplier(ThrowingRunnable<E> code, T value) {
        return () -> {
            code.run();
            return value;
        };
    }

    public static <T, E extends Exception> ThrowingSupplier<T, E> supplier(ThrowingRunnable<E> code) {
        return supplier(code, null);
    }

    public static <T> boolean contains(T[] array, T element) {
        for (T t : array) {
            if (element == t || (element != null && element.equals(t)) || t == null) {
                return true;
            }
        }

        return false;
    }

    public static <T> boolean containsRef(T[] array, T element) {
        for (T t : array) {
            if (element == t) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public static LivingObject jsonToLivingObject(String json) {
        String exception = "an error occurred while json reading";

        try {
            JSONObject jsonObject = JSON.parseObject(Objects.requireNonNull(json));

            exception = "an error occurred while living object constructing";
            LivingObject livingObject = mapToObject(jsonObject.entrySet().parallelStream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e ->
                                    e.getValue().toString())), LivingObject::new);

            callIf(jsonObject.get("lives"), Boolean.class::isInstance, lives ->
                    setLivingObjectLives(livingObject, (Boolean) lives));

            callIf(jsonObject.get("items"), Collection.class::isInstance, items ->
                    ((Collection<?>) items).parallelStream().forEach(_item ->
                            callIf(_item, JSONObject.class::isInstance, item -> livingObject.getItems()
                                    .add(mapToObject(((Map<String, ?>) item).entrySet().parallelStream()
                                                    .collect(Collectors.toMap(Map.Entry::getKey, e ->
                                                            e.getValue().toString())), Object::new)))));

            return livingObject;
        } catch (Throwable e) {
            log.log(Level.WARNING, exception, e);

            if (e.getMessage() != null) {
                throw new IllegalArgumentException(String.format("%s, %s", exception, e.getMessage()), e);
            }

            throw new IllegalArgumentException(exception, e);
        }
    }
}

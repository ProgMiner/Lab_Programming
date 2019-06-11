package ru.byprogminer.Lab8_Programming.gui;

import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GuiDisabler<T> {

    private static final Logger classLog = Loggers.getClassLogger(GuiDisabler.class);

    private final T object;
    private final Map<Field, Boolean> components;

    private final Logger log = Loggers.getObjectLogger(this);

    private GuiDisabler(T object, Map<Field, Boolean> components) {
        this.object = object;
        this.components = components;
    }

    public static <T> GuiDisabler<T> disable(T o) {
        final Map<Field, Boolean> components = new HashMap<>();

        for (Field field : o.getClass().getDeclaredFields()) {
            try {
                if (Component.class.isAssignableFrom(field.getType())) {
                    final boolean oldAccessible = field.isAccessible();
                    field.setAccessible(true);

                    final Component component = (Component) field.get(o);
                    components.put(field, component.isEnabled());
                    component.setEnabled(false);

                    field.setAccessible(oldAccessible);
                }
            } catch (Throwable e) {
                classLog.log(Level.WARNING, "unable to disable component", e);
            }
        }

        return new GuiDisabler<>(o, components);
    }

    public void revert() {
        for (Map.Entry<Field, Boolean> entry : components.entrySet()) {
            try {
                final boolean oldAccessible = entry.getKey().isAccessible();
                entry.getKey().setAccessible(true);

                ((Component) entry.getKey().get(object)).setEnabled(entry.getValue());

                entry.getKey().setAccessible(oldAccessible);
            } catch (Throwable e) {
                log.log(Level.WARNING, "unable to revert component", e);
            }
        }
    }
}

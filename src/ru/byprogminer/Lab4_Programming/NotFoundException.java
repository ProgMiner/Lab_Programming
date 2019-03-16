package ru.byprogminer.Lab4_Programming;

import ru.byprogminer.Lab3_Programming.Object;

import java.util.Formatter;
import java.util.Objects;

public class NotFoundException extends Exception { // Исключения наследуются от класса Exception (или Error/RuntimeException, если unchecked)

    private Object object; // Поле, хранящее объект с которым произошло искючение
                           // Может не быть

    // Конструктор должен принимать объект или строку,
    // Если строку, она должна быть передана в super() в качестве сообщения исключения
    public NotFoundException(Object object) {
        this.object = Objects.requireNonNull(object);
    }

    // Getter для приватного поля, для инкапсуляции
    public Object getObject() {
        return object;
    }

    @Override
    // getMessage необходимо переопределить, либо в конструкторе передавать сформированное сообщение исключения в super()
    public String getMessage() {
        return new Formatter()
                .format("Object %s (%s) not found", object.getName(), object.getClass().getSimpleName())
                .toString();
    }

    @Override
    public String getLocalizedMessage() {
        return new Formatter()
                .format("Объект %s (%s) не найден", object.getName(), object.getClass().getSimpleName())
                .toString();
    }
}

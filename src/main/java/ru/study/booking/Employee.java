package ru.study.booking;

import java.util.Objects;

/**
 * Сотрудник, оформляющий бронь.
 * @param id положительный номер сотрудника
 * @param name имя сотрудника
 */
public record Employee(int id, String name) implements Describable {
    /** Проверяет номер и имя сотрудника. */
    public Employee {
        if (id <= 0 || Objects.requireNonNull(name).isBlank()) {
            throw new IllegalArgumentException("Нужны положительный номер и непустое имя");
        }
    }

    /** @return имя и номер сотрудника */
    @Override
    public String describe() {
        return name + ", сотрудник №" + id;
    }
}


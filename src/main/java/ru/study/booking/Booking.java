package ru.study.booking;

import java.util.Objects;

/**
 * Неизменяемая бронь.
 * @param id положительный номер брони
 * @param room помещение
 * @param employee сотрудник
 * @param interval время бронирования
 */
public record Booking(int id, Room room, Employee employee, TimeInterval interval) {
    /** Проверяет номер брони и обязательные значения. */
    public Booking {
        if (id <= 0) {
            throw new IllegalArgumentException("Номер брони должен быть положительным");
        }
        Objects.requireNonNull(room);
        Objects.requireNonNull(employee);
        Objects.requireNonNull(interval);
    }
}


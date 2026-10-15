package ru.study.booking;

/** Возникает при попытке забронировать занятое помещение. */
public class BookingConflictException extends Exception {
    /**
     * Создаёт исключение с объяснением конфликта.
     * @param message описание занятого помещения и времени
     */
    public BookingConflictException(String message) {
        super(message);
    }
}


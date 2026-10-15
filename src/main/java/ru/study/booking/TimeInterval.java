package ru.study.booking;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Интервал [начало, конец): момент конца не входит в интервал.
 * @param start начало
 * @param end конец, строго позже начала
 */
public record TimeInterval(LocalDateTime start, LocalDateTime end) {
    /** Проверяет, что обе даты заданы и конец строго позже начала. */
    public TimeInterval {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Конец интервала должен быть позже начала");
        }
    }

    /**
     * Проверяет пересечение. Соседние интервалы не пересекаются.
     * @param other другой интервал
     * @return true, если есть общее время
     */
    public boolean overlaps(TimeInterval other) {
        Objects.requireNonNull(other);
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}


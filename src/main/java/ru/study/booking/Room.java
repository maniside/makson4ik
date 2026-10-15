package ru.study.booking;

import java.util.Objects;
import java.util.Set;

/**
 * Неизменяемое помещение. Оснащение копируется при создании.
 * @param id уникальный положительный номер
 * @param name название
 * @param capacity вместимость
 * @param type тип помещения
 * @param equipment набор оборудования
 */
public record Room(int id, String name, int capacity, RoomType type,
                   Set<String> equipment) implements Describable {
    /** Проверяет номер, название и вместимость; сохраняет копию оснащения. */
    public Room {
        if (id <= 0 || capacity <= 0) {
            throw new IllegalArgumentException("Номер и вместимость должны быть положительными");
        }
        if (Objects.requireNonNull(name).isBlank()) {
            throw new IllegalArgumentException("Название помещения не должно быть пустым");
        }
        Objects.requireNonNull(type);
        equipment = Set.copyOf(equipment);
    }

    /** @return название, тип, вместимость и оснащение помещения */
    @Override
    public String describe() {
        String typeName = switch (type) {
            case MEETING_ROOM -> "переговорная";
            case CLASSROOM -> "аудитория";
            case CONFERENCE_HALL -> "конференц-зал";
        };
        return name + " (" + typeName + "), мест: " + capacity + ", оснащение: " + equipment;
    }
}


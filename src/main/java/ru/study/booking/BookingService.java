package ru.study.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Управляет помещениями и бронями. Все данные хранятся только в памяти. */
public class BookingService {
    private final List<Room> rooms = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private int nextBookingId = 1;

    /** Создаёт пустую систему бронирования. */
    public BookingService() {
    }

    /**
     * Добавляет помещение с уникальным номером.
     * @param room помещение
     * @throws IllegalArgumentException если номер уже зарегистрирован
     */
    public void addRoom(Room room) {
        Objects.requireNonNull(room);
        for (Room existing : rooms) {
            if (existing.id() == room.id()) {
                throw new IllegalArgumentException("Помещение с таким номером уже есть");
            }
        }
        rooms.add(room);
    }

    /** @return неизменяемая копия списка помещений */
    public List<Room> getRooms() {
        return List.copyOf(rooms);
    }

    /** @return неизменяемая копия списка действующих броней */
    public List<Booking> getBookings() {
        return List.copyOf(bookings);
    }

    /**
     * Бронирует зарегистрированное помещение на заданное время.
     * @param roomId номер помещения
     * @param employee сотрудник
     * @param interval интервал
     * @return созданная бронь
     * @throws BookingConflictException если помещение занято
     * @throws IllegalArgumentException если помещение не найдено
     */
    public Booking book(int roomId, Employee employee, TimeInterval interval)
            throws BookingConflictException {
        Room room = findRoom(roomId);
        Objects.requireNonNull(employee);
        Objects.requireNonNull(interval);
        if (!isAvailable(roomId, interval)) {
            throw new BookingConflictException(room.name() + ": время " + interval + " уже занято");
        }
        Booking booking = new Booking(nextBookingId++, room, employee, interval);
        bookings.add(booking);
        return booking;
    }

    /**
     * Отменяет бронь по номеру.
     * @param bookingId номер брони
     * @return true, если бронь найдена и отменена; false, если её нет
     */
    public boolean cancel(int bookingId) {
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).id() == bookingId) {
                bookings.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Проверяет доступность зарегистрированного помещения.
     * @param roomId номер помещения
     * @param interval нужное время
     * @return true, если нет пересекающихся броней
     * @throws IllegalArgumentException если помещение не найдено
     */
    public boolean isAvailable(int roomId, TimeInterval interval) {
        findRoom(roomId);
        Objects.requireNonNull(interval);
        for (Booking booking : bookings) {
            if (booking.room().id() == roomId && booking.interval().overlaps(interval)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Подбирает свободные помещения с достаточной вместимостью.
     * @param capacity требуемое число мест, больше нуля
     * @param interval нужное время
     * @return неизменяемый список подходящих помещений
     */
    public List<Room> findRooms(int capacity, TimeInterval interval) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Количество мест должно быть положительным");
        }
        Objects.requireNonNull(interval);
        List<Room> result = new ArrayList<>();
        for (Room room : rooms) {
            if (room.capacity() >= capacity && isAvailable(room.id(), interval)) {
                result.add(room);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Находит свободные окна в пределах суток от 00:00 до 00:00 следующего дня.
     * Брони, выходящие за пределы дня, учитываются только внутри этих суток.
     * @param roomId номер помещения
     * @param day день
     * @return неизменяемый список свободных интервалов по времени начала
     * @throws IllegalArgumentException если помещение не найдено
     */
    public List<TimeInterval> freeWindows(int roomId, LocalDate day) {
        findRoom(roomId);
        Objects.requireNonNull(day);
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
        TimeInterval wholeDay = new TimeInterval(dayStart, dayEnd);
        List<TimeInterval> occupied = new ArrayList<>();

        for (Booking booking : bookings) {
            if (booking.room().id() == roomId && booking.interval().overlaps(wholeDay)) {
                occupied.add(booking.interval());
            }
        }
        occupied.sort(Comparator.comparing(TimeInterval::start));

        List<TimeInterval> result = new ArrayList<>();
        LocalDateTime cursor = dayStart;
        for (TimeInterval interval : occupied) {
            LocalDateTime start = interval.start().isBefore(dayStart) ? dayStart : interval.start();
            LocalDateTime end = interval.end().isAfter(dayEnd) ? dayEnd : interval.end();
            if (cursor.isBefore(start)) {
                result.add(new TimeInterval(cursor, start));
            }
            if (end.isAfter(cursor)) {
                cursor = end;
            }
        }
        if (cursor.isBefore(dayEnd)) {
            result.add(new TimeInterval(cursor, dayEnd));
        }
        return List.copyOf(result);
    }

    private Room findRoom(int roomId) {
        for (Room room : rooms) {
            if (room.id() == roomId) {
                return room;
            }
        }
        throw new IllegalArgumentException("Помещение №" + roomId + " не найдено");
    }
}


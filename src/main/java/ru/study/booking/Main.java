package ru.study.booking;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Консольная демонстрация всех операций базового уровня. */
public class Main {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 15);
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private Main() {
    }

    /**
     * Запускает демонстрацию.
     * @param args аргументы не используются
     */
    public static void main(String[] args) {
        BookingService service = new BookingService();
        List<Employee> employees = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            RoomType type = switch (i % 3) {
                case 0 -> RoomType.CONFERENCE_HALL;
                case 1 -> RoomType.MEETING_ROOM;
                default -> RoomType.CLASSROOM;
            };
            service.addRoom(new Room(i, "Помещение " + i, i * 5,
                    type, Set.of("Проектор", "Доска")));
            employees.add(new Employee(i, "Сотрудник " + i));
        }

        System.out.println("1. Помещения и сотрудники (общий интерфейс Describable)");
        List<Describable> descriptions = new ArrayList<>();
        descriptions.addAll(service.getRooms());
        descriptions.addAll(employees);
        for (Describable item : descriptions) {
            System.out.println(item.describe());
        }

        System.out.println("\n2. Создание броней");
        try {
            for (int i = 1; i <= 10; i++) {
                service.book(i, employees.get(i - 1), interval(9, 11));
                service.book(i, employees.get(i - 1), interval(13, 14));
            }
        } catch (BookingConflictException e) {
            System.out.println("Не удалось заполнить демонстрационные брони: " + e.getMessage());
            return;
        }
        System.out.println("Создано: 10 помещений, 10 сотрудников, 20 броней (40 объектов модели).");
        for (Booking booking : service.getBookings()) {
            System.out.println("Бронь №" + booking.id() + ": " + booking.room().name()
                    + ", " + booking.employee().name() + ", " + format(booking.interval()));
        }

        System.out.println("\n3. Попытка забронировать занятое время");
        try {
            service.book(1, employees.get(1), interval(10, 12));
        } catch (BookingConflictException e) {
            System.out.println("Бронь отклонена: " + e.getMessage());
            System.out.println("Выберите другое время или одно из свободных помещений.");
        }

        System.out.println("\n4. Свободные окна помещения 1 на " + DAY);
        printWindows(service, 1);

        System.out.println("\n5. Подбор помещения минимум на 20 человек с 11:00 до 13:00");
        for (Room room : service.findRooms(20, interval(11, 13))) {
            System.out.println(room.describe());
        }
        System.out.println("Соседние брони в 9:00-11:00 и 13:00-14:00 не мешают этому интервалу.");

        System.out.println("\n6. Отмена брони №1");
        System.out.println(service.cancel(1) ? "Бронь отменена" : "Бронь не найдена");
        System.out.println("Свободные окна после отмены:");
        printWindows(service, 1);

        System.out.println("\n7. Бронирование освободившегося времени");
        try {
            Booking booking = service.book(1, employees.get(1), interval(9, 11));
            System.out.println("Создана бронь №" + booking.id() + ": " + format(booking.interval()));
        } catch (BookingConflictException e) {
            System.out.println("Повторная бронь отклонена: " + e.getMessage());
        }
    }

    private static TimeInterval interval(int startHour, int endHour) {
        return new TimeInterval(DAY.atTime(startHour, 0), DAY.atTime(endHour, 0));
    }

    private static String format(TimeInterval interval) {
        return interval.start().format(FORMAT) + " - " + interval.end().format(FORMAT);
    }

    private static void printWindows(BookingService service, int roomId) {
        for (TimeInterval window : service.freeWindows(roomId, DAY)) {
            System.out.println(format(window));
        }
    }
}


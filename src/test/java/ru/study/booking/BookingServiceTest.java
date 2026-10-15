package ru.study.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class BookingServiceTest {
    private final LocalDate day = LocalDate.of(2026, 10, 15);
    private final Employee employee = new Employee(1, "Иван");
    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService();
        service.addRoom(new Room(1, "Комната 1", 10, RoomType.MEETING_ROOM, Set.of("Доска")));
        service.addRoom(new Room(2, "Комната 2", 20, RoomType.CLASSROOM, Set.of("Проектор")));
    }

    private TimeInterval interval(int start, int end) {
        return new TimeInterval(day.atTime(start, 0), day.atTime(end, 0));
    }

    @Test
    void acceptsValidInterval() {
        assertEquals(day.atTime(10, 0), interval(10, 12).start());
    }

    @Test
    void rejectsEmptyInterval() {
        assertThrows(IllegalArgumentException.class, () -> interval(10, 10));
    }

    @Test
    void rejectsReversedInterval() {
        assertThrows(IllegalArgumentException.class, () -> interval(12, 10));
    }

    @Test
    void rejectsNullIntervalBoundary() {
        assertThrows(NullPointerException.class,
                () -> new TimeInterval(null, day.atTime(12, 0)));
    }

    @Test
    void detectsPartialOverlapBothWays() {
        assertTrue(interval(9, 11).overlaps(interval(10, 12)));
        assertTrue(interval(10, 12).overlaps(interval(9, 11)));
    }

    @Test
    void detectsContainedInterval() {
        assertTrue(interval(9, 15).overlaps(interval(10, 11)));
    }

    @Test
    void adjacentIntervalsDoNotOverlap() {
        assertFalse(interval(9, 11).overlaps(interval(11, 13)));
        assertFalse(interval(11, 13).overlaps(interval(9, 11)));
    }

    @Test
    void createsBooking() throws BookingConflictException {
        Booking booking = service.book(1, employee, interval(9, 11));
        assertEquals(1, booking.room().id());
        assertEquals(employee, booking.employee());
        assertEquals(List.of(booking), service.getBookings());
    }

    @Test
    void rejectsConflictWithoutAddingBooking() throws BookingConflictException {
        service.book(1, employee, interval(9, 11));
        assertThrows(BookingConflictException.class,
                () -> service.book(1, employee, interval(10, 12)));
        assertEquals(1, service.getBookings().size());
    }

    @Test
    void allowsAdjacentBookings() throws BookingConflictException {
        service.book(1, employee, interval(9, 11));
        service.book(1, employee, interval(11, 13));
        assertEquals(2, service.getBookings().size());
    }

    @Test
    void allowsSameTimeInDifferentRooms() throws BookingConflictException {
        service.book(1, employee, interval(9, 11));
        service.book(2, employee, interval(9, 11));
        assertEquals(2, service.getBookings().size());
    }

    @Test
    void cancellationFreesTime() throws BookingConflictException {
        Booking booking = service.book(1, employee, interval(9, 11));
        assertTrue(service.cancel(booking.id()));
        assertTrue(service.isAvailable(1, interval(9, 11)));
        assertDoesNotThrow(() -> service.book(1, employee, interval(9, 11)));
    }

    @Test
    void cancellingMissingBookingReturnsFalse() {
        assertFalse(service.cancel(100));
    }

    @Test
    void unknownRoomIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.book(99, employee, interval(9, 11)));
        assertThrows(IllegalArgumentException.class, () -> service.freeWindows(99, day));
    }

    @Test
    void duplicateRoomIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addRoom(new Room(1, "Другая", 40,
                        RoomType.CONFERENCE_HALL, Set.of())));
    }

    @Test
    void emptyDayIsCompletelyFree() {
        assertEquals(List.of(new TimeInterval(day.atStartOfDay(), day.plusDays(1).atStartOfDay())),
                service.freeWindows(1, day));
    }

    @Test
    void freeWindowsAreSortedAndCoverGaps() throws BookingConflictException {
        service.book(1, employee, interval(13, 14));
        service.book(1, employee, interval(9, 11));
        assertEquals(List.of(
                new TimeInterval(day.atStartOfDay(), day.atTime(9, 0)),
                interval(11, 13),
                new TimeInterval(day.atTime(14, 0), day.plusDays(1).atStartOfDay())),
                service.freeWindows(1, day));
    }

    @Test
    void fullDayBookingLeavesNoWindows() throws BookingConflictException {
        service.book(1, employee,
                new TimeInterval(day.atStartOfDay(), day.plusDays(1).atStartOfDay()));
        assertTrue(service.freeWindows(1, day).isEmpty());
    }

    @Test
    void clipsBookingsCrossingDayBoundaries() throws BookingConflictException {
        service.book(1, employee,
                new TimeInterval(day.minusDays(1).atTime(23, 0), day.atTime(2, 0)));
        service.book(1, employee,
                new TimeInterval(day.atTime(22, 0), day.plusDays(1).atTime(3, 0)));
        assertEquals(List.of(interval(2, 22)), service.freeWindows(1, day));
    }

    @Test
    void bookingOnAnotherDayDoesNotOccupyToday() throws BookingConflictException {
        service.book(1, employee, new TimeInterval(day.plusDays(1).atStartOfDay(),
                day.plusDays(1).atTime(2, 0)));
        assertEquals(1, service.freeWindows(1, day).size());
        assertEquals(day.plusDays(1).atStartOfDay(), service.freeWindows(1, day).get(0).end());
    }

    @Test
    void searchIncludesExactCapacityAndExcludesBusyRoom() throws BookingConflictException {
        assertEquals(1, service.findRooms(20, interval(9, 11)).size());
        service.book(2, employee, interval(9, 11));
        assertTrue(service.findRooms(20, interval(9, 11)).isEmpty());
        assertEquals(2, service.findRooms(10, interval(11, 12)).size());
    }

    @Test
    void rejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findRooms(0, interval(9, 11)));
        assertThrows(IllegalArgumentException.class,
                () -> new Room(3, "Третья", -1, RoomType.CLASSROOM, Set.of()));
    }

    @Test
    void equipmentIsDefensivelyCopied() {
        Set<String> equipment = new HashSet<>(Set.of("Доска"));
        Room room = new Room(3, "Третья", 10, RoomType.CLASSROOM, equipment);
        equipment.add("Проектор");
        assertEquals(Set.of("Доска"), room.equipment());
        assertThrows(UnsupportedOperationException.class, () -> room.equipment().add("Экран"));
    }

    @Test
    void returnedListsCannotChangeService() throws BookingConflictException {
        service.book(1, employee, interval(9, 11));
        assertThrows(UnsupportedOperationException.class, () -> service.getRooms().clear());
        assertThrows(UnsupportedOperationException.class, () -> service.getBookings().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> service.freeWindows(1, day).clear());
        assertThrows(UnsupportedOperationException.class,
                () -> service.findRooms(10, interval(11, 12)).clear());
        List<Booking> snapshot = service.getBookings();
        service.book(2, employee, interval(9, 11));
        assertEquals(1, snapshot.size());
    }

    @Test
    void employeeEqualsAndHashCodeContract() {
        Employee a = new Employee(1, "Иван");
        Employee b = new Employee(1, "Иван");
        Employee c = new Employee(1, "Иван");
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(b, c);
        assertEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, null);
        assertNotEquals(a, new Employee(2, "Иван"));
        assertNotEquals(a, new Employee(1, "Анна"));
        Set<Employee> unique = new HashSet<>(List.of(a, b, c));
        assertEquals(1, unique.size());
    }

    @Test
    void polymorphicDescriptionsAreDifferent() {
        List<Describable> objects = new ArrayList<>();
        objects.add(employee);
        objects.add(service.getRooms().get(0));
        assertTrue(objects.get(0).describe().contains("Иван"));
        assertTrue(objects.get(1).describe().contains("мест: 10"));
    }
}


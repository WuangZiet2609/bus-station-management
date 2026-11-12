package com.busstation.controller;

import com.busstation.dto.response.PageResponse;
import com.busstation.entity.Booking;
import com.busstation.entity.BookingStatus;
import com.busstation.entity.PaymentStatus;
import com.busstation.entity.Trip;
import com.busstation.repository.BookingRepository;
import com.busstation.repository.TripRepository;
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.List
import java.util.Map
import java.util.UUID
import java.util.stream.Collectors

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class ReportController {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;

    @GetMapping("/revenue/daily")
    public ResponseEntity<Map<String, Object>> getDailyRevenue(
            @RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<Booking> bookings = bookingRepository.findByStatus(BookingStatus.PAID);
        List<Booking> dailyBookings = bookings.stream()
                .filter(b -> b.getPaidAt() != null && b.getPaidAt().isAfter(start) && b.getPaidAt().isBefore(end))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = dailyBookings.stream()
                .map(Booking::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
                "date", targetDate,
                "totalBookings", dailyBookings.size(),
                "totalRevenue", totalRevenue,
                "bookings", dailyBookings.stream().map(this::bookingSummary).collect(Collectors.toList())
        ));
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyRevenue(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        LocalDateTime start = LocalDate.of(targetYear, targetMonth, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        List<Booking> bookings = bookingRepository.findByStatus(BookingStatus.PAID);
        List<Booking> monthlyBookings = bookings.stream()
                .filter(b -> b.getPaidAt() != null && b.getPaidAt().isAfter(start) && b.getPaidAt().isBefore(end))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = monthlyBookings.stream()
                .map(Booking::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
                "year", targetYear,
                "month", targetMonth,
                "totalBookings", monthlyBookings.size(),
                "totalRevenue", totalRevenue
        ));
    }

    @GetMapping("/trips/occupancy")
    public ResponseEntity<List<Map<String, Object>>> getTripOccupancy(
            @RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<TripStatus> statuses = List.of(TripStatus.SCHEDULED, TripStatus.BOARDING, TripStatus.IN_PROGRESS, TripStatus.COMPLETED);
        List<Trip> trips = tripRepository.findByDepartureTimeBetweenAndStatuses(start, end, statuses);

        return ResponseEntity.ok(trips.stream().map(trip -> Map.of(
                "tripId", trip.getId(),
                "tripCode", trip.getTripCode(),
                "route", trip.getRoute().getName(),
                "departureTime", trip.getDepartureTime(),
                "totalSeats", trip.getTotalSeats(),
                "availableSeats", trip.getAvailableSeats(),
                "bookedSeats", trip.getTotalSeats() - trip.getAvailableSeats(),
                "occupancyRate", trip.getTotalSeats() > 0 ?
                        (double)(trip.getTotalSeats() - trip.getAvailableSeats()) / trip.getTotalSeats() * 100 : 0
        )).collect(Collectors.toList()));
    }

    @GetMapping("/vehicles/utilization")
    public ResponseEntity<List<Map<String, Object>>> getVehicleUtilization(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : LocalDate.now();

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<TripStatus> statuses = List.of(TripStatus.COMPLETED, TripStatus.IN_PROGRESS);
        List<Trip> trips = tripRepository.findByDepartureTimeBetweenAndStatuses(start, end, statuses);

        return ResponseEntity.ok(trips.stream().collect(Collectors.groupingBy(
                Trip::getVehicle,
                Collectors.collectingAndThen(Collectors.toList(), vehicleTrips -> {
                    Vehicle vehicle = vehicleTrips.get(0).getVehicle();
                    int totalTrips = vehicleTrips.size();
                    int totalSeats = vehicleTrips.stream().mapToInt(Trip::getTotalSeats).sum();
                    int totalBooked = vehicleTrips.stream().mapToInt(t -> t.getTotalSeats() - t.getAvailableSeats()).sum();
                    return Map.of(
                            "vehicleId", vehicle.getId(),
                            "licensePlate", vehicle.getLicensePlate(),
                            "totalTrips", totalTrips,
                            "totalSeats", totalSeats,
                            "totalBooked", totalBooked,
                            "utilizationRate", totalSeats > 0 ? (double) totalBooked / totalSeats * 100 : 0
                    );
                })
        )).values().stream().collect(Collectors.toList()));
    }

    private Map<String, Object> bookingSummary(Booking booking) {
        return Map.of(
                "bookingCode", booking.getBookingCode(),
                "tripCode", booking.getTrip().getTripCode(),
                "passengerName", booking.getPassengerName(),
                "amount", booking.getFinalAmount(),
                "paidAt", booking.getPaidAt()
        );
    }
}
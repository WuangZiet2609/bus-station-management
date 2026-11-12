package com.busstation.schedule;

import com.busstation.entity.Booking;
import com.busstation.entity.BookingStatus;
import com.busstation.entity.NotificationChannel;
import com.busstation.entity.NotificationType;
import com.busstation.entity.Trip;
import com.busstation.entity.TripStatus;
import com.busstation.repository.BookingRepository;
import com.busstation.repository.TripRepository;
import com.busstation.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void checkUpcomingTrips() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(30);

        List<Trip> upcomingTrips = tripRepository.findUpcomingTrips(now, reminderTime);

        for (Trip trip : upcomingTrips) {
            if (trip.getStatus() == TripStatus.SCHEDULED) {
                trip.setStatus(TripStatus.BOARDING);
                tripRepository.save(trip);
            }

            List<Booking> bookings = bookingRepository.findByTripIdAndStatuses(
                    trip.getId(),
                    List.of(BookingStatus.CONFIRMED, BookingStatus.PAID)
            );

            for (Booking booking : bookings) {
                notificationService.createNotification(
                        booking.getUser().getId(),
                        NotificationType.BOOKING_REMINDER,
                        "Nhắc nhớ chuyến xe",
                        "Chuyến xe " + trip.getTripCode() + " sẽ khởi hành vào " + trip.getDepartureTime(),
                        NotificationChannel.IN_APP,
                        booking.getId(), "BOOKING"
                );

                if (booking.getPassengerEmail() != null) {
                    notificationService.createNotification(
                            booking.getUser().getId(),
                            NotificationType.BOOKING_REMINDER,
                            "Nhắc nhớ chuyến xe",
                            "Chuyến xe " + trip.getTripCode() + " sẽ khởi hành vào " + trip.getDepartureTime(),
                            NotificationChannel.EMAIL,
                            booking.getId(), "BOOKING"
                    );
                }
            }
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void expirePendingBookings() {
        List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(LocalDateTime.now());

        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setPaymentStatus(booking.getPaymentStatus() == null ?
                    com.busstation.entity.PaymentStatus.CANCELLED : booking.getPaymentStatus());
            bookingRepository.save(booking);

            notificationService.createNotification(
                    booking.getUser().getId(),
                    NotificationType.BOOKING_CONFIRMATION,
                    "Đặt vé hết hạn",
                    "Đơn đặt vé " + booking.getBookingCode() + " đã hết hạn do không thanh toán kịp thời",
                    NotificationChannel.IN_APP,
                    booking.getId(), "BOOKING"
            );
        }

        if (!expiredBookings.isEmpty()) {
            log.info("Expired {} pending bookings", expiredBookings.size());
        }
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupOldData() {
        // Clean up old expired bookings, notifications, etc.
        log.info("Running daily cleanup task");
    }
}
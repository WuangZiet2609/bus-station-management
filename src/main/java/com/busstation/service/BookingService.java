package com.busstation.service;

import com.busstation.dto.request.BookingCreateRequest;
import com.busstation.dto.request.BookingUpdateRequest;
import com.busstation.dto.response.BookingResponse;
import com.busstation.dto.response.PageResponse;
import com.busstation.entity.Booking;
import com.busstation.entity.BookingStatus;
import com.busstation.entity.PaymentMethod;
import com.busstation.entity.PaymentStatus;
import com.busstation.entity.Seat;
import com.busstation.entity.SeatStatus;
import com.busstation.entity.Trip;
import com.busstation.entity.User;
import com.busstation.exception.BusinessException;
import com.busstation.repository.BookingRepository;
import com.busstation.repository.SeatRepository;
import com.busstation.repository.TripRepository;
import com.busstation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public BookingResponse createBooking(UUID userId, BookingCreateRequest request) {
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));

        if (trip.getStatus() != com.busstation.entity.TripStatus.SCHEDULED &&
            trip.getStatus() != com.busstation.entity.TripStatus.BOARDING) {
            throw BusinessException.badRequest("Chuyến xe không ở trạng thái có thể đặt vé");
        }

        if (trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw BusinessException.badRequest("Chuyến xe đã khởi hành");
        }

        List<Seat> seats = seatRepository.findByTripIdAndSeatNumberIn(
                request.getTripId(),
                request.getSeatIds().stream().map(UUID::toString).collect(Collectors.toList())
        );

        if (seats.size() != request.getSeatIds().size()) {
            throw BusinessException.notFound("Một số ghế không tồn tại");
        }

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw BusinessException.conflict("Ghế " + seat.getSeatNumber() + " đã được đặt");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));

        BigDecimal totalAmount = calculateTotalAmount(trip, seats);
        BigDecimal finalAmount = totalAmount;

        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode())
                .user(user)
                .trip(trip)
                .seatIds(request.getSeatIds())
                .passengerName(request.getPassengerName())
                .passengerPhone(request.getPassengerPhone())
                .passengerEmail(request.getPassengerEmail())
                .passengerIdentityCard(request.getPassengerIdentityCard())
                .totalAmount(totalAmount)
                .finalAmount(finalAmount)
                .status(BookingStatus.PENDING)
                .paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .notes(request.getNotes())
                .build();

        booking = bookingRepository.save(booking);

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
        }
        seatRepository.saveAll(seats);

        trip.setAvailableSeats(trip.getAvailableSeats() - seats.size());
        tripRepository.save(trip);

        log.info("Booking created: {}", booking.getBookingCode());

        notificationService.createNotification(userId,
                com.busstation.entity.NotificationType.BOOKING_CONFIRMATION,
                "Xác nhận đặt vé",
                "Bạn đã đặt vé thành công cho chuyến " + trip.getTripCode(),
                com.busstation.entity.NotificationChannel.IN_APP,
                booking.getId(), "BOOKING");

        return buildBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getUserBookings(UUID userId, int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        List<BookingStatus> statuses = status != null ?
                List.of(BookingStatus.valueOf(status.toUpperCase())) :
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.PAID, BookingStatus.CANCELLED, BookingStatus.REFUNDED);

        Page<Booking> bookingPage = bookingRepository.findByUserIdAndStatuses(userId, statuses, pageable);

        return PageResponse.<BookingResponse>builder()
                .content(bookingPage.map(this::buildBookingResponse).getContent())
                .page(bookingPage.getNumber())
                .size(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .first(bookingPage.isFirst())
                .last(bookingPage.isLast())
                .empty(bookingPage.isEmpty())
                .build();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn đặt vé"));
        return buildBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn đặt vé"));
        return buildBookingResponse(booking);
    }

    @Transactional
    public BookingResponse updateBooking(UUID bookingId, BookingUpdateRequest request, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn đặt vé"));

        if (!booking.getUser().getId().equals(userId) &&
            !hasRole(userId, com.busstation.entity.UserRole.ADMIN) &&
            !hasRole(userId, com.busstation.entity.UserRole.EMPLOYEE)) {
            throw BusinessException.forbidden("Không có quyền cập nhật đơn đặt vé này");
        }

        if (request.getStatus() != null) {
            BookingStatus newStatus = BookingStatus.valueOf(request.getStatus().toUpperCase());
            booking.setStatus(newStatus);

            if (newStatus == BookingStatus.CANCELLED) {
                booking.setCancelledAt(LocalDateTime.now());
                booking.setCancelReason(request.getCancelReason());
                releaseSeats(booking);
            }
        }

        booking = bookingRepository.save(booking);
        return buildBookingResponse(booking);
    }

    @Transactional
    public void cancelBooking(UUID bookingId, UUID userId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn đặt vé"));

        if (!booking.getUser().getId().equals(userId) &&
            !hasRole(userId, com.busstation.entity.UserRole.ADMIN) &&
            !hasRole(userId, com.busstation.entity.UserRole.EMPLOYEE)) {
            throw BusinessException.forbidden("Không có quyền hủy đơn đặt vé này");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REFUNDED) {
            throw BusinessException.badRequest("Đơn đặt vé đã bị hủy hoặc hoàn tiền");
        }

        if (booking.getTrip().getDepartureTime().isBefore(LocalDateTime.now().plusHours(1))) {
            throw BusinessException.badRequest("Không thể hủy vé trước giờ khởi hành dưới 1 giờ");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelReason(reason);

        releaseSeats(booking);

        bookingRepository.save(booking);

        notificationService.createNotification(userId,
                com.busstation.entity.NotificationType.BOOKING_CONFIRMATION,
                "Vé đã bị hủy",
                "Vé chuyến " + booking.getTrip().getTripCode() + " đã bị hủy: " + reason,
                com.busstation.entity.NotificationChannel.IN_APP,
                booking.getId(), "BOOKING");
    }

    @Transactional
    public void confirmPayment(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn đặt vé"));

        booking.setStatus(BookingStatus.PAID);
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        booking.setPaidAt(LocalDateTime.now());

        bookingRepository.save(booking);

        notificationService.createNotification(booking.getUser().getId(),
                com.busstation.entity.NotificationType.PAYMENT_SUCCESS,
                "Thanh toán thành công",
                "Thanh toán vé " + booking.getBookingCode() + " thành công",
                com.busstation.entity.NotificationChannel.IN_APP,
                booking.getId(), "BOOKING");
    }

    @Transactional
    public void expirePendingBookings() {
        List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(LocalDateTime.now());
        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setPaymentStatus(PaymentStatus.CANCELLED);
            releaseSeats(booking);
            bookingRepository.save(booking);
        }
        log.info("Expired {} pending bookings", expiredBookings.size());
    }

    private void releaseSeats(Booking booking) {
        List<Seat> seats = seatRepository.findByTripIdAndSeatNumberIn(
                booking.getTrip().getId(),
                booking.getSeatIds().stream().map(UUID::toString).collect(Collectors.toList())
        );

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
        }
        seatRepository.saveAll(seats);

        Trip trip = booking.getTrip();
        trip.setAvailableSeats(trip.getAvailableSeats() + seats.size());
        tripRepository.save(trip);
    }

    private BigDecimal calculateTotalAmount(Trip trip, List<Seat> seats) {
        return seats.stream()
                .map(seat -> trip.getBasePrice().multiply(BigDecimal.valueOf(seat.getPriceMultiplier())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateBookingCode() {
        return "BK" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private boolean hasRole(UUID userId, com.busstation.entity.UserRole role) {
        return userRepository.findById(userId).map(u -> u.getRole() == role).orElse(false);
    }

    private BookingResponse buildBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .user(buildUserSummary(booking.getUser()))
                .trip(buildTripSummary(booking.getTrip()))
                .seats(booking.getSeatIds().stream()
                        .map(id -> seatRepository.findById(id).map(this::buildSeatResponse).orElse(null))
                        .filter(s -> s != null)
                        .collect(Collectors.toList()))
                .passengerName(booking.getPassengerName())
                .passengerPhone(booking.getPassengerPhone())
                .passengerEmail(booking.getPassengerEmail())
                .passengerIdentityCard(booking.getPassengerIdentityCard())
                .totalAmount(booking.getTotalAmount())
                .discountAmount(booking.getDiscountAmount())
                .finalAmount(booking.getFinalAmount())
                .status(booking.getStatus().name())
                .paymentMethod(booking.getPaymentMethod().name())
                .paymentStatus(booking.getPaymentStatus().name())
                .paymentTransactionId(booking.getPaymentTransactionId())
                .paidAt(booking.getPaidAt())
                .expiresAt(booking.getExpiresAt())
                .cancelledAt(booking.getCancelledAt())
                .cancelReason(booking.getCancelReason())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    private com.busstation.dto.response.UserSummary buildUserSummary(User user) {
        return com.busstation.dto.response.UserSummary.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private com.busstation.dto.response.TripSummary buildTripSummary(Trip trip) {
        return com.busstation.dto.response.TripSummary.builder()
                .id(trip.getId())
                .tripCode(trip.getTripCode())
                .route(buildRouteSummary(trip.getRoute()))
                .vehicle(buildVehicleSummary(trip.getVehicle()))
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .status(trip.getStatus().name())
                .build();
    }

    private com.busstation.dto.response.RouteSummary buildRouteSummary(com.busstation.entity.Route route) {
        if (route == null) return null;
        return com.busstation.dto.response.RouteSummary.builder()
                .id(route.getId())
                .name(route.getName())
                .code(route.getCode())
                .departureProvince(route.getDepartureProvince())
                .arrivalProvince(route.getArrivalProvince())
                .distanceKm(route.getDistanceKm())
                .estimatedDurationMinutes(route.getEstimatedDurationMinutes())
                .build();
    }

    private com.busstation.dto.response.VehicleSummary buildVehicleSummary(com.busstation.entity.Vehicle vehicle) {
        if (vehicle == null) return null;
        return com.busstation.dto.response.VehicleSummary.builder()
                .id(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .name(vehicle.getName())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .seatCount(vehicle.getSeatCount())
                .vehicleType(vehicle.getVehicleType())
                .status(vehicle.getStatus().name())
                .build();
    }

    private com.busstation.dto.response.SeatResponse buildSeatResponse(Seat seat) {
        return com.busstation.dto.response.SeatResponse.builder()
                .id(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .seatRow(seat.getSeatRow())
                .seatCol(seat.getSeatCol())
                .floor(seat.getFloor())
                .seatType(seat.getSeatType())
                .priceMultiplier(seat.getPriceMultiplier())
                .status(seat.getStatus().name())
                .build();
    }
}
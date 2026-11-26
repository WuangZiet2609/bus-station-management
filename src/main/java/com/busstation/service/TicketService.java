package com.busstation.service;

import com.busstation.dto.request.TicketCreateRequest;
import com.busstation.dto.response.TicketResponse;
import com.busstation.entity.Ticket;
import com.busstation.entity.Trip;
import com.busstation.entity.User;
import com.busstation.entity.UserRole;
import com.busstation.entity.UserStatus;
import com.busstation.exception.BusinessException;
import com.busstation.repository.TicketRepository;
import com.busstation.repository.TripRepository;
import com.busstation.repository.UserRepository;
import com.busstation.security.UserPrincipal;
import com.busstation.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Transactional
    public TicketResponse book(TicketCreateRequest request) {
        Trip trip = tripRepository.findLockedById(request.getTripId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));
        if (!trip.isOpenForBooking()) {
            throw BusinessException.conflict("Chuyến xe không còn nhận đặt vé");
        }
        if (request.getSeatNumber() < 1 || request.getSeatNumber() > trip.getTotalSeats()) {
            throw BusinessException.badRequest("Số ghế nằm ngoài sơ đồ ghế của chuyến");
        }
        if (ticketRepository.existsByTripIdAndSeatNumber(trip.getId(), request.getSeatNumber())) {
            throw BusinessException.conflict("Ghế đã được đặt");
        }
        int remaining = trip.getTotalSeats() - Math.toIntExact(ticketRepository.countByTripId(trip.getId()));
        if (remaining <= 0) {
            throw BusinessException.badRequest("Chuyến xe đã hết chỗ");
        }
        UserPrincipal principal = currentUser();
        UUID bookerId = request.getCustomerId() == null ? principal.getId() : request.getCustomerId();
        if (!bookerId.equals(principal.getId()) && !canManageTickets(principal)) {
            throw BusinessException.forbidden("Không có quyền đặt vé cho người khác");
        }
        User user = userRepository.findById(bookerId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.badRequest("Tài khoản đặt vé không hoạt động");
        }
        String ticketCode = CodeGenerator.generate("TKT", ticketRepository.count());
        Ticket ticket = Ticket.builder()
                .trip(trip)
                .user(user)
                .ticketCode(ticketCode)
                .seatNumber(request.getSeatNumber())
                .price(trip.getBasePrice())
                .status("BOOKED")
                .paymentStatus("UNPAID")
                .build();
        trip.setAvailableSeats(remaining - 1);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse pay(UUID id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy vé"));
        if (!ticket.getUser().getId().equals(currentUserId())) {
            throw BusinessException.forbidden("Không có quyền thanh toán vé này");
        }
        if ("PAID".equals(ticket.getPaymentStatus())) {
            return toResponse(ticket);
        }
        if (!"BOOKED".equals(ticket.getStatus()) || !ticket.getTrip().isOpenForBooking()) {
            throw BusinessException.conflict("Vé không còn đủ điều kiện thanh toán");
        }
        ticket.setPaymentStatus("PAID");
        ticket.setStatus("CONFIRMED");
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> search(UUID tripId, String status, String paymentStatus) {
        UserPrincipal principal = currentUser();
        List<Ticket> tickets = canManageTickets(principal)
                ? ticketRepository.findAll() : ticketRepository.findByUserId(principal.getId());
        return tickets.stream()
                .filter(t -> tripId == null || t.getTrip().getId().equals(tripId))
                .filter(t -> status == null || status.isBlank() || t.getStatus().equalsIgnoreCase(status))
                .filter(t -> paymentStatus == null || paymentStatus.isBlank() || t.getPaymentStatus().equalsIgnoreCase(paymentStatus))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> myTickets() {
        return ticketRepository.findByUserId(currentUserId()).stream().map(this::toResponse).toList();
    }

    private UUID currentUserId() {
        return currentUser().getId();
    }

    private UserPrincipal currentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean canManageTickets(UserPrincipal principal) {
        return principal.getRole() == UserRole.ADMIN || principal.getRole() == UserRole.EMPLOYEE;
    }

    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .ticketCode(t.getTicketCode())
                .tripId(t.getTrip().getId())
                .userId(t.getUser().getId())
                .seatNumber(t.getSeatNumber())
                .price(t.getPrice())
                .status(t.getStatus())
                .paymentStatus(t.getPaymentStatus())
                .build();
    }
}

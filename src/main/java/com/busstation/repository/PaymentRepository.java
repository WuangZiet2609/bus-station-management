package com.busstation.repository;

import com.busstation.entity.Payment;
import com.busstation.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByBookingId(UUID bookingId);

    List<Payment> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.gatewayTransactionId = :gatewayTxnId")
    Optional<Payment> findByGatewayTransactionId(@Param("gatewayTxnId") String gatewayTransactionId);

    boolean existsByTransactionId(String transactionId);
}
package com.busstation.service;

import com.busstation.config.AppProperties;
import com.busstation.dto.request.PaymentRequest;
import com.busstation.dto.response.PaymentInitResponse;
import com.busstation.dto.response.PaymentResponse;
import com.busstation.entity.Booking;
import com.busstation.entity.BookingStatus;
import com.busstation.entity.Payment;
import com.busstation.entity.PaymentMethod;
import com.busstation.entity.PaymentStatus;
import com.busstation.exception.BusinessException;
import com.busstation.repository.BookingRepository;
import com.busstation.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final AppProperties appProperties;

    @Transactional
    public PaymentInitResponse initPayment(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn đặt vé"));

        if (booking.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw BusinessException.badRequest("Đơn đặt vé đã thanh toán");
        }

        String transactionId = "TXN" + System.currentTimeMillis();
        Payment payment = Payment.builder()
                .booking(booking)
                .transactionId(transactionId)
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()))
                .amount(booking.getFinalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        booking.setPaymentTransactionId(transactionId);
        bookingRepository.save(booking);

        return switch (PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase())) {
            case VNPAY -> createVnpayPayment(payment, request.getReturnUrl());
            case MOMO -> createMomoPayment(payment, request.getReturnUrl());
            default -> throw BusinessException.badRequest("Phương thức thanh toán không hỗ trợ");
        };
    }

    private PaymentInitResponse createVnpayPayment(Payment payment, String returnUrl) {
        AppProperties.Payment.Vnpay config = appProperties.getPayment().getVnpay();

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", config.getVersion());
        params.put("vnp_Command", config.getCommand());
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Amount", String.valueOf(payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getTransactionId());
        params.put("vnp_OrderInfo", "Thanh toan ve " + payment.getBooking().getBookingCode());
        params.put("vnp_OrderType", config.getOrderType());
        params.put("vnp_Locale", config.getLocale());
        params.put("vnp_ReturnUrl", returnUrl != null ? returnUrl : config.getReturnUrl());
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        String queryString = buildQueryString(params);
        String hashData = config.getHashSecret() + queryString;
        String secureHash = HmacUtils.hmacSha512Hex(config.getHashSecret(), queryString);

        String paymentUrl = config.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;

        return PaymentInitResponse.builder()
                .paymentUrl(paymentUrl)
                .transactionId(payment.getTransactionId())
                .build();
    }

    private PaymentInitResponse createMomoPayment(Payment payment, String returnUrl) {
        AppProperties.Payment.Momo config = appProperties.getPayment().getMomo();

        Map<String, String> params = new HashMap<>();
        params.put("partnerCode", config.getPartnerCode());
        params.put("accessKey", config.getAccessKey());
        params.put("requestId", payment.getTransactionId());
        params.put("amount", payment.getAmount().toString());
        params.put("orderId", payment.getBooking().getBookingCode());
        params.put("orderInfo", "Thanh toan ve " + payment.getBooking().getBookingCode());
        params.put("returnUrl", returnUrl != null ? returnUrl : config.getReturnUrl());
        params.put("notifyUrl", config.getNotifyUrl());
        params.put("requestType", config.getRequestType());
        params.put("extraData", "");

        String rawHash = buildRawHash(params);
        String signature = HmacUtils.hmacSha256Hex(config.getSecretKey(), rawHash);
        params.put("signature", signature);

        return PaymentInitResponse.builder()
                .transactionId(payment.getTransactionId())
                .paymentUrl(config.getEndpoint())
                .build();
    }

    @Transactional
    public void handleVnpayReturn(Map<String, String> params) {
        String vnpTxnRef = params.get("vnp_TxnRef");
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String vnpTransactionNo = params.get("vnp_TransactionNo");
        String vnpSecureHash = params.get("vnp_SecureHash");

        AppProperties.Payment.Vnpay config = appProperties.getPayment().getVnpay();

        String queryString = buildQueryStringForVerify(params);
        String calculatedHash = HmacUtils.hmacSha512Hex(config.getHashSecret(), queryString);

        if (!calculatedHash.equals(vnpSecureHash)) {
            log.error("VNPAY invalid signature for transaction: {}", vnpTxnRef);
            throw BusinessException.badRequest("Chữ ký không hợp lệ");
        }

        Payment payment = paymentRepository.findByTransactionId(vnpTxnRef)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy giao dịch"));

        payment.setGatewayTransactionId(vnpTransactionNo);
        payment.setGatewayResponse(params);

        if ("00".equals(vnpResponseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            Booking booking = payment.getBooking();
            booking.setPaymentStatus(PaymentStatus.SUCCESS);
            booking.setStatus(BookingStatus.PAID);
            booking.setPaidAt(LocalDateTime.now());
            bookingRepository.save(booking);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepository.save(payment);
    }

    @Transactional
    public void handleMomoNotify(Map<String, String> params) {
        String orderId = params.get("orderId");
        String requestId = params.get("requestId");
        String resultCode = params.get("resultCode");
        String transId = params.get("transId");
        String signature = params.get("signature");

        AppProperties.Payment.Momo config = appProperties.getPayment().getMomo();

        Map<String, String> verifyParams = new HashMap<>(params);
        verifyParams.remove("signature");

        String rawHash = buildRawHash(verifyParams);
        String calculatedSignature = HmacUtils.hmacSha256Hex(config.getSecretKey(), rawHash);

        if (!calculatedSignature.equals(signature)) {
            log.error("MoMo invalid signature for order: {}", orderId);
            throw BusinessException.badRequest("Chữ ký không hợp lệ");
        }

        Payment payment = paymentRepository.findByTransactionId(requestId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy giao dịch"));

        payment.setGatewayTransactionId(transId);
        payment.setGatewayResponse(params);

        if ("0".equals(resultCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            Booking booking = payment.getBooking();
            booking.setPaymentStatus(PaymentStatus.SUCCESS);
            booking.setStatus(BookingStatus.PAID);
            booking.setPaidAt(LocalDateTime.now());
            bookingRepository.save(booking);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(UUID bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy giao dịch"));
        return buildPaymentResponse(payment);
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private String buildQueryStringForVerify(Map<String, String> params) {
        Map<String, String> filtered = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!entry.getKey().equals("vnp_SecureHash") && !entry.getKey().equals("vnp_SecureHashType")) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return buildQueryString(filtered);
    }

    private String buildRawHash(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append("&");
            sb.append(keys.get(i)).append("=").append(params.get(keys.get(i)));
        }
        return sb.toString();
    }

    private PaymentResponse buildPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .transactionId(payment.getTransactionId())
                .paymentMethod(payment.getPaymentMethod().name())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .paidAt(payment.getPaidAt())
                .refundedAt(payment.getRefundedAt())
                .refundAmount(payment.getRefundAmount())
                .refundReason(payment.getRefundReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
package com.busstation.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendBookingConfirmationEmail(String to, String bookingCode, String tripCode,
                                             String departureTime, String arrivalTime,
                                             String passengerName, String seats) {
        String subject = "Xác nhận đặt vé - " + bookingCode;
        String content = String.format("""
            <html>
            <body>
                <h2>Xác nhận đặt vé thành công</h2>
                <p>Kính chào <strong>%s</strong>,</p>
                <p>Bạn đã đặt vé thành công với thông tin chi tiết:</p>
                <ul>
                    <li><strong>Mã đặt vé:</strong> %s</li>
                    <li><strong>Chuyến xe:</strong> %s</li>
                    <li><strong>Giờ khởi hành:</strong> %s</li>
                    <li><strong>Giờ đến dự kiến:</strong> %s</li>
                    <li><strong>Ghế:</strong> %s</li>
                </ul>
                <p>Vui lòng có mặt tại bến xe trước 30 phút.</p>
                <p>Trân trọng,<br/>Hệ thống quản lý nhà xe</p>
            </body>
            </html>
            """, passengerName, bookingCode, tripCode, departureTime, arrivalTime, seats);

        sendEmail(to, subject, content);
    }

    @Async
    public void sendTripReminderEmail(String to, String bookingCode, String tripCode,
                                      String departureTime, int minutesBefore) {
        String subject = "Nhắc nhở chuyến xe - " + bookingCode;
        String content = String.format("""
            <html>
            <body>
                <h2>Nhắc nhớ chuyến xe sắp khởi hành</h2>
                <p>Kính chào quý khách,</p>
                <p>Chuyến xe <strong>%s</strong> (mã vé: <strong>%s</strong>) sẽ khởi hành trong <strong>%d phút</strong>.</p>
                <p><strong>Giờ khởi hành:</strong> %s</p>
                <p>Vui lòng di chuyển đến bến xe ngay lập tức.</p>
                <p>Trân trọng,<br/>Hệ thống quản lý nhà xe</p>
            </body>
            </html>
            """, tripCode, bookingCode, minutesBefore, departureTime);

        sendEmail(to, subject, content);
    }
}
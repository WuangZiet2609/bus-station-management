package com.busstation.service;

import com.busstation.dto.response.NotificationResponse;
import com.busstation.dto.response.PageResponse;
import com.busstation.entity.Notification;
import com.busstation.entity.NotificationChannel;
import com.busstation.entity.NotificationType;
import com.busstation.entity.User;
import com.busstation.exception.BusinessException;
import com.busstation.repository.NotificationRepository;
import com.busstation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public NotificationResponse createNotification(UUID userId, NotificationType type, String title, String message,
                                                   NotificationChannel channel, UUID referenceId, String referenceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .channel(channel)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);

        if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.IN_APP) {
            sendNotification(notification);
        }

        return buildNotificationResponse(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(UUID userId, int page, int size, Boolean unreadOnly) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> notificationPage;
        if (Boolean.TRUE.equals(unreadOnly)) {
            List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
            // Convert to page manually for simplicity
            notificationPage = new org.springframework.data.domain.PageImpl<>(
                    notifications, pageable, notifications.size());
        } else {
            notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return PageResponse.<NotificationResponse>builder()
                .content(notificationPage.map(this::buildNotificationResponse).getContent())
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .first(notificationPage.isFirst())
                .last(notificationPage.isLast())
                .empty(notificationPage.isEmpty())
                .build();
    }

    @Transactional
    public void markAsRead(UUID userId, List<UUID> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            notificationRepository.markAllAsRead(userId);
        } else {
            notificationRepository.markAsRead(notificationIds);
        }
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private void sendNotification(Notification notification) {
        try {
            if (notification.getChannel() == NotificationChannel.EMAIL) {
                emailService.sendEmail(notification.getUser().getEmail(), notification.getTitle(), notification.getMessage());
            }
            // WebSocket push would be handled separately

            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
            notification.setFailedAt(LocalDateTime.now());
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    private NotificationResponse buildNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .channel(notification.getChannel().name())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
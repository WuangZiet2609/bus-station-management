package com.busstation.controller;

import com.busstation.dto.response.NotificationResponse;
import com.busstation.dto.response.PageResponse;
import com.busstation.service.NotificationService;
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*;

import java.util.List
import java.util.UUID

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, page, size, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/read")
    public ResponseEntity<Void> markAsRead(@RequestBody(required = false) List<UUID> notificationIds) {
        UUID userId = getCurrentUserId();
        notificationService.markAsRead(userId, notificationIds);
        return ResponseEntity.ok().build();
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.busstation.security.UserPrincipal principal) {
            return principal.getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}
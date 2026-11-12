package com.busstation.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/notification/read")
    public void markNotificationRead(Principal principal, @Payload Map<String, Object> payload) {
        log.info("User {} marked notification as read", principal.getName());
    }

    @MessageMapping("/notification/subscribe")
    public void subscribeNotifications(Principal principal) {
        log.info("User {} subscribed to notifications", principal.getName());
    }

    public void sendNotificationToUser(String username, Object notification) {
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
    }

    public void sendTripUpdate(String tripCode, Object update) {
        messagingTemplate.convertAndSend("/topic/trips/" + tripCode, update);
    }

    public void sendBookingUpdate(String bookingCode, Object update) {
        messagingTemplate.convertAndSend("/topic/bookings/" + bookingCode, update);
    }
}
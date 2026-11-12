package com.busstation.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private boolean success;
    private String message;
    private UserInfo user;
    private String sessionId;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private UUID id;
    private String email;
    private String phone;
    private String fullName;
    private String avatarUrl;
    private String role;
    private String status;
    private boolean emailVerified;
    private boolean phoneVerified;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
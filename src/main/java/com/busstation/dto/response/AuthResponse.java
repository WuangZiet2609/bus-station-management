package com.busstation.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    boolean success;
    String message;
    UserInfo user;
}

package com.busstation.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class UserInfo {
    UUID id;
    String customerCode;
    String email;
    String phone;
    String fullName;
    String role;
}

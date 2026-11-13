package com.busstation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DriverRequest {
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String fullName;
    private String phone;
    private String password;
}

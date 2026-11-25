package com.busstation.controller;

import com.busstation.dto.request.LoginRequest;
import com.busstation.dto.request.RegisterRequest;
import com.busstation.dto.response.AuthResponse;
import com.busstation.dto.response.UserInfo;
import com.busstation.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfo> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}

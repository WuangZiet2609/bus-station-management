package com.busstation.controller;

import com.busstation.dto.request.RegisterRequest;
import com.busstation.dto.response.PageResponse;
import com.busstation.dto.response.UserInfo;
import com.busstation.entity.UserRole;
import com.busstation.entity.UserStatus;
import com.busstation.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<PageResponse<UserInfo>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(userService.getAllUsers(page, size, role, status, keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserInfo> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserInfo> createUser(@Valid @RequestBody RegisterRequest request,
                                               @RequestParam UserRole role) {
        return ResponseEntity.ok(userService.createUser(request, role));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserInfo> updateUser(@PathVariable UUID id,
                                               @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserInfo> changeStatus(@PathVariable UUID id,
                                                 @RequestParam UserStatus status) {
        return ResponseEntity.ok(userService.changeStatus(id, status));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserInfo> changeRole(@PathVariable UUID id,
                                               @RequestParam UserRole role) {
        return ResponseEntity.ok(userService.changeRole(id, role));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<UserInfo> resetPassword(@PathVariable UUID id,
                                                  @RequestParam String newPassword) {
        return ResponseEntity.ok(userService.resetPassword(id, newPassword));
    }

    @GetMapping("/drivers")
    public ResponseEntity<List<UserInfo>> getDrivers() {
        return ResponseEntity.ok(userService.getDrivers());
    }

    @GetMapping("/employees")
    public ResponseEntity<List<UserInfo>> getEmployees() {
        return ResponseEntity.ok(userService.getEmployees());
    }
}
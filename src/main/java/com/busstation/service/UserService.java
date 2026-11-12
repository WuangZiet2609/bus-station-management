package com.busstation.service;

import com.busstation.dto.request.RegisterRequest;
import com.busstation.dto.response.PageResponse;
import com.busstation.dto.response.UserInfo;
import com.busstation.entity.User;
import com.busstation.entity.UserRole;
import com.busstation.entity.UserStatus;
import com.busstation.exception.BusinessException;
import com.busstation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UserInfo> getAllUsers(int page, int size, String role, String status, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        UserRole roleEnum = role != null ? UserRole.valueOf(role.toUpperCase()) : null;
        UserStatus statusEnum = status != null ? UserStatus.valueOf(status.toUpperCase()) : null;

        Page<User> userPage;
        if (roleEnum != null && statusEnum != null) {
            userPage = userRepository.findByRoleAndStatus(roleEnum, statusEnum, pageable);
        } else if (roleEnum != null) {
            userPage = userRepository.findByRole(roleEnum, pageable);
        } else if (statusEnum != null) {
            userPage = userRepository.findByStatus(statusEnum, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        if (keyword != null && !keyword.isBlank()) {
            // For simplicity, we'll filter in memory. In production, use a custom query.
            List<User> filtered = userPage.getContent().stream()
                    .filter(u -> u.getEmail().toLowerCase().contains(keyword.toLowerCase())
                            || u.getFullName().toLowerCase().contains(keyword.toLowerCase())
                            || (u.getPhone() != null && u.getPhone().contains(keyword)))
                    .toList();
            // Note: This is a simplified approach. Better to use a custom query for production.
        }

        return PageResponse.<UserInfo>builder()
                .content(userPage.map(this::buildUserInfo).getContent())
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .empty(userPage.isEmpty())
                .build();
    }

    @Transactional(readOnly = true)
    public UserInfo getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));
        return buildUserInfo(user);
    }

    @Transactional
    public UserInfo createUser(RegisterRequest request, UserRole role) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict("Email đã được đăng ký");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw BusinessException.conflict("Số điện thoại đã được đăng ký");
        }

        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .identityCard(request.getIdentityCard())
                .address(request.getAddress())
                .role(role)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(true)
                .build();

        user = userRepository.save(user);
        log.info("Admin created user: {}", user.getEmail());

        return buildUserInfo(user);
    }

    @Transactional
    public UserInfo updateUser(UUID id, RegisterRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict("Email đã được sử dụng");
        }
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw BusinessException.conflict("Số điện thoại đã được sử dụng");
        }

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setIdentityCard(request.getIdentityCard());
        user.setAddress(request.getAddress());

        user = userRepository.save(user);
        log.info("User updated: {}", user.getEmail());

        return buildUserInfo(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));

        userRepository.delete(user);
        log.info("User deleted: {}", user.getEmail());
    }

    @Transactional
    public UserInfo changeStatus(UUID id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));

        user.setStatus(status);
        user = userRepository.save(user);

        return buildUserInfo(user);
    }

    @Transactional
    public UserInfo changeRole(UUID id, UserRole role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));

        user.setRole(role);
        user = userRepository.save(user);

        return buildUserInfo(user);
    }

    @Transactional
    public UserInfo resetPassword(UUID id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user = userRepository.save(user);

        return buildUserInfo(user);
    }

    @Transactional(readOnly = true)
    public List<UserInfo> getDrivers() {
        return userRepository.findByRole(UserRole.DRIVER).stream()
                .map(this::buildUserInfo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserInfo> getEmployees() {
        return userRepository.findByRole(UserRole.EMPLOYEE).stream()
                .map(this::buildUserInfo)
                .toList();
    }

    private UserInfo buildUserInfo(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
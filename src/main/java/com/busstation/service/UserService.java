package com.busstation.service;

import com.busstation.dto.request.DriverRequest;
import com.busstation.dto.response.UserInfo;
import com.busstation.entity.User;
import com.busstation.entity.UserRole;
import com.busstation.entity.UserStatus;
import com.busstation.exception.BusinessException;
import com.busstation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserInfo> findDrivers() {
        return userRepository.findByRole(UserRole.DRIVER).stream().map(this::toInfo).toList();
    }

    @Transactional
    public UserInfo createDriver(DriverRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict("Email đã được đăng ký");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw BusinessException.badRequest("Mật khẩu tài xế phải có ít nhất 8 ký tự");
        }
        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.DRIVER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(true)
                .build();
        return toInfo(userRepository.save(user));
    }

    @Transactional
    public UserInfo updateDriver(UUID id, DriverRequest request) {
        User user = findDriver(id);
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict("Email đã được đăng ký");
        }
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())
                && userRepository.existsByPhone(request.getPhone())) {
            throw BusinessException.conflict("Số điện thoại đã được đăng ký");
        }
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 8) {
                throw BusinessException.badRequest("Mật khẩu phải có ít nhất 8 ký tự");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        return toInfo(user);
    }

    @Transactional
    public void deleteDriver(UUID id) {
        userRepository.delete(findDriver(id));
    }

    private User findDriver(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tài xế"));
        if (user.getRole() != UserRole.DRIVER) {
            throw BusinessException.notFound("Không tìm thấy tài xế");
        }
        return user;
    }

    private UserInfo toInfo(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}

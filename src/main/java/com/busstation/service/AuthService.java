package com.busstation.service;

import com.busstation.dto.request.LoginRequest;
import com.busstation.dto.request.RegisterRequest;
import com.busstation.dto.response.AuthResponse;
import com.busstation.dto.response.UserInfo;
import com.busstation.entity.User;
import com.busstation.entity.UserRole;
import com.busstation.entity.UserStatus;
import com.busstation.exception.BusinessException;
import com.busstation.repository.UserRepository;
import com.busstation.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict("Email đã được đăng ký");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw BusinessException.conflict("Số điện thoại đã được đăng ký");
        }
        if (request.getIdentityCard() != null && userRepository.existsByIdentityCard(request.getIdentityCard())) {
            throw BusinessException.conflict("CCCD/CMND đã được đăng ký");
        }

        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .identityCard(request.getIdentityCard())
                .address(request.getAddress())
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(true)
                .build();

        user = userRepository.save(user);
        return AuthResponse.builder()
                .success(true)
                .message("Đăng ký thành công")
                .user(toUserInfo(user))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            httpRequest.changeSessionId();
        }
        session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> BusinessException.unauthorized("Tài khoản không tồn tại"));
        user.setLastLoginAt(java.time.LocalDateTime.now());

        return AuthResponse.builder()
                .success(true)
                .message("Đăng nhập thành công")
                .user(toUserInfo(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserInfo getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw BusinessException.unauthorized("Chưa đăng nhập");
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));
        return toUserInfo(user);
    }

    private UserInfo toUserInfo(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}

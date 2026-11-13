package com.busstation.repository;

import com.busstation.entity.User;
import com.busstation.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByIdentityCard(String identityCard);
    List<User> findByRole(UserRole role);
}

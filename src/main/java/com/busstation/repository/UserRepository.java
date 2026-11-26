package com.busstation.repository;

import com.busstation.entity.User;
import com.busstation.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from User r where r.id = :id")
    Optional<User> findLockedById(UUID id);

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByIdentityCard(String identityCard);
    boolean existsByCustomerCode(String customerCode);
    Optional<User> findByCustomerCode(String customerCode);
    List<User> findByRole(UserRole role);
}

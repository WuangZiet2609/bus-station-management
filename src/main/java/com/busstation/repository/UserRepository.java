package com.busstation.repository;

import com.busstation.entity.User;
import com.busstation.entity.UserRole;
import com.busstation.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByIdentityCard(String identityCard);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<User> findByRole(UserRole role);

    List<User> findByStatus(UserStatus status);

    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role IN :roles AND u.status = :status")
    List<User> findByRolesAndStatus(@Param("roles") List<UserRole> roles, @Param("status") UserStatus status);

    @Query("SELECT u FROM User u WHERE u.email = :email OR u.phone = :phone")
    Optional<User> findByEmailOrPhone(@Param("email") String email, @Param("phone") String phone);
}
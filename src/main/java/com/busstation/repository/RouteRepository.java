package com.busstation.repository;

import com.busstation.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RouteRepository extends JpaRepository<Route, UUID> {
    boolean existsByCode(String code);
    Optional<Route> findByCode(String code);
}

package com.busstation.repository;

import com.busstation.entity.Route;
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
public interface RouteRepository extends JpaRepository<Route, UUID> {

    Optional<Route> findByCode(String code);

    List<Route> findByIsActiveTrue();

    Page<Route> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT r FROM Route r WHERE (LOWER(r.departureProvince) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.arrivalProvince) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND r.isActive = true")
    List<Route> search(@Param("keyword") String keyword);

    @Query("SELECT r FROM Route r WHERE r.departureProvince = :departure AND r.arrivalProvince = :arrival AND r.isActive = true")
    List<Route> findByDepartureAndArrival(@Param("departure") String departure, @Param("arrival") String arrival);
}
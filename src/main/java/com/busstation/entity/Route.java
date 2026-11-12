package com.busstation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "departure_province", nullable = false, length = 100)
    private String departureProvince;

    @Column(name = "departure_district", length = 100)
    private String departureDistrict;

    @Column(name = "departure_ward", length = 100)
    private String departureWard;

    @Column(name = "departure_address", columnDefinition = "TEXT")
    private String departureAddress;

    @Column(name = "arrival_province", nullable = false, length = 100)
    private String arrivalProvince;

    @Column(name = "arrival_district", length = 100)
    private String arrivalDistrict;

    @Column(name = "arrival_ward", length = 100)
    private String arrivalWard;

    @Column(name = "arrival_address", columnDefinition = "TEXT")
    private String arrivalAddress;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private Double distanceKm;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
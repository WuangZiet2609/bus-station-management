package com.busstation.service;

import com.busstation.dto.request.RouteCreateRequest;
import com.busstation.dto.request.RouteUpdateRequest;
import com.busstation.entity.Route;
import com.busstation.exception.BusinessException;
import com.busstation.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;

    public List<Route> findAll() {
        return routeRepository.findAll();
    }

    public Route findById(UUID id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tuyến đường"));
    }

    @Transactional
    public Route create(RouteCreateRequest request) {
        if (routeRepository.existsByCode(request.getCode())) {
            throw BusinessException.conflict("Mã tuyến đã tồn tại");
        }
        return routeRepository.save(Route.builder()
                .name(request.getName())
                .code(request.getCode())
                .departureProvince(request.getDepartureProvince())
                .arrivalProvince(request.getArrivalProvince())
                .distanceKm(request.getDistanceKm())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .isActive(true)
                .build());
    }

    @Transactional
    public Route update(UUID id, RouteUpdateRequest request) {
        Route route = findById(id);
        if (request.getCode() != null && !request.getCode().equals(route.getCode())
                && routeRepository.existsByCode(request.getCode())) {
            throw BusinessException.conflict("Mã tuyến đã tồn tại");
        }
        if (request.getName() != null) route.setName(request.getName());
        if (request.getCode() != null) route.setCode(request.getCode());
        if (request.getDepartureProvince() != null) route.setDepartureProvince(request.getDepartureProvince());
        if (request.getArrivalProvince() != null) route.setArrivalProvince(request.getArrivalProvince());
        if (request.getDistanceKm() != null) route.setDistanceKm(request.getDistanceKm());
        if (request.getEstimatedDurationMinutes() != null) {
            route.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        }
        if (request.getIsActive() != null) route.setIsActive(request.getIsActive());
        return route;
    }

    public void delete(UUID id) {
        routeRepository.delete(findById(id));
    }
}

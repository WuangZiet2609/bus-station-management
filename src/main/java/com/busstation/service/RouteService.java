package com.busstation.service;

import com.busstation.dto.request.RouteCreateRequest;
import com.busstation.dto.request.RouteUpdateRequest;
import com.busstation.dto.response.PageResponse;
import com.busstation.dto.response.RouteResponse;
import com.busstation.entity.Route;
import com.busstation.exception.BusinessException;
import com.busstation.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteService {

    private final RouteRepository routeRepository;

    @Transactional
    public RouteResponse createRoute(RouteCreateRequest request) {
        if (routeRepository.existsByCode(request.getCode())) {
            throw BusinessException.conflict("Mã tuyến đã tồn tại");
        }

        Route route = Route.builder()
                .name(request.getName())
                .code(request.getCode())
                .departureProvince(request.getDepartureProvince())
                .departureDistrict(request.getDepartureDistrict())
                .departureWard(request.getDepartureWard())
                .departureAddress(request.getDepartureAddress())
                .arrivalProvince(request.getArrivalProvince())
                .arrivalDistrict(request.getArrivalDistrict())
                .arrivalWard(request.getArrivalWard())
                .arrivalAddress(request.getArrivalAddress())
                .distanceKm(request.getDistanceKm())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .description(request.getDescription())
                .isActive(true)
                .build();

        route = routeRepository.save(route);
        log.info("Route created: {}", route.getCode());

        return buildRouteResponse(route);
    }

    @Transactional(readOnly = true)
    public PageResponse<RouteResponse> getAllRoutes(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Route> routePage;
        if (keyword != null && !keyword.isBlank()) {
            List<Route> routes = routeRepository.search(keyword);
            // Simplified - in production use custom query with pagination
            routePage = new org.springframework.data.domain.PageImpl<>(routes, pageable, routes.size());
        } else {
            routePage = routeRepository.findByIsActiveTrue(pageable);
        }

        return PageResponse.<RouteResponse>builder()
                .content(routePage.map(this::buildRouteResponse).getContent())
                .page(routePage.getNumber())
                .size(routePage.getSize())
                .totalElements(routePage.getTotalElements())
                .totalPages(routePage.getTotalPages())
                .first(routePage.isFirst())
                .last(routePage.isLast())
                .empty(routePage.isEmpty())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> searchRoutes(String departure, String arrival) {
        return routeRepository.findByDepartureAndArrival(departure, arrival).stream()
                .map(this::buildRouteResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RouteResponse getRouteById(UUID id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tuyến đường"));
        return buildRouteResponse(route);
    }

    @Transactional
    public RouteResponse updateRoute(UUID id, RouteUpdateRequest request) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tuyến đường"));

        if (request.getName() != null) route.setName(request.getName());
        if (request.getCode() != null) {
            if (routeRepository.existsByCode(request.getCode()) && !route.getCode().equals(request.getCode())) {
                throw BusinessException.conflict("Mã tuyến đã tồn tại");
            }
            route.setCode(request.getCode());
        }
        if (request.getDepartureProvince() != null) route.setDepartureProvince(request.getDepartureProvince());
        if (request.getDepartureDistrict() != null) route.setDepartureDistrict(request.getDepartureDistrict());
        if (request.getDepartureWard() != null) route.setDepartureWard(request.getDepartureWard());
        if (request.getDepartureAddress() != null) route.setDepartureAddress(request.getDepartureAddress());
        if (request.getArrivalProvince() != null) route.setArrivalProvince(request.getArrivalProvince());
        if (request.getArrivalDistrict() != null) route.setArrivalDistrict(request.getArrivalDistrict());
        if (request.getArrivalWard() != null) route.setArrivalWard(request.getArrivalWard());
        if (request.getArrivalAddress() != null) route.setArrivalAddress(request.getArrivalAddress());
        if (request.getDistanceKm() != null) route.setDistanceKm(request.getDistanceKm());
        if (request.getEstimatedDurationMinutes() != null) route.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        if (request.getDescription() != null) route.setDescription(request.getDescription());
        if (request.getIsActive() != null) route.setIsActive(request.getIsActive());

        route = routeRepository.save(route);
        return buildRouteResponse(route);
    }

    @Transactional
    public void deleteRoute(UUID id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tuyến đường"));
        routeRepository.delete(route);
    }

    @Transactional
    public RouteResponse changeStatus(UUID id, Boolean isActive) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tuyến đường"));

        route.setIsActive(isActive);
        route = routeRepository.save(route);

        return buildRouteResponse(route);
    }

    private RouteResponse buildRouteResponse(Route route) {
        return RouteResponse.builder()
                .id(route.getId())
                .name(route.getName())
                .code(route.getCode())
                .departureProvince(route.getDepartureProvince())
                .departureDistrict(route.getDepartureDistrict())
                .departureWard(route.getDepartureWard())
                .departureAddress(route.getDepartureAddress())
                .arrivalProvince(route.getArrivalProvince())
                .arrivalDistrict(route.getArrivalDistrict())
                .arrivalWard(route.getArrivalWard())
                .arrivalAddress(route.getArrivalAddress())
                .distanceKm(route.getDistanceKm())
                .estimatedDurationMinutes(route.getEstimatedDurationMinutes())
                .description(route.getDescription())
                .isActive(route.getIsActive())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}
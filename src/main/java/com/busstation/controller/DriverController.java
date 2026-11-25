package com.busstation.controller;

import com.busstation.dto.request.DriverRequest;
import com.busstation.dto.response.UserInfo;
import com.busstation.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DriverController {
    private final UserService userService;

    @GetMapping
    public List<UserInfo> findAll() {
        return userService.findDrivers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserInfo create(@Valid @RequestBody DriverRequest request) {
        return userService.createDriver(request);
    }

    @PutMapping("/{id}")
    public UserInfo update(@PathVariable UUID id, @Valid @RequestBody DriverRequest request) {
        return userService.updateDriver(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        userService.deleteDriver(id);
    }
}

package org.covoiturage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.route.RouteCreateRequest;
import org.covoiturage.dto.route.RouteResponse;
import org.covoiturage.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class RouteController {
    private final RouteService routeService;


    @PostMapping("/driver/{driverId}")
    public ResponseEntity<RouteResponse> create(@PathVariable UUID driverId,
                                                @Valid @RequestBody RouteCreateRequest request) {
        RouteResponse created = routeService.create(driverId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/routes/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

}

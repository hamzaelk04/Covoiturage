package org.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.route.RouteCreateRequest;
import org.covoiturage.dto.route.RouteResponse;
import org.covoiturage.dto.route.RouteUpdateRequest;
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

    @PutMapping("/driver/{driverId}/routes/{id}")
    @Operation(summary = "Modifier un trajet")
    public ResponseEntity<RouteResponse> update(@PathVariable UUID driverId,
                                                @PathVariable Long id,
                                                @Valid @RequestBody RouteUpdateRequest request) {
        return ResponseEntity.ok(routeService.update(driverId, id, request));
    }

}

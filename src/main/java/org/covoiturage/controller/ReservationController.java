package org.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.reservation.ReservationCreateRequest;
import org.covoiturage.dto.reservation.ReservationResponse;
import org.covoiturage.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Gestion des réservations")
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping("/passengers/{passengerId}/reservations")
    @Operation(summary = "Réserver des places sur un trajet")
    public ResponseEntity<ReservationResponse> create(@PathVariable UUID passengerId,
                                                      @Valid @RequestBody ReservationCreateRequest request) {
        ReservationResponse created = reservationService.create(passengerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/passengers/{passengerId}/reservations")
    @Operation(summary = "Consulter l'historique des réservations d'un passager")
    public ResponseEntity<List<ReservationResponse>> getByPassenger(@PathVariable UUID passengerId) {
        return ResponseEntity.ok(reservationService.getByPassenger(passengerId));
    }

    @GetMapping("/passengers/{passengerId}/reservations/{id}")
    @Operation(summary = "Consulter une réservation")
    public ResponseEntity<ReservationResponse> getById(@PathVariable UUID passengerId, @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(passengerId, id));
    }

    @DeleteMapping("/passengers/{passengerId}/reservations/{id}")
    @Operation(summary = "Annuler une réservation")
    public ResponseEntity<Void> cancel(@PathVariable UUID passengerId, @PathVariable Long id) {
        reservationService.cancel(passengerId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/driver/{driverId}/routes/{routeId}/reservations")
    @Operation(summary = "Consulter les réservations d'un trajet")
    public ResponseEntity<List<ReservationResponse>> getByRoute(@PathVariable UUID driverId,
                                                                @PathVariable Long routeId) {
        return ResponseEntity.ok(reservationService.getByRoute(driverId, routeId));
    }
}
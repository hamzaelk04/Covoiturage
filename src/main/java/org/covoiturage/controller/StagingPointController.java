package org.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.stagingPoint.StagingPointResponse;
import org.covoiturage.dto.stagingPoint.StagingPointUpdateRequest;
import org.covoiturage.service.StagingPointService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Staging points", description = "Gestion des points d'arrêt")
public class StagingPointController {
    private final StagingPointService stagingPointService;

    @GetMapping("/routes/{routeId}/staging-points")
    @Operation(summary = "Lister les points d'arrêt d'un trajet")
    public ResponseEntity<List<StagingPointResponse>> findByRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(stagingPointService.findByRoute(routeId));
    }

    @GetMapping("/routes/{routeId}/staging-points/{id}")
    @Operation(summary = "Consulter un point d'arrêt")
    public ResponseEntity<StagingPointResponse> findById(@PathVariable Long routeId, @PathVariable Long id) {
        return ResponseEntity.ok(stagingPointService.findById(routeId, id));
    }

    @PutMapping("/driver/{driverId}/routes/{routeId}/staging-points/{id}")
    @Operation(summary = "Modifier un point d'arrêt")
    public ResponseEntity<StagingPointResponse> update(@PathVariable UUID driverId,
                                                       @PathVariable Long routeId,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody StagingPointUpdateRequest request) {
        return ResponseEntity.ok(stagingPointService.update(driverId, routeId, id, request));
    }

    @DeleteMapping("/driver/{driverId}/routes/{routeId}/staging-points/{id}")
    @Operation(summary = "Supprimer un point d'arrêt")
    public ResponseEntity<Void> delete(@PathVariable UUID driverId,
                                       @PathVariable Long routeId,
                                       @PathVariable Long id) {
        stagingPointService.delete(driverId, routeId, id);
        return ResponseEntity.noContent().build();
    }
}
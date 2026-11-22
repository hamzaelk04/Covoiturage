package org.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.route.RouteCreateRequest;
import org.covoiturage.dto.route.RouteResponse;
import org.covoiturage.dto.route.RouteSearchCriteria;
import org.covoiturage.dto.route.RouteUpdateRequest;
import org.covoiturage.service.RouteService;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Routes", description = "Gestion des routes")
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

    @DeleteMapping("/driver/{driverId}/routes/{id}")
    @Operation(summary = "Supprimer un trajet")
    public ResponseEntity<Void> delete(@PathVariable UUID driverId, @PathVariable Long id) {
        routeService.delete(driverId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/routes/search")
    @Operation(summary = "Rechercher des trajets par critères")
    public ResponseEntity<PagedModel<RouteResponse>> search(@Valid @ModelAttribute RouteSearchCriteria criteria,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new PagedModel<>(routeService.search(criteria, page, size)));
    }

    @GetMapping("/routes/{id}")
    @Operation(summary = "Consulter le détail d'un trajet")
    public ResponseEntity<RouteResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.getById(id));
    }

    @GetMapping("/driver/{driverId}/routes")
    @Operation(summary = "Lister les trajets d'un conducteur")
    public ResponseEntity<List<RouteResponse>> getByDriver(@PathVariable UUID driverId) {
        return ResponseEntity.ok(routeService.getByDriver(driverId));
    }

    @PatchMapping("/driver/{driverId}/routes/{id}/complete")
    @Operation(summary = "Terminer un trajet")
    public ResponseEntity<Void> complete(@PathVariable UUID driverId, @PathVariable Long id) {
        routeService.complete(driverId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/driver/{driverId}/routes/{id}/cancel")
    @Operation(summary = "Annuler un trajet et ses réservations")
    public ResponseEntity<Void> cancel(@PathVariable UUID driverId, @PathVariable Long id) {
        routeService.cancel(driverId, id);
        return ResponseEntity.noContent().build();
    }
}

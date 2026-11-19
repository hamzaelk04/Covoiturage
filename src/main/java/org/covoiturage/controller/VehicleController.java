package org.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.vehicle.VehicleCreateRequest;
import org.covoiturage.dto.vehicle.VehicleResponse;
import org.covoiturage.dto.vehicle.VehicleUpdateRequest;
import org.covoiturage.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Gestion des vehicles")
public class VehicleController {
    private final VehicleService vehicleService;

//    CREATE
    @PostMapping("/drivers/{driverId}/vehicles")
    @Operation(summary = "Creer une vehicule")
    public ResponseEntity<VehicleResponse> create(@PathVariable UUID driverId, @Valid @RequestBody VehicleCreateRequest request) {
        VehicleResponse body = vehicleService.create(driverId, request);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{id}")
                .buildAndExpand(body.getId())
                .toUri();

        return ResponseEntity.created(location).body(body);
    }

//    UPDATE
    @PutMapping("/drivers/{driverId}/vehicles/{id}")
    @Operation(summary = "Modifier une vehicule")
    public ResponseEntity<VehicleResponse> update(@PathVariable UUID driverId,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody VehicleUpdateRequest request) {
        return ResponseEntity.ok(vehicleService.update(driverId, id, request));
    }

//    DELETE
    @DeleteMapping("/drivers/{driverId}/vehicle/{id}")
    @Operation(summary = "Modifier une vehicule")
    public ResponseEntity<Void> delete(
            @PathVariable UUID driverId,
            @PathVariable Long id
    ) {
        vehicleService.delete(id, driverId);
        return ResponseEntity.noContent().build();
    }

}

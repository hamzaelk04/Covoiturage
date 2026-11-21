package org.covoiturage.dto.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.covoiturage.dto.stagingPoint.StagingPointRequest;

import java.util.List;

public class RouteCreateRequest {
    @NotNull(message = "L'identifiant du véhicule est obligatoire")
    Long vehicleId;

    @NotNull(message = "Le nombre de places proposées est obligatoire")
    @Min(value = 1, message = "Au moins 1 place doit être proposée")
    Integer availablePlaces;

    @NotNull(message = "La liste des points d'arrêt est obligatoire")
    @Size(min = 2, message = "Un trajet doit contenir au moins deux points d'arrêt")
    @Valid
    List<StagingPointRequest> stagingPoints;
}

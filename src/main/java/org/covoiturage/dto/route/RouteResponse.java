package org.covoiturage.dto.route;

import org.covoiturage.dto.stagingPoint.StagingPointResponse;
import org.covoiturage.enums.RouteStatus;

import java.time.LocalDateTime;
import java.util.List;

public class RouteResponse {
    Long id;
    String startCity;
    String arrivalCity;
    LocalDateTime startDate;
    int availablePlaces;
    RouteStatus status;
    Long vehicleId;
    List<StagingPointResponse> stagingPoints;
}

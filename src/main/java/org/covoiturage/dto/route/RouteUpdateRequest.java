package org.covoiturage.dto.route;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RouteUpdateRequest {
    private Long vehicleId;

    @Min(value = 1, message = "Au moins 1 place doit être proposée")
    private Integer availablePlaces;
}
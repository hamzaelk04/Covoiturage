package org.covoiturage.dto.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReservationCreateRequest {
    @NotNull(message = "L'identifiant du trajet est obligatoire")
    private Long routeId;

    @NotNull(message = "Le point de montée est obligatoire")
    private Long boardingPointId;

    @NotNull(message = "Le point de descente est obligatoire")
    private Long descentPointId;

    @NotNull(message = "Le nombre de places est obligatoire")
    @Min(value = 1, message = "Au moins 1 place doit être réservée")
    private Integer numberOfSeats;
}
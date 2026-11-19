package org.covoiturage.dto.vehicle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VehicleUpdateRequest {
    private String brand;

    private String model;

    private String registrationNumber;

    @Min(value = 1, message = "La capacité doit être au moins 1")
    @Max(value = 8, message = "La capacité ne peut pas dépasser 8")
    private Integer capacity;
}

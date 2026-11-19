package org.covoiturage.dto.vehicle;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class VehicleCreateRequest {

    @NotBlank(message = "La marque est obligatoire")
    private String brand;

    @NotBlank(message = "Le model est obligatoire")
    private String model;

    @NotBlank(message = "L'immatriculation est obligatoire")
    private String registrationNumber;

    @NotNull(message = "La capacite est obligatoire")
    @Min(value = 1, message = "La capacité doit être au moins 1")
    @Max(value = 8, message = "La capacité ne peut pas dépasser 8")
    private int capacity;
}

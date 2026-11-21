package org.covoiturage.dto.stagingPoint;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StagingPointRequest {
    @NotBlank(message = "La ville est obligatoire")
    String city;

    @NotNull(message = "L'ordre du point est obligatoire")
    @Min(value = 0, message = "L'ordre doit être >= 0 (0 = départ)")
    Integer stopOrder;

    @NotNull(message = "L'heure de passage est obligatoire")
    LocalDateTime passingTime;

    @NotNull(message = "Le prix depuis le départ est obligatoire")
    @DecimalMin(value = "0.00", message = "Le prix ne peut pas être négatif")
    @Digits(integer = 8, fraction = 2, message = "Le prix doit avoir au maximum 2 décimales")
    BigDecimal price;
}

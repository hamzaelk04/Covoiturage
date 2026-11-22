package org.covoiturage.dto.stagingPoint;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class StagingPointUpdateRequest {
    @Pattern(regexp = ".*\\S.*", message = "La ville ne peut pas être vide")
    private String city;

    private LocalDateTime passingTime;

    @DecimalMin(value = "0.00", message = "Le prix ne peut pas être négatif")
    @Digits(integer = 8, fraction = 2, message = "Le prix doit avoir au maximum 2 décimales")
    private BigDecimal price;
}
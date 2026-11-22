package org.covoiturage.dto.route;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class RouteSearchCriteria {
    private String startCity;

    private String arrivalCity;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @Min(value = 1, message = "Au moins 1 place doit être demandée")
    private Integer seats;

    @DecimalMin(value = "0.00", message = "Le prix maximal ne peut pas être négatif")
    private BigDecimal maxPrice;
}
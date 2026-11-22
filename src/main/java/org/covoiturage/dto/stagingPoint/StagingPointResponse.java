package org.covoiturage.dto.stagingPoint;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class StagingPointResponse {
    Long id;
    String city;
    int stopOrder;
    LocalDateTime passingTime;
    BigDecimal price;
}

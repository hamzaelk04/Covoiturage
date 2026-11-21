package org.covoiturage.dto.stagingPoint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StagingPointResponse {
    Long id;
    String city;
    int stopOrder;
    LocalDateTime passingTime;
    BigDecimal price;
}

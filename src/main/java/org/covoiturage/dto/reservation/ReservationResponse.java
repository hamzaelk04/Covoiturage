package org.covoiturage.dto.reservation;

import lombok.*;
import org.covoiturage.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    Long id;
    UUID passengerId;
    Long routeId;
    Long boardingPointId;
    String boardingCity;
    Long descentPointId;
    String descentCity;
    int numberOfSeats;
    BigDecimal totalAmount;
    LocalDateTime reservationDate;
    ReservationStatus status;
    Integer remainingPlaces;
}
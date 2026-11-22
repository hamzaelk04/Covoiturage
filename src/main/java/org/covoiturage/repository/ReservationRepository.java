package org.covoiturage.repository;

import org.covoiturage.entity.Reservation;
import org.covoiturage.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByBoardingPointRouteId(Long routeId);

    boolean existsByBoardingPointRouteIdAndStatus(Long routeId, ReservationStatus status);

    boolean existsByBoardingPointIdOrDescentPointId(Long boardingPointId, Long descentPointId);
}
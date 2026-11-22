package org.covoiturage.repository;

import org.covoiturage.entity.Reservation;
import org.covoiturage.enums.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByBoardingPointRouteId(Long routeId);

    boolean existsByBoardingPointRouteIdAndStatus(Long routeId, ReservationStatus status);

    boolean existsByBoardingPointIdOrDescentPointId(Long boardingPointId, Long descentPointId);

    boolean existsByPassengerIdAndBoardingPointRouteIdAndStatus(UUID passengerId, Long routeId, ReservationStatus status);

    List<Reservation> findByBoardingPointRouteIdAndStatus(Long routeId, ReservationStatus status);

    @EntityGraph(attributePaths = {"boardingPoint", "descentPoint"})
    List<Reservation> findByPassengerIdOrderByReservationDateDesc(UUID passengerId);

    @EntityGraph(attributePaths = {"boardingPoint", "descentPoint", "passenger"})
    List<Reservation> findByBoardingPointRouteIdOrderByReservationDateDesc(Long routeId);

    @Query("""
            SELECT COALESCE(SUM(r.numberOfSeats), 0L) FROM Reservation r
            WHERE r.boardingPoint.route.id = :routeId AND r.status = :status
            """)
    long sumSeatsByRouteIdAndStatus(@Param("routeId") Long routeId, @Param("status") ReservationStatus status);
}
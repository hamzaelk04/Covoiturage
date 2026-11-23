package org.covoiturage.repository;

import jakarta.persistence.EntityManager;
import org.covoiturage.entity.Passenger;
import org.covoiturage.entity.Reservation;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.testutil.TestPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private EntityManager em;

    private TestPersistence db;
    private Route route;
    private Route emptyRoute;
    private Passenger salma;
    private Passenger karim;
    private Reservation salmaReservation;
    private Reservation karimReservation;

    @BeforeEach
    void setUp() {
        db = new TestPersistence(em);
        Vehicle vehicle = db.vehicle(db.driver(), 4);
        route = db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(3));
        emptyRoute = db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(4));
        salma = db.passenger();
        karim = db.passenger();
        salmaReservation = db.reservation(salma, route, 2, 3, 2, ReservationStatus.CONFIRMED, LocalDateTime.now().minusDays(2));
        karimReservation = db.reservation(karim, route, 1, 3, 1, ReservationStatus.CANCELED, LocalDateTime.now().minusDays(1));
        db.flushAndClear();
    }

    @Test
    void existsByBoardingPointRouteId_dependsOnRoute() {
        assertThat(reservationRepository.existsByBoardingPointRouteId(route.getId())).isTrue();
        assertThat(reservationRepository.existsByBoardingPointRouteId(emptyRoute.getId())).isFalse();
    }

    @Test
    void existsByBoardingPointRouteIdAndStatus_dependsOnStatus() {
        assertThat(reservationRepository.existsByBoardingPointRouteIdAndStatus(route.getId(), ReservationStatus.CONFIRMED)).isTrue();
        assertThat(reservationRepository.existsByBoardingPointRouteIdAndStatus(route.getId(), ReservationStatus.CANCELED)).isTrue();
        assertThat(reservationRepository.existsByBoardingPointRouteIdAndStatus(route.getId(), ReservationStatus.DRAFT)).isFalse();
        assertThat(reservationRepository.existsByBoardingPointRouteIdAndStatus(emptyRoute.getId(), ReservationStatus.CONFIRMED)).isFalse();
    }

    @Test
    void existsByBoardingPointIdOrDescentPointId_detectsBoardingAndDescentUse() {
        Long unused = route.getStagingPoints().get(0).getId();
        Long boarding = route.getStagingPoints().get(2).getId();
        Long descent = route.getStagingPoints().get(3).getId();

        assertThat(reservationRepository.existsByBoardingPointIdOrDescentPointId(boarding, boarding)).isTrue();
        assertThat(reservationRepository.existsByBoardingPointIdOrDescentPointId(descent, descent)).isTrue();
        assertThat(reservationRepository.existsByBoardingPointIdOrDescentPointId(unused, unused)).isFalse();
    }

    @Test
    void existsByPassengerIdAndBoardingPointRouteIdAndStatus_matchesPassengerRouteAndStatus() {
        assertThat(reservationRepository.existsByPassengerIdAndBoardingPointRouteIdAndStatus(
                salma.getId(), route.getId(), ReservationStatus.CONFIRMED)).isTrue();
        assertThat(reservationRepository.existsByPassengerIdAndBoardingPointRouteIdAndStatus(
                karim.getId(), route.getId(), ReservationStatus.CONFIRMED)).isFalse();
        assertThat(reservationRepository.existsByPassengerIdAndBoardingPointRouteIdAndStatus(
                salma.getId(), emptyRoute.getId(), ReservationStatus.CONFIRMED)).isFalse();
    }

    @Test
    void findByBoardingPointRouteIdAndStatus_returnsOnlyMatchingStatus() {
        List<Reservation> confirmed = reservationRepository
                .findByBoardingPointRouteIdAndStatus(route.getId(), ReservationStatus.CONFIRMED);

        assertThat(confirmed).extracting(Reservation::getId).containsExactly(salmaReservation.getId());
    }

    @Test
    void findByPassengerIdOrderByReservationDateDesc_returnsPassengerHistory() {
        Reservation later = db.reservation(salma, emptyRoute, 0, 3, 1, ReservationStatus.CONFIRMED, LocalDateTime.now());
        db.flushAndClear();

        List<Reservation> history = reservationRepository.findByPassengerIdOrderByReservationDateDesc(salma.getId());

        assertThat(history).extracting(Reservation::getId).containsExactly(later.getId(), salmaReservation.getId());
    }

    @Test
    void findByBoardingPointRouteIdOrderByReservationDateDesc_returnsRouteReservationsNewestFirst() {
        List<Reservation> reservations = reservationRepository
                .findByBoardingPointRouteIdOrderByReservationDateDesc(route.getId());

        assertThat(reservations).extracting(Reservation::getId)
                .containsExactly(karimReservation.getId(), salmaReservation.getId());
        assertThat(reservations.get(0).getPassenger().getId()).isEqualTo(karim.getId());
    }

    @Test
    void sumSeatsByRouteIdAndStatus_sumsOnlyRequestedStatus() {
        Passenger imane = db.passenger();
        db.reservation(imane, route, 0, 3, 1, ReservationStatus.CONFIRMED, LocalDateTime.now());
        db.flushAndClear();

        assertThat(reservationRepository.sumSeatsByRouteIdAndStatus(route.getId(), ReservationStatus.CONFIRMED)).isEqualTo(3L);
        assertThat(reservationRepository.sumSeatsByRouteIdAndStatus(route.getId(), ReservationStatus.CANCELED)).isEqualTo(1L);
    }

    @Test
    void sumSeatsByRouteIdAndStatus_returnsZeroWhenNoReservation() {
        assertThat(reservationRepository.sumSeatsByRouteIdAndStatus(emptyRoute.getId(), ReservationStatus.CONFIRMED)).isZero();
    }
}

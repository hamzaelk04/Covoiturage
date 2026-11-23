package org.covoiturage.mapper;

import org.covoiturage.dto.reservation.ReservationResponse;
import org.covoiturage.entity.Passenger;
import org.covoiturage.entity.Reservation;
import org.covoiturage.entity.Route;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.testutil.TestData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationMapperTest {
    private final ReservationMapper mapper = new ReservationMapperImpl();

    @Test
    void toResponse_flattensPassengerRouteAndPoints() {
        UUID passengerId = UUID.randomUUID();
        Passenger passenger = TestData.passenger(passengerId);
        Route route = TestData.route(7L, TestData.vehicle(1L, TestData.driver(UUID.randomUUID()), 4),
                3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        Reservation reservation = TestData.reservation(100L, passenger, route, 2, 3, 2, ReservationStatus.CONFIRMED);

        ReservationResponse response = mapper.toResponse(reservation);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getPassengerId()).isEqualTo(passengerId);
        assertThat(response.getRouteId()).isEqualTo(7L);
        assertThat(response.getBoardingPointId()).isEqualTo(3L);
        assertThat(response.getBoardingCity()).isEqualTo("Settat");
        assertThat(response.getDescentPointId()).isEqualTo(4L);
        assertThat(response.getDescentCity()).isEqualTo("Marrakech");
        assertThat(response.getNumberOfSeats()).isEqualTo(2);
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(response.getReservationDate()).isEqualTo(reservation.getReservationDate());
        assertThat(response.getRemainingPlaces()).isNull();
    }

    @Test
    void toResponses_mapsEveryReservation() {
        Passenger passenger = TestData.passenger(UUID.randomUUID());
        Route route = TestData.route(7L, TestData.vehicle(1L, TestData.driver(UUID.randomUUID()), 4),
                3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        Reservation first = TestData.reservation(1L, passenger, route, 0, 1, 1, ReservationStatus.CONFIRMED);
        Reservation second = TestData.reservation(2L, passenger, route, 1, 2, 1, ReservationStatus.CANCELED);

        List<ReservationResponse> responses = mapper.toResponses(List.of(first, second));

        assertThat(responses).extracting(ReservationResponse::getId).containsExactly(1L, 2L);
        assertThat(responses).extracting(ReservationResponse::getStatus)
                .containsExactly(ReservationStatus.CONFIRMED, ReservationStatus.CANCELED);
    }
}

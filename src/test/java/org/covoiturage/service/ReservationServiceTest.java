package org.covoiturage.service;

import org.covoiturage.dto.reservation.ReservationCreateRequest;
import org.covoiturage.dto.reservation.ReservationResponse;
import org.covoiturage.entity.Driver;
import org.covoiturage.entity.Passenger;
import org.covoiturage.entity.Reservation;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.mapper.ReservationMapper;
import org.covoiturage.repository.PassengerRepository;
import org.covoiturage.repository.ReservationRepository;
import org.covoiturage.repository.RouteRepository;
import org.covoiturage.testutil.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.covoiturage.testutil.ApiAssert.assertFails;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    private static final long ROUTE_ID = 1L;
    private static final long RESERVATION_ID = 50L;

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private RouteService routeService;
    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationService reservationService;

    private UUID passengerId;
    private UUID driverId;
    private Passenger passenger;
    private Route route;

    @BeforeEach
    void setUp() {
        passengerId = UUID.randomUUID();
        driverId = UUID.randomUUID();
        passenger = TestData.passenger(passengerId);
        Driver driver = TestData.driver(driverId);
        Vehicle vehicle = TestData.vehicle(1L, driver, 4);
        route = TestData.route(ROUTE_ID, vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(3));
    }

    @Test
    void create_confirmsReservationAndComputesAmount() {
        givenPassengerAndRoute();
        givenNoPreviousReservationAndBookedSeats(0L);
        givenSaveAndMapping();

        ReservationResponse response = reservationService.create(passengerId, bookingRequest(3L, 4L, 1));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(saved.getPassenger()).isSameAs(passenger);
        assertThat(saved.getBoardingPoint().getCity()).isEqualTo("Settat");
        assertThat(saved.getDescentPoint().getCity()).isEqualTo("Marrakech");
        assertThat(saved.getNumberOfSeats()).isEqualTo(1);
        assertThat(saved.getTotalAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(saved.getReservationDate()).isNotNull();
        assertThat(response.getRemainingPlaces()).isEqualTo(2);
        assertThat(route.getStatus()).isEqualTo(RouteStatus.PUBLISHED);
    }

    @Test
    void create_multipliesAmountBySeatsAndMarksRouteFullWhenNoSeatLeft() {
        route.setAvailablePlaces(2);
        givenPassengerAndRoute();
        givenNoPreviousReservationAndBookedSeats(0L);
        givenSaveAndMapping();

        ReservationResponse response = reservationService.create(passengerId, bookingRequest(2L, 4L, 2));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalAmount()).isEqualTo(new BigDecimal("160.00"));
        assertThat(response.getRemainingPlaces()).isZero();
        assertThat(route.getStatus()).isEqualTo(RouteStatus.FULL);
    }

    @Test
    void create_countsAlreadyBookedSeats() {
        givenPassengerAndRoute();
        givenNoPreviousReservationAndBookedSeats(1L);
        givenSaveAndMapping();

        ReservationResponse response = reservationService.create(passengerId, bookingRequest(1L, 4L, 2));

        assertThat(response.getRemainingPlaces()).isZero();
        assertThat(route.getStatus()).isEqualTo(RouteStatus.FULL);
    }

    @Test
    void create_roundsAmountHalfUp() {
        route.getStagingPoints().get(3).setPrice(new BigDecimal("120.005"));
        givenPassengerAndRoute();
        givenNoPreviousReservationAndBookedSeats(0L);
        givenSaveAndMapping();

        reservationService.create(passengerId, bookingRequest(3L, 4L, 1));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalAmount()).isEqualTo(new BigDecimal("50.01"));
    }

    @Test
    void create_failsWhenPassengerNotFound() {
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.empty());

        assertFails(() -> reservationService.create(passengerId, bookingRequest(3L, 4L, 1)),
                HttpStatus.NOT_FOUND, "Passager introuvable");

        verifyNoInteractions(routeRepository);
    }

    @Test
    void create_failsWhenRouteNotFound() {
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(passenger));
        when(routeRepository.findByIdForUpdate(ROUTE_ID)).thenReturn(Optional.empty());

        assertFails(() -> reservationService.create(passengerId, bookingRequest(3L, 4L, 1)),
                HttpStatus.NOT_FOUND, "Trajet introuvable");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void create_refusesFullRoute() {
        route.setStatus(RouteStatus.FULL);
        givenPassengerAndRoute();

        assertFails(() -> reservationService.create(passengerId, bookingRequest(3L, 4L, 1)),
                HttpStatus.CONFLICT, "Le trajet est complet.");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @ParameterizedTest
    @EnumSource(value = RouteStatus.class, names = {"COMPLETED", "CANCELED", "OPEN"})
    void create_refusesRouteNotPublished(RouteStatus status) {
        route.setStatus(status);
        givenPassengerAndRoute();

        assertFails(() -> reservationService.create(passengerId, bookingRequest(3L, 4L, 1)),
                HttpStatus.CONFLICT, "pas ouvert à la réservation");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void create_refusesBoardingPointOutsideRoute() {
        givenPassengerAndRoute();

        assertFails(() -> reservationService.create(passengerId, bookingRequest(99L, 4L, 1)),
                HttpStatus.BAD_REQUEST, "montée");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void create_refusesDescentPointOutsideRoute() {
        givenPassengerAndRoute();

        assertFails(() -> reservationService.create(passengerId, bookingRequest(3L, 99L, 1)),
                HttpStatus.BAD_REQUEST, "descente");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @ParameterizedTest
    @CsvSource({"4,3", "3,3"})
    void create_refusesBoardingNotBeforeDescent(long boardingId, long descentId) {
        givenPassengerAndRoute();

        assertFails(() -> reservationService.create(passengerId, bookingRequest(boardingId, descentId, 1)),
                HttpStatus.BAD_REQUEST, "avant le point de descente");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void create_refusesSecondConfirmedReservationOnSameRoute() {
        givenPassengerAndRoute();
        when(reservationRepository.existsByPassengerIdAndBoardingPointRouteIdAndStatus(
                passengerId, ROUTE_ID, ReservationStatus.CONFIRMED)).thenReturn(true);

        assertFails(() -> reservationService.create(passengerId, bookingRequest(3L, 4L, 1)),
                HttpStatus.CONFLICT, "déjà une réservation confirmée");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void create_refusesWhenNotEnoughPlaces() {
        givenPassengerAndRoute();
        givenNoPreviousReservationAndBookedSeats(2L);

        assertFails(() -> reservationService.create(passengerId, bookingRequest(3L, 4L, 2)),
                HttpStatus.CONFLICT, "Il ne reste que 1 place(s)");

        verify(reservationRepository, never()).save(any(Reservation.class));
        assertThat(route.getStatus()).isEqualTo(RouteStatus.PUBLISHED);
    }

    @Test
    void cancel_cancelsReservation() {
        Reservation reservation = confirmedReservation();
        givenReservationAndRoute(reservation);

        reservationService.cancel(passengerId, RESERVATION_ID);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(route.getStatus()).isEqualTo(RouteStatus.PUBLISHED);
    }

    @Test
    void cancel_reopensFullRoute() {
        route.setStatus(RouteStatus.FULL);
        Reservation reservation = confirmedReservation();
        givenReservationAndRoute(reservation);

        reservationService.cancel(passengerId, RESERVATION_ID);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(route.getStatus()).isEqualTo(RouteStatus.PUBLISHED);
    }

    @Test
    void cancel_acceptsMoreThan24HoursBeforeDeparture() {
        route.setStartDate(LocalDateTime.now().plusHours(25));
        Reservation reservation = confirmedReservation();
        givenReservationAndRoute(reservation);

        reservationService.cancel(passengerId, RESERVATION_ID);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
    }

    @Test
    void cancel_refusesLessThan24HoursBeforeDeparture() {
        route.setStartDate(LocalDateTime.now().plusHours(23));
        Reservation reservation = confirmedReservation();
        givenReservationAndRoute(reservation);

        assertFails(() -> reservationService.cancel(passengerId, RESERVATION_ID),
                HttpStatus.CONFLICT, "délai d'annulation");

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void cancel_failsWhenReservationNotFound() {
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        assertFails(() -> reservationService.cancel(passengerId, RESERVATION_ID),
                HttpStatus.NOT_FOUND, "Réservation introuvable");
    }

    @Test
    void cancel_refusesAnotherPassengersReservation() {
        Passenger other = TestData.passenger(UUID.randomUUID());
        Reservation reservation = TestData.reservation(RESERVATION_ID, other, route, 2, 3, 1, ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertFails(() -> reservationService.cancel(passengerId, RESERVATION_ID),
                HttpStatus.FORBIDDEN, "autre passager");

        verifyNoInteractions(routeRepository);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void cancel_refusesReservationAlreadyCanceled() {
        Reservation reservation = TestData.reservation(RESERVATION_ID, passenger, route, 2, 3, 1, ReservationStatus.CANCELED);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertFails(() -> reservationService.cancel(passengerId, RESERVATION_ID),
                HttpStatus.CONFLICT, "ne peut pas être annulée");

        verifyNoInteractions(routeRepository);
    }

    @Test
    void getById_returnsMappedReservation() {
        Reservation reservation = confirmedReservation();
        ReservationResponse expected = ReservationResponse.builder().id(RESERVATION_ID).build();
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(reservationMapper.toResponse(reservation)).thenReturn(expected);

        assertThat(reservationService.getById(passengerId, RESERVATION_ID)).isSameAs(expected);
    }

    @Test
    void getById_refusesAnotherPassengersReservation() {
        Reservation reservation = TestData.reservation(RESERVATION_ID, TestData.passenger(UUID.randomUUID()),
                route, 2, 3, 1, ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertFails(() -> reservationService.getById(passengerId, RESERVATION_ID), HttpStatus.FORBIDDEN, "autre passager");

        verifyNoInteractions(reservationMapper);
    }

    @Test
    void getByPassenger_returnsMappedHistory() {
        Reservation reservation = confirmedReservation();
        List<ReservationResponse> expected = List.of(ReservationResponse.builder().id(RESERVATION_ID).build());
        when(passengerRepository.existsById(passengerId)).thenReturn(true);
        when(reservationRepository.findByPassengerIdOrderByReservationDateDesc(passengerId))
                .thenReturn(List.of(reservation));
        when(reservationMapper.toResponses(List.of(reservation))).thenReturn(expected);

        assertThat(reservationService.getByPassenger(passengerId)).isSameAs(expected);
    }

    @Test
    void getByPassenger_failsWhenPassengerNotFound() {
        when(passengerRepository.existsById(passengerId)).thenReturn(false);

        assertFails(() -> reservationService.getByPassenger(passengerId), HttpStatus.NOT_FOUND, "Passager introuvable");

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void getByRoute_checksOwnershipThenReturnsReservations() {
        Reservation reservation = confirmedReservation();
        List<ReservationResponse> expected = List.of(ReservationResponse.builder().id(RESERVATION_ID).build());
        when(reservationRepository.findByBoardingPointRouteIdOrderByReservationDateDesc(ROUTE_ID))
                .thenReturn(List.of(reservation));
        when(reservationMapper.toResponses(List.of(reservation))).thenReturn(expected);

        assertThat(reservationService.getByRoute(driverId, ROUTE_ID)).isSameAs(expected);

        verify(routeService).findOwnedRoute(driverId, ROUTE_ID);
    }

    @Test
    void getByRoute_refusesAnotherDriversRoute() {
        when(routeService.findOwnedRoute(driverId, ROUTE_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce trajet appartient à un autre conducteur."));

        assertFails(() -> reservationService.getByRoute(driverId, ROUTE_ID), HttpStatus.FORBIDDEN, "autre conducteur");

        verifyNoInteractions(reservationRepository);
    }

    private Reservation confirmedReservation() {
        return TestData.reservation(RESERVATION_ID, passenger, route, 2, 3, 1, ReservationStatus.CONFIRMED);
    }

    private void givenPassengerAndRoute() {
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(passenger));
        when(routeRepository.findByIdForUpdate(ROUTE_ID)).thenReturn(Optional.of(route));
    }

    private void givenNoPreviousReservationAndBookedSeats(long booked) {
        when(reservationRepository.existsByPassengerIdAndBoardingPointRouteIdAndStatus(
                passengerId, ROUTE_ID, ReservationStatus.CONFIRMED)).thenReturn(false);
        when(reservationRepository.sumSeatsByRouteIdAndStatus(ROUTE_ID, ReservationStatus.CONFIRMED)).thenReturn(booked);
    }

    private void givenSaveAndMapping() {
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation reservation = inv.getArgument(0);
            reservation.setId(100L);
            return reservation;
        });
        when(reservationMapper.toResponse(any(Reservation.class)))
                .thenReturn(ReservationResponse.builder().id(100L).build());
    }

    private void givenReservationAndRoute(Reservation reservation) {
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(routeRepository.findByIdForUpdate(ROUTE_ID)).thenReturn(Optional.of(route));
    }

    private ReservationCreateRequest bookingRequest(long boardingId, long descentId, int seats) {
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setRouteId(ROUTE_ID);
        request.setBoardingPointId(boardingId);
        request.setDescentPointId(descentId);
        request.setNumberOfSeats(seats);
        return request;
    }
}

package org.covoiturage.service;

import org.covoiturage.dto.route.RouteCreateRequest;
import org.covoiturage.dto.route.RouteResponse;
import org.covoiturage.dto.route.RouteSearchCriteria;
import org.covoiturage.dto.route.RouteUpdateRequest;
import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.covoiturage.entity.Driver;
import org.covoiturage.entity.Passenger;
import org.covoiturage.entity.Reservation;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.StagingPoint;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.mapper.RouteMapper;
import org.covoiturage.mapper.StagingPointMapper;
import org.covoiturage.repository.DriverRepository;
import org.covoiturage.repository.ReservationRepository;
import org.covoiturage.repository.RouteRepository;
import org.covoiturage.repository.VehicleRepository;
import org.covoiturage.service.validator.StagingPointValidator;
import org.covoiturage.testutil.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.covoiturage.testutil.ApiAssert.assertFails;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {
    private static final long ROUTE_ID = 10L;

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private RouteMapper routeMapper;
    @Mock
    private StagingPointMapper stagingPointMapper;
    @Mock
    private StagingPointValidator stagingPointValidator;

    @InjectMocks
    private RouteService routeService;

    private UUID driverId;
    private Driver driver;
    private Vehicle vehicle;
    private LocalDateTime start;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();
        driver = TestData.driver(driverId);
        vehicle = TestData.vehicle(1L, driver, 4);
        start = LocalDateTime.now().plusDays(5);
    }

    @Test
    void create_publishesRouteAndLinksPoints() {
        RouteCreateRequest request = TestData.routeCreateRequest(1L, 3, start);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(stagingPointValidator.validateAndSort(anyList(), any(LocalDateTime.class)))
                .thenReturn(request.getStagingPoints());
        when(stagingPointMapper.toEntity(any(StagingPointRequest.class)))
                .thenAnswer(inv -> toPoint(inv.getArgument(0)));
        when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));
        when(routeMapper.toResponse(any(Route.class))).thenReturn(RouteResponse.builder().id(1L).build());

        RouteResponse response = routeService.create(driverId, request);

        ArgumentCaptor<Route> captor = ArgumentCaptor.forClass(Route.class);
        verify(routeRepository).save(captor.capture());
        Route saved = captor.getValue();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(saved.getStartCity()).isEqualTo("Rabat");
        assertThat(saved.getArrivalCity()).isEqualTo("Marrakech");
        assertThat(saved.getStartDate()).isEqualTo(start);
        assertThat(saved.getAvailablePlaces()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(RouteStatus.PUBLISHED);
        assertThat(saved.getVehicle()).isSameAs(vehicle);
        assertThat(saved.getStagingPoints()).hasSize(4);
        assertThat(saved.getStagingPoints()).allSatisfy(p -> assertThat(p.getRoute()).isSameAs(saved));
    }

    @Test
    void create_failsWhenVehicleNotFound() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.empty());

        assertFails(() -> routeService.create(driverId, TestData.routeCreateRequest(1L, 3, start)),
                HttpStatus.NOT_FOUND, "Véhicule introuvable");

        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    void create_failsWhenVehicleBelongsToAnotherDriver() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        assertFails(() -> routeService.create(UUID.randomUUID(), TestData.routeCreateRequest(1L, 3, start)),
                HttpStatus.FORBIDDEN, "autre conducteur");

        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    void create_failsWhenPlacesExceedCapacity() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        assertFails(() -> routeService.create(driverId, TestData.routeCreateRequest(1L, 5, start)),
                HttpStatus.BAD_REQUEST, "capacité de 4");

        verifyNoInteractions(stagingPointValidator);
        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    void create_propagatesValidatorError() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(stagingPointValidator.validateAndSort(anyList(), any(LocalDateTime.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Un trajet doit contenir au moins deux points d'arrêt."));

        assertFails(() -> routeService.create(driverId, TestData.routeCreateRequest(1L, 3, start)),
                HttpStatus.BAD_REQUEST, "deux points");

        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    void update_changesPlaces() {
        Route route = route(RouteStatus.PUBLISHED, 3);
        givenRoute(route);
        when(reservationRepository.existsByBoardingPointRouteIdAndStatus(ROUTE_ID, ReservationStatus.CONFIRMED))
                .thenReturn(false);
        when(routeMapper.toResponse(route)).thenReturn(RouteResponse.builder().id(ROUTE_ID).build());

        RouteResponse response = routeService.update(driverId, ROUTE_ID, updateRequest(null, 2));

        assertThat(response.getId()).isEqualTo(ROUTE_ID);
        assertThat(route.getAvailablePlaces()).isEqualTo(2);
        assertThat(route.getStatus()).isEqualTo(RouteStatus.PUBLISHED);
    }

    @Test
    void update_changesVehicleWhenNoBookings() {
        Vehicle other = TestData.vehicle(2L, driver, 5);
        Route route = route(RouteStatus.PUBLISHED, 3);
        givenRoute(route);
        when(reservationRepository.existsByBoardingPointRouteIdAndStatus(ROUTE_ID, ReservationStatus.CONFIRMED))
                .thenReturn(false);
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(other));
        when(routeMapper.toResponse(route)).thenReturn(RouteResponse.builder().build());

        routeService.update(driverId, ROUTE_ID, updateRequest(2L, null));

        assertThat(route.getVehicle()).isSameAs(other);
    }

    @Test
    void update_ignoresVehicleWhenItIsTheSame() {
        Route route = route(RouteStatus.PUBLISHED, 3);
        givenRoute(route);
        when(reservationRepository.existsByBoardingPointRouteIdAndStatus(ROUTE_ID, ReservationStatus.CONFIRMED))
                .thenReturn(true);
        when(routeMapper.toResponse(route)).thenReturn(RouteResponse.builder().build());

        routeService.update(driverId, ROUTE_ID, updateRequest(1L, null));

        verifyNoInteractions(vehicleRepository);
        assertThat(route.getVehicle()).isSameAs(vehicle);
    }

    @Test
    void update_refusesVehicleOfAnotherDriver() {
        Vehicle foreign = TestData.vehicle(2L, TestData.driver(UUID.randomUUID()), 5);
        givenRoute(route(RouteStatus.PUBLISHED, 3));
        when(reservationRepository.existsByBoardingPointRouteIdAndStatus(ROUTE_ID, ReservationStatus.CONFIRMED))
                .thenReturn(false);
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(foreign));

        assertFails(() -> routeService.update(driverId, ROUTE_ID, updateRequest(2L, null)),
                HttpStatus.FORBIDDEN, "autre conducteur");
    }

    @Test
    void update_refusesVehicleChangeWhenBookingsExist() {
        givenRoute(route(RouteStatus.PUBLISHED, 3));
        when(reservationRepository.existsByBoardingPointRouteIdAndStatus(ROUTE_ID, ReservationStatus.CONFIRMED))
                .thenReturn(true);

        assertFails(() -> routeService.update(driverId, ROUTE_ID, updateRequest(2L, null)),
                HttpStatus.CONFLICT, "véhicule ne peut plus être changé");

        verifyNoInteractions(vehicleRepository);
    }

    @Test
    void update_refusesPlacesDecreaseWhenBookingsExist() {
        Route route = route(RouteStatus.PUBLISHED, 3);
        givenRoute(route);
        when(reservationRepository.existsByBoardingPointRouteIdAndStatus(ROUTE_ID, ReservationStatus.CONFIRMED))
                .thenReturn(true);

        assertFails(() -> routeService.update(driverId, ROUTE_ID, updateRequest(null, 2)),
                HttpStatus.CONFLICT, "ne peuvent pas être réduites");

        assertThat(route.getAvailablePlaces()).isEqualTo(3);
    }

    @Test
    void update_refusesPlacesAboveCapacity() {
        givenRoute(route(RouteStatus.PUBLISHED, 3));
        when(reservationRepository.existsByBoardingPointRouteIdAndStatus(ROUTE_ID, ReservationStatus.CONFIRMED))
                .thenReturn(false);

        assertFails(() -> routeService.update(driverId, ROUTE_ID, updateRequest(null, 6)),
                HttpStatus.BAD_REQUEST, "capacité de 4");
    }

    @Test
    void update_reopensFullRouteWhenPlacesIncrease() {
        Route route = route(RouteStatus.FULL, 3);
        givenRoute(route);
        when(reservationRepository.existsByBoardingPointRouteIdAndStatus(ROUTE_ID, ReservationStatus.CONFIRMED))
                .thenReturn(true);
        when(routeMapper.toResponse(route)).thenReturn(RouteResponse.builder().build());

        routeService.update(driverId, ROUTE_ID, updateRequest(null, 4));

        assertThat(route.getAvailablePlaces()).isEqualTo(4);
        assertThat(route.getStatus()).isEqualTo(RouteStatus.PUBLISHED);
    }

    @ParameterizedTest
    @EnumSource(value = RouteStatus.class, names = {"COMPLETED", "CANCELED", "OPEN"})
    void update_refusesNonEditableStatus(RouteStatus status) {
        givenRoute(route(status, 3));

        assertFails(() -> routeService.update(driverId, ROUTE_ID, updateRequest(null, 2)),
                HttpStatus.CONFLICT, "ne peut plus être modifié");

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void update_refusesAnotherDriversRoute() {
        givenRoute(route(RouteStatus.PUBLISHED, 3));

        assertFails(() -> routeService.update(UUID.randomUUID(), ROUTE_ID, updateRequest(null, 2)),
                HttpStatus.FORBIDDEN, "autre conducteur");
    }

    @Test
    void update_failsWhenRouteNotFound() {
        when(routeRepository.findDetailById(ROUTE_ID)).thenReturn(Optional.empty());

        assertFails(() -> routeService.update(driverId, ROUTE_ID, updateRequest(null, 2)),
                HttpStatus.NOT_FOUND, "Trajet introuvable");
    }

    @Test
    void delete_removesRouteWithoutReservations() {
        Route route = route(RouteStatus.PUBLISHED, 3);
        givenRoute(route);
        when(reservationRepository.existsByBoardingPointRouteId(ROUTE_ID)).thenReturn(false);

        routeService.delete(driverId, ROUTE_ID);

        verify(routeRepository).delete(route);
    }

    @Test
    void delete_refusesRouteWithReservations() {
        givenRoute(route(RouteStatus.PUBLISHED, 3));
        when(reservationRepository.existsByBoardingPointRouteId(ROUTE_ID)).thenReturn(true);

        assertFails(() -> routeService.delete(driverId, ROUTE_ID), HttpStatus.CONFLICT, "réservations");

        verify(routeRepository, never()).delete(any(Route.class));
    }

    @Test
    void delete_refusesAnotherDriversRoute() {
        givenRoute(route(RouteStatus.PUBLISHED, 3));

        assertFails(() -> routeService.delete(UUID.randomUUID(), ROUTE_ID), HttpStatus.FORBIDDEN, "autre conducteur");

        verify(routeRepository, never()).delete(any(Route.class));
    }

    @ParameterizedTest
    @EnumSource(value = RouteStatus.class, names = {"PUBLISHED", "FULL"})
    void complete_marksRouteCompletedAfterDeparture(RouteStatus status) {
        Route route = TestData.route(ROUTE_ID, vehicle, 3, status, LocalDateTime.now().minusHours(1));
        givenRoute(route);

        routeService.complete(driverId, ROUTE_ID);

        assertThat(route.getStatus()).isEqualTo(RouteStatus.COMPLETED);
    }

    @Test
    void complete_refusesBeforeDeparture() {
        Route route = route(RouteStatus.PUBLISHED, 3);
        givenRoute(route);

        assertFails(() -> routeService.complete(driverId, ROUTE_ID), HttpStatus.CONFLICT, "après son heure de départ");

        assertThat(route.getStatus()).isEqualTo(RouteStatus.PUBLISHED);
    }

    @ParameterizedTest
    @EnumSource(value = RouteStatus.class, names = {"COMPLETED", "CANCELED", "OPEN"})
    void complete_refusesNonOpenStatus(RouteStatus status) {
        givenRoute(TestData.route(ROUTE_ID, vehicle, 3, status, LocalDateTime.now().minusHours(1)));

        assertFails(() -> routeService.complete(driverId, ROUTE_ID), HttpStatus.CONFLICT, "terminé");
    }

    @Test
    void cancel_cancelsRouteAndConfirmedReservations() {
        Route route = route(RouteStatus.PUBLISHED, 3);
        Passenger passenger = TestData.passenger(UUID.randomUUID());
        Reservation first = TestData.reservation(1L, passenger, route, 0, 3, 1, ReservationStatus.CONFIRMED);
        Reservation second = TestData.reservation(2L, passenger, route, 1, 2, 1, ReservationStatus.CONFIRMED);
        givenRoute(route);
        when(reservationRepository.findByBoardingPointRouteIdAndStatus(ROUTE_ID, ReservationStatus.CONFIRMED))
                .thenReturn(List.of(first, second));

        routeService.cancel(driverId, ROUTE_ID);

        assertThat(route.getStatus()).isEqualTo(RouteStatus.CANCELED);
        assertThat(first.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(second.getStatus()).isEqualTo(ReservationStatus.CANCELED);
    }

    @Test
    void cancel_worksWithoutReservations() {
        Route route = route(RouteStatus.FULL, 3);
        givenRoute(route);
        when(reservationRepository.findByBoardingPointRouteIdAndStatus(ROUTE_ID, ReservationStatus.CONFIRMED))
                .thenReturn(List.of());

        routeService.cancel(driverId, ROUTE_ID);

        assertThat(route.getStatus()).isEqualTo(RouteStatus.CANCELED);
    }

    @ParameterizedTest
    @EnumSource(value = RouteStatus.class, names = {"COMPLETED", "CANCELED", "OPEN"})
    void cancel_refusesNonOpenStatus(RouteStatus status) {
        givenRoute(route(status, 3));

        assertFails(() -> routeService.cancel(driverId, ROUTE_ID), HttpStatus.CONFLICT, "annulé");

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void getById_returnsMappedRoute() {
        Route route = route(RouteStatus.PUBLISHED, 3);
        RouteResponse expected = RouteResponse.builder().id(ROUTE_ID).build();
        givenRoute(route);
        when(routeMapper.toResponse(route)).thenReturn(expected);

        assertThat(routeService.getById(ROUTE_ID)).isSameAs(expected);
    }

    @Test
    void getById_failsWhenNotFound() {
        when(routeRepository.findDetailById(99L)).thenReturn(Optional.empty());

        assertFails(() -> routeService.getById(99L), HttpStatus.NOT_FOUND, "Trajet introuvable");
    }

    @Test
    void getByDriver_returnsMappedRoutes() {
        Route route = route(RouteStatus.PUBLISHED, 3);
        List<RouteResponse> expected = List.of(RouteResponse.builder().id(ROUTE_ID).build());
        when(driverRepository.existsById(driverId)).thenReturn(true);
        when(routeRepository.findByVehicleDriverIdOrderByStartDateDesc(driverId)).thenReturn(List.of(route));
        when(routeMapper.toResponses(List.of(route))).thenReturn(expected);

        assertThat(routeService.getByDriver(driverId)).isSameAs(expected);
    }

    @Test
    void getByDriver_failsWhenDriverNotFound() {
        when(driverRepository.existsById(driverId)).thenReturn(false);

        assertFails(() -> routeService.getByDriver(driverId), HttpStatus.NOT_FOUND, "Conducteur introuvable");

        verifyNoInteractions(routeRepository);
    }

    @Test
    void search_clampsPageAndSizeAndSortsByStartDate() {
        when(routeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<Route>(List.of()));

        routeService.search(new RouteSearchCriteria(), -3, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(routeRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().getOrderFor("startDate")).isNotNull();
        assertThat(captor.getValue().getSort().getOrderFor("startDate").isAscending()).isTrue();
    }

    @Test
    void search_usesMinimumSizeOfOne() {
        when(routeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<Route>(List.of()));

        routeService.search(new RouteSearchCriteria(), 2, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(routeRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void search_mapsEachResult() {
        Route route = route(RouteStatus.PUBLISHED, 3);
        RouteResponse expected = RouteResponse.builder().id(ROUTE_ID).build();
        when(routeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(route)));
        when(routeMapper.toResponse(route)).thenReturn(expected);

        Page<RouteResponse> result = routeService.search(new RouteSearchCriteria(), 0, 10);

        assertThat(result.getContent()).containsExactly(expected);
    }

    private Route route(RouteStatus status, int places) {
        return TestData.route(ROUTE_ID, vehicle, places, status, start);
    }

    private void givenRoute(Route route) {
        when(routeRepository.findDetailById(ROUTE_ID)).thenReturn(Optional.of(route));
    }

    private RouteUpdateRequest updateRequest(Long vehicleId, Integer places) {
        RouteUpdateRequest request = new RouteUpdateRequest();
        request.setVehicleId(vehicleId);
        request.setAvailablePlaces(places);
        return request;
    }

    private static StagingPoint toPoint(StagingPointRequest request) {
        return StagingPoint.builder()
                .city(request.getCity())
                .stopOrder(request.getStopOrder())
                .passingTime(request.getPassingTime())
                .price(request.getPrice())
                .build();
    }
}

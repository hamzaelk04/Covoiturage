package org.covoiturage.service;

import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.covoiturage.dto.stagingPoint.StagingPointResponse;
import org.covoiturage.dto.stagingPoint.StagingPointUpdateRequest;
import org.covoiturage.entity.Driver;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.StagingPoint;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.mapper.StagingPointMapper;
import org.covoiturage.repository.ReservationRepository;
import org.covoiturage.repository.RouteRepository;
import org.covoiturage.repository.StagingPointRepository;
import org.covoiturage.service.validator.StagingPointValidator;
import org.covoiturage.testutil.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
class StagingPointServiceTest {
    private static final long ROUTE_ID = 1L;

    @Mock
    private StagingPointRepository stagingPointRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private StagingPointMapper stagingPointMapper;
    @Mock
    private StagingPointValidator stagingPointValidator;
    @Mock
    private RouteService routeService;

    @Captor
    private ArgumentCaptor<List<StagingPointRequest>> candidatesCaptor;
    @Captor
    private ArgumentCaptor<StagingPointUpdateRequest> requestCaptor;

    @InjectMocks
    private StagingPointService stagingPointService;

    private UUID driverId;
    private Route route;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();
        Driver driver = TestData.driver(driverId);
        Vehicle vehicle = TestData.vehicle(1L, driver, 4);
        route = TestData.route(ROUTE_ID, vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(5));
    }

    @Test
    void findByRoute_returnsMappedPoints() {
        List<StagingPointResponse> expected = List.of(StagingPointResponse.builder().id(1L).build());
        List<StagingPoint> points = route.getStagingPoints();
        when(routeRepository.existsById(ROUTE_ID)).thenReturn(true);
        when(stagingPointRepository.findByRouteIdOrderByStopOrderAsc(ROUTE_ID)).thenReturn(points);
        when(stagingPointMapper.toResponses(points)).thenReturn(expected);

        assertThat(stagingPointService.findByRoute(ROUTE_ID)).isSameAs(expected);
    }

    @Test
    void findByRoute_failsWhenRouteNotFound() {
        when(routeRepository.existsById(ROUTE_ID)).thenReturn(false);

        assertFails(() -> stagingPointService.findByRoute(ROUTE_ID), HttpStatus.NOT_FOUND, "Trajet introuvable");

        verifyNoInteractions(stagingPointRepository);
    }

    @Test
    void findById_returnsMappedPoint() {
        StagingPoint point = route.getStagingPoints().get(1);
        StagingPointResponse expected = StagingPointResponse.builder().id(2L).build();
        when(stagingPointRepository.findByIdAndRouteId(2L, ROUTE_ID)).thenReturn(Optional.of(point));
        when(stagingPointMapper.toResponse(point)).thenReturn(expected);

        assertThat(stagingPointService.findById(ROUTE_ID, 2L)).isSameAs(expected);
    }

    @Test
    void findById_failsWhenPointNotOnRoute() {
        when(stagingPointRepository.findByIdAndRouteId(99L, ROUTE_ID)).thenReturn(Optional.empty());

        assertFails(() -> stagingPointService.findById(ROUTE_ID, 99L), HttpStatus.NOT_FOUND, "Point d'arrêt introuvable");
    }

    @Test
    void update_appliesChangesAndRevalidatesWholeRoute() {
        StagingPointResponse expected = StagingPointResponse.builder().id(2L).build();
        StagingPointUpdateRequest request = new StagingPointUpdateRequest();
        request.setPrice(new BigDecimal("45.00"));
        givenOwnedRoute();
        stubUpdateEntityApplyingRequest();
        stubToRequest();
        when(stagingPointMapper.toResponse(any(StagingPoint.class))).thenReturn(expected);

        StagingPointResponse response = stagingPointService.update(driverId, ROUTE_ID, 2L, request);

        assertThat(response).isSameAs(expected);
        assertThat(route.getStagingPoints().get(1).getPrice()).isEqualTo(new BigDecimal("45.00"));
        verify(stagingPointValidator).validateAndSort(candidatesCaptor.capture(), any(LocalDateTime.class));
        assertThat(candidatesCaptor.getValue()).hasSize(4);
        assertThat(candidatesCaptor.getValue()).extracting(StagingPointRequest::getPrice)
                .contains(new BigDecimal("45.00"));
    }

    @Test
    void update_trimsCity() {
        StagingPointUpdateRequest request = new StagingPointUpdateRequest();
        request.setCity("  Agadir  ");
        givenOwnedRoute();
        stubUpdateEntityApplyingRequest();
        stubToRequest();
        when(stagingPointMapper.toResponse(any(StagingPoint.class))).thenReturn(StagingPointResponse.builder().build());

        stagingPointService.update(driverId, ROUTE_ID, 2L, request);

        verify(stagingPointMapper).updateEntity(requestCaptor.capture(), eq(route.getStagingPoints().get(1)));
        assertThat(requestCaptor.getValue().getCity()).isEqualTo("Agadir");
    }

    @Test
    void update_resyncsArrivalCityWhenLastPointChanges() {
        StagingPointUpdateRequest request = new StagingPointUpdateRequest();
        request.setCity("Agadir");
        givenOwnedRoute();
        stubUpdateEntityApplyingRequest();
        stubToRequest();
        when(stagingPointMapper.toResponse(any(StagingPoint.class))).thenReturn(StagingPointResponse.builder().build());

        stagingPointService.update(driverId, ROUTE_ID, 4L, request);

        assertThat(route.getArrivalCity()).isEqualTo("Agadir");
        assertThat(route.getStartCity()).isEqualTo("Rabat");
    }

    @Test
    void update_resyncsStartDateAndCityWhenFirstPointChanges() {
        LocalDateTime newTime = route.getStartDate().plusMinutes(10);
        StagingPointUpdateRequest request = new StagingPointUpdateRequest();
        request.setCity("Kenitra");
        request.setPassingTime(newTime);
        givenOwnedRoute();
        stubUpdateEntityApplyingRequest();
        stubToRequest();
        when(stagingPointMapper.toResponse(any(StagingPoint.class))).thenReturn(StagingPointResponse.builder().build());

        stagingPointService.update(driverId, ROUTE_ID, 1L, request);

        assertThat(route.getStartDate()).isEqualTo(newTime);
        assertThat(route.getStartCity()).isEqualTo("Kenitra");
    }

    @Test
    void update_leavesRouteUntouchedWhenValidatorRejects() {
        StagingPointUpdateRequest request = new StagingPointUpdateRequest();
        request.setCity("Agadir");
        givenOwnedRoute();
        stubUpdateEntityApplyingRequest();
        stubToRequest();
        when(stagingPointValidator.validateAndSort(anyList(), any(LocalDateTime.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prix invalide"));

        assertFails(() -> stagingPointService.update(driverId, ROUTE_ID, 4L, request),
                HttpStatus.BAD_REQUEST, "Prix invalide");

        assertThat(route.getArrivalCity()).isEqualTo("Marrakech");
        verify(stagingPointMapper, never()).toResponse(any(StagingPoint.class));
    }

    @Test
    void update_failsWhenPointNotOnRoute() {
        givenOwnedRoute();

        assertFails(() -> stagingPointService.update(driverId, ROUTE_ID, 99L, new StagingPointUpdateRequest()),
                HttpStatus.NOT_FOUND, "Point d'arrêt introuvable");

        verify(stagingPointMapper, never()).updateEntity(any(StagingPointUpdateRequest.class), any(StagingPoint.class));
    }

    @Test
    void update_refusesPointUsedByReservation() {
        givenOwnedRoute();
        when(reservationRepository.existsByBoardingPointIdOrDescentPointId(2L, 2L)).thenReturn(true);

        assertFails(() -> stagingPointService.update(driverId, ROUTE_ID, 2L, new StagingPointUpdateRequest()),
                HttpStatus.CONFLICT, "utilisé par des réservations");

        verify(stagingPointMapper, never()).updateEntity(any(StagingPointUpdateRequest.class), any(StagingPoint.class));
    }

    @Test
    void update_propagatesOwnershipError() {
        when(routeService.findOwnedEditableRoute(driverId, ROUTE_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce trajet appartient à un autre conducteur."));

        assertFails(() -> stagingPointService.update(driverId, ROUTE_ID, 2L, new StagingPointUpdateRequest()),
                HttpStatus.FORBIDDEN, "autre conducteur");

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void delete_removesIntermediatePointAndRenumbers() {
        givenOwnedRoute();

        stagingPointService.delete(driverId, ROUTE_ID, 2L);

        assertThat(route.getStagingPoints()).extracting(StagingPoint::getCity)
                .containsExactly("Rabat", "Settat", "Marrakech");
        assertThat(route.getStagingPoints()).extracting(StagingPoint::getStopOrder)
                .containsExactly(0, 1, 2);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 4L})
    void delete_refusesFirstAndLastPoints(long pointId) {
        givenOwnedRoute();

        assertFails(() -> stagingPointService.delete(driverId, ROUTE_ID, pointId),
                HttpStatus.CONFLICT, "ne peuvent pas être supprimés");

        assertThat(route.getStagingPoints()).hasSize(4);
    }

    @Test
    void delete_refusesPointUsedByReservation() {
        givenOwnedRoute();
        when(reservationRepository.existsByBoardingPointIdOrDescentPointId(3L, 3L)).thenReturn(true);

        assertFails(() -> stagingPointService.delete(driverId, ROUTE_ID, 3L),
                HttpStatus.CONFLICT, "utilisé par des réservations");

        assertThat(route.getStagingPoints()).hasSize(4);
    }

    @Test
    void delete_failsWhenPointNotOnRoute() {
        givenOwnedRoute();

        assertFails(() -> stagingPointService.delete(driverId, ROUTE_ID, 99L),
                HttpStatus.NOT_FOUND, "Point d'arrêt introuvable");
    }

    private void givenOwnedRoute() {
        when(routeService.findOwnedEditableRoute(driverId, ROUTE_ID)).thenReturn(route);
    }

    private void stubUpdateEntityApplyingRequest() {
        doAnswer(inv -> {
            StagingPointUpdateRequest request = inv.getArgument(0);
            StagingPoint point = inv.getArgument(1);
            if (request.getCity() != null) {
                point.setCity(request.getCity());
            }
            if (request.getPassingTime() != null) {
                point.setPassingTime(request.getPassingTime());
            }
            if (request.getPrice() != null) {
                point.setPrice(request.getPrice());
            }
            return null;
        }).when(stagingPointMapper).updateEntity(any(StagingPointUpdateRequest.class), any(StagingPoint.class));
    }

    private void stubToRequest() {
        when(stagingPointMapper.toRequest(any(StagingPoint.class))).thenAnswer(inv -> {
            StagingPoint point = inv.getArgument(0);
            return StagingPointRequest.builder()
                    .city(point.getCity())
                    .stopOrder(point.getStopOrder())
                    .passingTime(point.getPassingTime())
                    .price(point.getPrice())
                    .build();
        });
    }
}

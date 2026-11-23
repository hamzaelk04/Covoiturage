package org.covoiturage.controller;

import org.covoiturage.dto.stagingPoint.StagingPointResponse;
import org.covoiturage.dto.stagingPoint.StagingPointUpdateRequest;
import org.covoiturage.service.StagingPointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StagingPointControllerTest {
    private static final long ROUTE_ID = 2L;

    @Mock
    private StagingPointService stagingPointService;

    @InjectMocks
    private StagingPointController stagingPointController;

    private MockMvc mockMvc;
    private final UUID driverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(stagingPointController).build();
    }

    @Test
    void findByRoute_returns200() throws Exception {
        when(stagingPointService.findByRoute(ROUTE_ID)).thenReturn(List.of(response(1L, "Tanger"), response(2L, "Kenitra")));

        mockMvc.perform(get("/api/routes/{routeId}/staging-points", ROUTE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].city").value("Tanger"));
    }

    @Test
    void findByRoute_returns404WhenRouteUnknown() throws Exception {
        when(stagingPointService.findByRoute(ROUTE_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Trajet introuvable (id=2)."));

        mockMvc.perform(get("/api/routes/{routeId}/staging-points", ROUTE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returns200() throws Exception {
        when(stagingPointService.findById(ROUTE_ID, 5L)).thenReturn(response(5L, "Kenitra"));

        mockMvc.perform(get("/api/routes/{routeId}/staging-points/{id}", ROUTE_ID, 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.city").value("Kenitra"));
    }

    @Test
    void findById_returns404WhenPointUnknown() throws Exception {
        when(stagingPointService.findById(ROUTE_ID, 99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Point d'arrêt introuvable (id=99)."));

        mockMvc.perform(get("/api/routes/{routeId}/staging-points/{id}", ROUTE_ID, 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returns200AndBindsBody() throws Exception {
        when(stagingPointService.update(eq(driverId), eq(ROUTE_ID), eq(5L), any(StagingPointUpdateRequest.class)))
                .thenReturn(response(5L, "Kenitra"));

        mockMvc.perform(put("/api/driver/{driverId}/routes/{routeId}/staging-points/{id}", driverId, ROUTE_ID, 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price": 35.00, "passingTime": "2026-12-12T09:10:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        ArgumentCaptor<StagingPointUpdateRequest> captor = ArgumentCaptor.forClass(StagingPointUpdateRequest.class);
        verify(stagingPointService).update(eq(driverId), eq(ROUTE_ID), eq(5L), captor.capture());
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("35.00");
        assertThat(captor.getValue().getPassingTime()).isNotNull();
        assertThat(captor.getValue().getCity()).isNull();
    }

    @Test
    void update_returns400WhenPriceNegative() throws Exception {
        mockMvc.perform(put("/api/driver/{driverId}/routes/{routeId}/staging-points/{id}", driverId, ROUTE_ID, 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price": -5}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stagingPointService);
    }

    @Test
    void update_returns400WhenCityBlank() throws Exception {
        mockMvc.perform(put("/api/driver/{driverId}/routes/{routeId}/staging-points/{id}", driverId, ROUTE_ID, 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city": "   "}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stagingPointService);
    }

    @Test
    void update_returns409WhenPointIsReserved() throws Exception {
        when(stagingPointService.update(eq(driverId), eq(ROUTE_ID), eq(5L), any(StagingPointUpdateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ce point d'arrêt est utilisé par des réservations."));

        mockMvc.perform(put("/api/driver/{driverId}/routes/{routeId}/staging-points/{id}", driverId, ROUTE_ID, 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price": 35.00}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/driver/{driverId}/routes/{routeId}/staging-points/{id}", driverId, ROUTE_ID, 5L))
                .andExpect(status().isNoContent());

        verify(stagingPointService).delete(driverId, ROUTE_ID, 5L);
    }

    @Test
    void delete_returns409WhenDepartureOrArrival() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Le point de départ et le point d'arrivée ne peuvent pas être supprimés."))
                .when(stagingPointService).delete(driverId, ROUTE_ID, 1L);

        mockMvc.perform(delete("/api/driver/{driverId}/routes/{routeId}/staging-points/{id}", driverId, ROUTE_ID, 1L))
                .andExpect(status().isConflict());
    }

    private StagingPointResponse response(Long id, String city) {
        return StagingPointResponse.builder()
                .id(id)
                .city(city)
                .stopOrder(1)
                .price(new BigDecimal("30.00"))
                .build();
    }
}

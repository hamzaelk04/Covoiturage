package org.covoiturage.controller;

import org.covoiturage.dto.route.RouteCreateRequest;
import org.covoiturage.dto.route.RouteResponse;
import org.covoiturage.dto.route.RouteSearchCriteria;
import org.covoiturage.dto.route.RouteUpdateRequest;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RouteControllerTest {
    private static final long ROUTE_ID = 10L;

    @Mock
    private RouteService routeService;

    @InjectMocks
    private RouteController routeController;

    private MockMvc mockMvc;
    private final UUID driverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(routeController).build();
    }

    @Test
    void create_returns201WithLocationAndBody() throws Exception {
        when(routeService.create(eq(driverId), any(RouteCreateRequest.class))).thenReturn(response(1L));

        mockMvc.perform(post("/api/driver/{driverId}", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": 1,
                                  "availablePlaces": 3,
                                  "stagingPoints": [
                                    {"city": "Rabat", "stopOrder": 0, "passingTime": "2026-12-10T17:00:00", "price": 0.00},
                                    {"city": "Casablanca", "stopOrder": 1, "passingTime": "2026-12-10T18:15:00", "price": 40.00}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/routes/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        ArgumentCaptor<RouteCreateRequest> captor = ArgumentCaptor.forClass(RouteCreateRequest.class);
        verify(routeService).create(eq(driverId), captor.capture());
        assertThat(captor.getValue().getVehicleId()).isEqualTo(1L);
        assertThat(captor.getValue().getAvailablePlaces()).isEqualTo(3);
        assertThat(captor.getValue().getStagingPoints()).hasSize(2);
    }

    @Test
    void create_returns400WhenBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/driver/{driverId}", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"availablePlaces": 0, "stagingPoints": []}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routeService);
    }

    @Test
    void create_returns403WhenServiceRefuses() throws Exception {
        when(routeService.create(eq(driverId), any(RouteCreateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce véhicule appartient à un autre conducteur."));

        mockMvc.perform(post("/api/driver/{driverId}", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": 1,
                                  "availablePlaces": 2,
                                  "stagingPoints": [
                                    {"city": "Rabat", "stopOrder": 0, "passingTime": "2026-12-20T17:00:00", "price": 0.00},
                                    {"city": "Marrakech", "stopOrder": 1, "passingTime": "2026-12-20T21:00:00", "price": 120.00}
                                  ]
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_returns200() throws Exception {
        when(routeService.getById(ROUTE_ID)).thenReturn(response(ROUTE_ID));

        mockMvc.perform(get("/api/routes/{id}", ROUTE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.startCity").value("Rabat"))
                .andExpect(jsonPath("$.arrivalCity").value("Marrakech"));
    }

    @Test
    void getById_returns404WhenUnknown() throws Exception {
        when(routeService.getById(99999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Trajet introuvable (id=99999)."));

        mockMvc.perform(get("/api/routes/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_bindsCriteriaAndReturnsPage() throws Exception {
        when(routeService.search(any(RouteSearchCriteria.class), eq(1), eq(5)))
                .thenReturn(new PageImpl<>(List.of(response(1L)), PageRequest.of(1, 5), 6));

        mockMvc.perform(get("/api/routes/search")
                        .param("startCity", "Rabat")
                        .param("arrivalCity", "Marrakech")
                        .param("date", "2026-12-10")
                        .param("seats", "2")
                        .param("maxPrice", "80")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(6));

        ArgumentCaptor<RouteSearchCriteria> captor = ArgumentCaptor.forClass(RouteSearchCriteria.class);
        verify(routeService).search(captor.capture(), eq(1), eq(5));
        assertThat(captor.getValue().getStartCity()).isEqualTo("Rabat");
        assertThat(captor.getValue().getArrivalCity()).isEqualTo("Marrakech");
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.of(2026, 12, 10));
        assertThat(captor.getValue().getSeats()).isEqualTo(2);
        assertThat(captor.getValue().getMaxPrice()).isEqualByComparingTo("80");
    }

    @Test
    void search_usesDefaultPaging() throws Exception {
        when(routeService.search(any(RouteSearchCriteria.class), eq(0), eq(10)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/routes/search"))
                .andExpect(status().isOk());

        verify(routeService).search(any(RouteSearchCriteria.class), eq(0), eq(10));
    }

    @Test
    void search_returns400WhenSeatsInvalid() throws Exception {
        mockMvc.perform(get("/api/routes/search").param("seats", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routeService);
    }

    @Test
    void getByDriver_returns200() throws Exception {
        when(routeService.getByDriver(driverId)).thenReturn(List.of(response(1L), response(2L)));

        mockMvc.perform(get("/api/driver/{driverId}/routes", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void update_returns200() throws Exception {
        when(routeService.update(eq(driverId), eq(ROUTE_ID), any(RouteUpdateRequest.class)))
                .thenReturn(response(ROUTE_ID));

        mockMvc.perform(put("/api/driver/{driverId}/routes/{id}", driverId, ROUTE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"availablePlaces": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));

        ArgumentCaptor<RouteUpdateRequest> captor = ArgumentCaptor.forClass(RouteUpdateRequest.class);
        verify(routeService).update(eq(driverId), eq(ROUTE_ID), captor.capture());
        assertThat(captor.getValue().getAvailablePlaces()).isEqualTo(2);
        assertThat(captor.getValue().getVehicleId()).isNull();
    }

    @Test
    void update_returns400WhenPlacesBelowOne() throws Exception {
        mockMvc.perform(put("/api/driver/{driverId}/routes/{id}", driverId, ROUTE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"availablePlaces": 0}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routeService);
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/driver/{driverId}/routes/{id}", driverId, ROUTE_ID))
                .andExpect(status().isNoContent());

        verify(routeService).delete(driverId, ROUTE_ID);
    }

    @Test
    void delete_returns409WhenRouteHasReservations() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ce trajet a des réservations."))
                .when(routeService).delete(driverId, ROUTE_ID);

        mockMvc.perform(delete("/api/driver/{driverId}/routes/{id}", driverId, ROUTE_ID))
                .andExpect(status().isConflict());
    }

    @Test
    void complete_returns204() throws Exception {
        mockMvc.perform(patch("/api/driver/{driverId}/routes/{id}/complete", driverId, ROUTE_ID))
                .andExpect(status().isNoContent());

        verify(routeService).complete(driverId, ROUTE_ID);
    }

    @Test
    void complete_returns409BeforeDeparture() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Un trajet ne peut être terminé qu'après son heure de départ."))
                .when(routeService).complete(driverId, ROUTE_ID);

        mockMvc.perform(patch("/api/driver/{driverId}/routes/{id}/complete", driverId, ROUTE_ID))
                .andExpect(status().isConflict());
    }

    @Test
    void cancel_returns204() throws Exception {
        mockMvc.perform(patch("/api/driver/{driverId}/routes/{id}/cancel", driverId, ROUTE_ID))
                .andExpect(status().isNoContent());

        verify(routeService).cancel(driverId, ROUTE_ID);
    }

    @Test
    void cancel_returns403WhenAnotherDriversRoute() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce trajet appartient à un autre conducteur."))
                .when(routeService).cancel(driverId, ROUTE_ID);

        mockMvc.perform(patch("/api/driver/{driverId}/routes/{id}/cancel", driverId, ROUTE_ID))
                .andExpect(status().isForbidden());
    }

    private RouteResponse response(Long id) {
        return RouteResponse.builder()
                .id(id)
                .startCity("Rabat")
                .arrivalCity("Marrakech")
                .availablePlaces(3)
                .status(RouteStatus.PUBLISHED)
                .vehicleId(1L)
                .stagingPoints(List.of())
                .build();
    }
}

package org.covoiturage.controller;

import org.covoiturage.dto.reservation.ReservationCreateRequest;
import org.covoiturage.dto.reservation.ReservationResponse;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.service.ReservationService;
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
class ReservationControllerTest {
    private static final String BODY = """
            {"routeId": 1, "boardingPointId": 3, "descentPointId": 4, "numberOfSeats": 1}
            """;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationController reservationController;

    private MockMvc mockMvc;
    private final UUID passengerId = UUID.randomUUID();
    private final UUID driverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reservationController).build();
    }

    @Test
    void create_returns201WithLocationAndAmount() throws Exception {
        when(reservationService.create(eq(passengerId), any(ReservationCreateRequest.class))).thenReturn(response(100L));

        mockMvc.perform(post("/api/passengers/{passengerId}/reservations", passengerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "http://localhost/api/passengers/" + passengerId + "/reservations/100"))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.totalAmount").value(50.0))
                .andExpect(jsonPath("$.remainingPlaces").value(2))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        ArgumentCaptor<ReservationCreateRequest> captor = ArgumentCaptor.forClass(ReservationCreateRequest.class);
        verify(reservationService).create(eq(passengerId), captor.capture());
        assertThat(captor.getValue().getRouteId()).isEqualTo(1L);
        assertThat(captor.getValue().getBoardingPointId()).isEqualTo(3L);
        assertThat(captor.getValue().getDescentPointId()).isEqualTo(4L);
        assertThat(captor.getValue().getNumberOfSeats()).isEqualTo(1);
    }

    @Test
    void create_returns400WhenBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/passengers/{passengerId}/reservations", passengerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"routeId": 1, "numberOfSeats": 0}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reservationService);
    }

    @Test
    void create_returns409WhenRouteIsFull() throws Exception {
        when(reservationService.create(eq(passengerId), any(ReservationCreateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Le trajet est complet."));

        mockMvc.perform(post("/api/passengers/{passengerId}/reservations", passengerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void create_returns404WhenPassengerUnknown() throws Exception {
        when(reservationService.create(eq(passengerId), any(ReservationCreateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Passager introuvable"));

        mockMvc.perform(post("/api/passengers/{passengerId}/reservations", passengerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByPassenger_returns200() throws Exception {
        when(reservationService.getByPassenger(passengerId)).thenReturn(List.of(response(1L), response(2L)));

        mockMvc.perform(get("/api/passengers/{passengerId}/reservations", passengerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getById_returns200() throws Exception {
        when(reservationService.getById(passengerId, 5L)).thenReturn(response(5L));

        mockMvc.perform(get("/api/passengers/{passengerId}/reservations/{id}", passengerId, 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void getById_returns403WhenAnotherPassengersReservation() throws Exception {
        when(reservationService.getById(passengerId, 5L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette réservation appartient à un autre passager."));

        mockMvc.perform(get("/api/passengers/{passengerId}/reservations/{id}", passengerId, 5L))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancel_returns204() throws Exception {
        mockMvc.perform(delete("/api/passengers/{passengerId}/reservations/{id}", passengerId, 5L))
                .andExpect(status().isNoContent());

        verify(reservationService).cancel(passengerId, 5L);
    }

    @Test
    void cancel_returns409WhenDelayExceeded() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Le délai d'annulation est dépassé."))
                .when(reservationService).cancel(passengerId, 5L);

        mockMvc.perform(delete("/api/passengers/{passengerId}/reservations/{id}", passengerId, 5L))
                .andExpect(status().isConflict());
    }

    @Test
    void getByRoute_returns200() throws Exception {
        when(reservationService.getByRoute(driverId, 1L)).thenReturn(List.of(response(1L)));

        mockMvc.perform(get("/api/driver/{driverId}/routes/{routeId}/reservations", driverId, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByRoute_returns403WhenAnotherDriversRoute() throws Exception {
        when(reservationService.getByRoute(driverId, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce trajet appartient à un autre conducteur."));

        mockMvc.perform(get("/api/driver/{driverId}/routes/{routeId}/reservations", driverId, 1L))
                .andExpect(status().isForbidden());
    }

    private ReservationResponse response(Long id) {
        return ReservationResponse.builder()
                .id(id)
                .passengerId(passengerId)
                .routeId(1L)
                .boardingPointId(3L)
                .boardingCity("Settat")
                .descentPointId(4L)
                .descentCity("Marrakech")
                .numberOfSeats(1)
                .totalAmount(new BigDecimal("50.00"))
                .status(ReservationStatus.CONFIRMED)
                .remainingPlaces(2)
                .build();
    }
}

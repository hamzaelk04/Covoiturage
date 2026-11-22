package org.covoiturage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.covoiturage.dto.reservation.ReservationCreateRequest;
import org.covoiturage.dto.reservation.ReservationResponse;
import org.covoiturage.entity.Passenger;
import org.covoiturage.entity.Reservation;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.StagingPoint;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.mapper.ReservationMapper;
import org.covoiturage.repository.PassengerRepository;
import org.covoiturage.repository.ReservationRepository;
import org.covoiturage.repository.RouteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
    private static final Duration CANCELLATION_DELAY = Duration.ofHours(24);

    private final ReservationRepository reservationRepository;
    private final RouteRepository routeRepository;
    private final PassengerRepository passengerRepository;
    private final RouteService routeService;
    private final ReservationMapper reservationMapper;

    @Transactional
    public ReservationResponse create(UUID passengerId, ReservationCreateRequest request) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> refused(HttpStatus.NOT_FOUND, "Passager introuvable : " + passengerId));

        Route route = routeRepository.findByIdForUpdate(request.getRouteId())
                .orElseThrow(() -> refused(HttpStatus.NOT_FOUND,
                        "Trajet introuvable (id=" + request.getRouteId() + ")."));

        if (route.getStatus() == RouteStatus.FULL) {
            throw refused(HttpStatus.CONFLICT, "Le trajet est complet.");
        }
        if (route.getStatus() != RouteStatus.PUBLISHED) {
            throw refused(HttpStatus.CONFLICT,
                    "Le trajet n'est pas ouvert à la réservation (statut " + route.getStatus() + ").");
        }

        StagingPoint boardingPoint = findPoint(route, request.getBoardingPointId(), "montée");
        StagingPoint descentPoint = findPoint(route, request.getDescentPointId(), "descente");

        if (boardingPoint.getStopOrder() >= descentPoint.getStopOrder()) {
            throw refused(HttpStatus.BAD_REQUEST,
                    "Le point de montée doit être situé avant le point de descente.");
        }

        if (reservationRepository.existsByPassengerIdAndBoardingPointRouteIdAndStatus(
                passengerId, route.getId(), ReservationStatus.CONFIRMED)) {
            throw refused(HttpStatus.CONFLICT,
                    "Vous avez déjà une réservation confirmée sur ce trajet.");
        }

        long booked = reservationRepository.sumSeatsByRouteIdAndStatus(route.getId(), ReservationStatus.CONFIRMED);
        long remaining = route.getAvailablePlaces() - booked;
        if (request.getNumberOfSeats() > remaining) {
            throw refused(HttpStatus.CONFLICT, "Il ne reste que " + remaining + " place(s) sur ce trajet.");
        }

        BigDecimal totalAmount = descentPoint.getPrice()
                .subtract(boardingPoint.getPrice())
                .multiply(BigDecimal.valueOf(request.getNumberOfSeats()))
                .setScale(2, RoundingMode.HALF_UP);

        Reservation saved = reservationRepository.save(Reservation.builder()
                .passenger(passenger)
                .boardingPoint(boardingPoint)
                .descentPoint(descentPoint)
                .numberOfSeats(request.getNumberOfSeats())
                .totalAmount(totalAmount)
                .reservationDate(LocalDateTime.now())
                .status(ReservationStatus.CONFIRMED)
                .build());

        int remainingAfter = (int) (remaining - request.getNumberOfSeats());
        if (remainingAfter == 0) {
            route.setStatus(RouteStatus.FULL);
        }

        log.info("Réservation créée : id={}, trajet={}, passager={}, places={}, montant={}",
                saved.getId(), route.getId(), passengerId, saved.getNumberOfSeats(), totalAmount);

        ReservationResponse response = reservationMapper.toResponse(saved);
        response.setRemainingPlaces(remainingAfter);
        return response;
    }

    @Transactional
    public void cancel(UUID passengerId, Long id) {
        Reservation reservation = findOwned(passengerId, id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw refused(HttpStatus.CONFLICT,
                    "Cette réservation ne peut pas être annulée (statut " + reservation.getStatus() + ").");
        }

        Long routeId = reservation.getBoardingPoint().getRoute().getId();
        Route route = routeRepository.findByIdForUpdate(routeId)
                .orElseThrow(() -> refused(HttpStatus.NOT_FOUND, "Trajet introuvable (id=" + routeId + ")."));

        if (LocalDateTime.now().isAfter(route.getStartDate().minus(CANCELLATION_DELAY))) {
            throw refused(HttpStatus.CONFLICT,
                    "Le délai d'annulation est dépassé : une réservation s'annule jusqu'à 24 heures avant le départ.");
        }

        reservation.setStatus(ReservationStatus.CANCELED);

        if (route.getStatus() == RouteStatus.FULL) {
            route.setStatus(RouteStatus.PUBLISHED);
        }

        log.info("Réservation annulée : id={}, trajet={}, passager={}", id, routeId, passengerId);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(UUID passengerId, Long id) {
        return reservationMapper.toResponse(findOwned(passengerId, id));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getByPassenger(UUID passengerId) {
        if (!passengerRepository.existsById(passengerId)) {
            throw refused(HttpStatus.NOT_FOUND, "Passager introuvable : " + passengerId);
        }
        return reservationMapper.toResponses(
                reservationRepository.findByPassengerIdOrderByReservationDateDesc(passengerId));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getByRoute(UUID driverId, Long routeId) {
        routeService.findOwnedRoute(driverId, routeId);
        return reservationMapper.toResponses(
                reservationRepository.findByBoardingPointRouteIdOrderByReservationDateDesc(routeId));
    }

    private Reservation findOwned(UUID passengerId, Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> refused(HttpStatus.NOT_FOUND, "Réservation introuvable (id=" + id + ")."));

        if (!reservation.getPassenger().getId().equals(passengerId)) {
            throw refused(HttpStatus.FORBIDDEN, "Cette réservation appartient à un autre passager.");
        }
        return reservation;
    }

    private StagingPoint findPoint(Route route, Long pointId, String role) {
        return route.getStagingPoints().stream()
                .filter(p -> p.getId().equals(pointId))
                .findFirst()
                .orElseThrow(() -> refused(HttpStatus.BAD_REQUEST,
                        "Le point de " + role + " n'appartient pas à ce trajet (id=" + pointId + ")."));
    }

    private ResponseStatusException refused(HttpStatus status, String message) {
        log.warn(message);
        return new ResponseStatusException(status, message);
    }
}
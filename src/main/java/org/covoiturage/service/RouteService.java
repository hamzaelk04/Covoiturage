package org.covoiturage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.covoiturage.dto.route.RouteCreateRequest;
import org.covoiturage.dto.route.RouteResponse;
import org.covoiturage.dto.route.RouteUpdateRequest;
import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.mapper.RouteMapper;
import org.covoiturage.mapper.StagingPointMapper;
import org.covoiturage.repository.ReservationRepository;
import org.covoiturage.repository.RouteRepository;
import org.covoiturage.repository.VehicleRepository;
import org.covoiturage.service.validator.StagingPointValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    private static final Set<RouteStatus> EDITABLE_STATUSES = Set.of(RouteStatus.PUBLISHED, RouteStatus.FULL);

    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteMapper routeMapper;
    private final StagingPointMapper stagingPointMapper;
    private final StagingPointValidator stagingPointValidator;
    private final ReservationRepository reservationRepository;

    @Transactional
    public RouteResponse create(UUID driverId, RouteCreateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
//                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable (id=" + request.vehicleId() + ")."));
                .orElseThrow();
//        Driver exception
        if (!vehicle.getDriver().getId().equals(driverId)) throw new RuntimeException();

        if (request.getAvailablePlaces() > vehicle.getCapacity()) {
            throw refused(HttpStatus.BAD_REQUEST, "Le véhicule a une capacité de " + vehicle.getCapacity()
                    + " places : " + vehicle.getCapacity() + " places au maximum.");
        }

        List<StagingPointRequest> pointRequests = stagingPointValidator
                .validateAndSort(request.getStagingPoints(), LocalDateTime.now());

        StagingPointRequest first = pointRequests.getFirst();
        StagingPointRequest last = pointRequests.getLast();

        Route route = Route.builder()
                .startCity(first.getCity())
                .arrivalCity(last.getCity())
                .startDate(first.getPassingTime())
                .availablePlaces(request.getAvailablePlaces())
                .status(RouteStatus.PUBLISHED)
                .vehicle(vehicle)
                .build();

        pointRequests.stream()
                .map(stagingPointMapper::toEntity)
                .forEach(p -> {
                    p.setRoute(route);
                    route.getStagingPoints().add(p);
                });

        Route saved = routeRepository.save(route);
        log.info("Trajet publié : id={}, {} -> {}, départ={}, conducteur={}",
                saved.getId(), saved.getStartCity(), saved.getArrivalCity(), saved.getStartDate(), driverId);
        return routeMapper.toResponse(saved);
    }

    @Transactional
    public RouteResponse update(UUID driverId, Long id, RouteUpdateRequest request) {
        Route route = findOwnedEditableRoute(driverId, id);
        boolean hasBookings = reservationRepository
                .existsByBoardingPointRouteIdAndStatus(id, ReservationStatus.CONFIRMED);
        int previousPlaces = route.getAvailablePlaces();

        if (request.getVehicleId() != null && !request.getVehicleId().equals(route.getVehicle().getId())) {
            if (hasBookings) {
                throw refused(HttpStatus.CONFLICT,
                        "Le véhicule ne peut plus être changé : le trajet a des réservations confirmées.");
            }
            route.setVehicle(findOwnedVehicle(driverId, request.getVehicleId()));
        }

        if (request.getAvailablePlaces() != null) {
            if (hasBookings && request.getAvailablePlaces() < previousPlaces) {
                throw refused(HttpStatus.CONFLICT,
                        "Les places proposées ne peuvent pas être réduites : le trajet a des réservations confirmées.");
            }
            route.setAvailablePlaces(request.getAvailablePlaces());
        }

        int capacity = route.getVehicle().getCapacity();
        if (route.getAvailablePlaces() > capacity) {
            throw refused(HttpStatus.BAD_REQUEST, "Le véhicule a une capacité de " + capacity
                    + " places : " + capacity + " places au maximum.");
        }

        if (route.getStatus() == RouteStatus.FULL && route.getAvailablePlaces() > previousPlaces) {
            route.setStatus(RouteStatus.PUBLISHED);
        }

        log.info("Trajet modifié : id={}, conducteur={}", id, driverId);
        return routeMapper.toResponse(route);
    }

//    HELPERS
    private ResponseStatusException refused(HttpStatus status, String message) {
        log.warn(message);
        return new ResponseStatusException(status, message);
    }

    private Route findOwnedRoute(UUID driverId, Long routeId) {
        Route route = routeRepository.findDetailById(routeId)
                .orElseThrow(() -> refused(HttpStatus.NOT_FOUND, "Trajet introuvable (id=" + routeId + ")."));

        if (!route.getVehicle().getDriver().getId().equals(driverId)) {
            throw refused(HttpStatus.FORBIDDEN, "Ce trajet appartient à un autre conducteur.");
        }

        return route;
    }

    public Route findOwnedEditableRoute(UUID driverId, Long routeId) {
        Route route = findOwnedRoute(driverId, routeId);

        if (!EDITABLE_STATUSES.contains(route.getStatus())) {
            throw refused(HttpStatus.CONFLICT,
                    "Un trajet au statut " + route.getStatus() + " ne peut plus être modifié.");
        }

        return route;
    }

    private Vehicle findOwnedVehicle(UUID driverId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> refused(HttpStatus.NOT_FOUND, "Véhicule introuvable (id=" + vehicleId + ")."));

        if (!vehicle.getDriver().getId().equals(driverId)) {
            throw refused(HttpStatus.FORBIDDEN, "Ce véhicule appartient à un autre conducteur.");
        }
        return vehicle;
    }
}

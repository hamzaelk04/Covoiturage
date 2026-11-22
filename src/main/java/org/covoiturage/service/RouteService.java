package org.covoiturage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.covoiturage.dto.route.RouteCreateRequest;
import org.covoiturage.dto.route.RouteResponse;
import org.covoiturage.dto.route.RouteSearchCriteria;
import org.covoiturage.dto.route.RouteUpdateRequest;
import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.covoiturage.entity.Reservation;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.exception.ConflictException;
import org.covoiturage.exception.ForbiddenOperationException;
import org.covoiturage.exception.InvalidRequestException;
import org.covoiturage.exception.ResourceNotFoundException;
import org.covoiturage.mapper.RouteMapper;
import org.covoiturage.mapper.StagingPointMapper;
import org.covoiturage.repository.DriverRepository;
import org.covoiturage.repository.ReservationRepository;
import org.covoiturage.repository.RouteRepository;
import org.covoiturage.repository.VehicleRepository;
import org.covoiturage.service.validator.StagingPointValidator;
import org.covoiturage.specification.RouteSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    private static final int MAX_PAGE_SIZE = 20;
    private static final Set<RouteStatus> EDITABLE_STATUSES = Set.of(RouteStatus.PUBLISHED, RouteStatus.FULL);

    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteMapper routeMapper;
    private final StagingPointMapper stagingPointMapper;
    private final StagingPointValidator stagingPointValidator;
    private final ReservationRepository reservationRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public RouteResponse create(UUID driverId, RouteCreateRequest request) {
        Vehicle vehicle = findOwnedVehicle(driverId, request.getVehicleId());

        if (request.getAvailablePlaces() > vehicle.getCapacity()) {
            throw new InvalidRequestException("Le véhicule a une capacité de " + vehicle.getCapacity()
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
                throw new ConflictException(
                        "Le véhicule ne peut plus être changé : le trajet a des réservations confirmées.");
            }
            route.setVehicle(findOwnedVehicle(driverId, request.getVehicleId()));
        }

        if (request.getAvailablePlaces() != null) {
            if (hasBookings && request.getAvailablePlaces() < previousPlaces) {
                throw new ConflictException(
                        "Les places proposées ne peuvent pas être réduites : le trajet a des réservations confirmées.");
            }
            route.setAvailablePlaces(request.getAvailablePlaces());
        }

        int capacity = route.getVehicle().getCapacity();
        if (route.getAvailablePlaces() > capacity) {
            throw new InvalidRequestException("Le véhicule a une capacité de " + capacity
                    + " places : " + capacity + " places au maximum.");
        }

        if (route.getStatus() == RouteStatus.FULL && route.getAvailablePlaces() > previousPlaces) {
            route.setStatus(RouteStatus.PUBLISHED);
        }

        log.info("Trajet modifié : id={}, conducteur={}", id, driverId);
        return routeMapper.toResponse(route);
    }

    @Transactional
    public void complete(UUID driverId, Long id) {
        Route route = findOwnedRoute(driverId, id);
        requireOpen(route, "terminé");

        if (LocalDateTime.now().isBefore(route.getStartDate())) {
            throw new ConflictException("Un trajet ne peut être terminé qu'après son heure de départ.");
        }

        route.setStatus(RouteStatus.COMPLETED);
        log.info("Trajet terminé : id={}, conducteur={}", id, driverId);
    }

    @Transactional
    public void cancel(UUID driverId, Long id) {
        Route route = findOwnedRoute(driverId, id);
        requireOpen(route, "annulé");

        List<Reservation> confirmed = reservationRepository
                .findByBoardingPointRouteIdAndStatus(id, ReservationStatus.CONFIRMED);
        confirmed.forEach(r -> r.setStatus(ReservationStatus.CANCELED));
        route.setStatus(RouteStatus.CANCELED);

        log.info("Trajet annulé : id={}, conducteur={}, réservations annulées={}", id, driverId, confirmed.size());
    }

    private void requireOpen(Route route, String action) {
        if (!EDITABLE_STATUSES.contains(route.getStatus())) {
            throw new ConflictException(
                    "Un trajet au statut " + route.getStatus() + " ne peut pas être " + action + ".");
        }
    }

    @Transactional
    public void delete(UUID driverId, Long id) {
        Route route = findOwnedRoute(driverId, id);

        if (reservationRepository.existsByBoardingPointRouteId(id)) {
            throw new ConflictException(
                    "Ce trajet a des réservations : il ne peut pas être supprimé, annulez-le à la place.");
        }

        routeRepository.delete(route);
        log.info("Trajet supprimé : id={}, conducteur={}", id, driverId);
    }

    @Transactional(readOnly = true)
    public RouteResponse getById(Long id) {
        Route route = routeRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable (id=" + id + ")."));
        return routeMapper.toResponse(route);
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> getByDriver(UUID driverId) {
        if (!driverRepository.existsById(driverId)) {
            throw new ResourceNotFoundException("Conducteur introuvable : " + driverId);
        }
        return routeMapper.toResponses(routeRepository.findByVehicleDriverIdOrderByStartDateDesc(driverId));
    }

    @Transactional(readOnly = true)
    public Page<RouteResponse> search(RouteSearchCriteria criteria, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by("startDate").ascending());

        return routeRepository
                .findAll(RouteSpecification.search(criteria), pageable)
                .map(routeMapper::toResponse);
    }

    public Route findOwnedRoute(UUID driverId, Long routeId) {
        Route route = routeRepository.findDetailById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable (id=" + routeId + ")."));

        if (!route.getVehicle().getDriver().getId().equals(driverId)) {
            throw new ForbiddenOperationException("Ce trajet appartient à un autre conducteur.");
        }

        return route;
    }

    public Route findOwnedEditableRoute(UUID driverId, Long routeId) {
        Route route = findOwnedRoute(driverId, routeId);

        if (!EDITABLE_STATUSES.contains(route.getStatus())) {
            throw new ConflictException(
                    "Un trajet au statut " + route.getStatus() + " ne peut plus être modifié.");
        }

        return route;
    }

    private Vehicle findOwnedVehicle(UUID driverId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable (id=" + vehicleId + ")."));

        if (!vehicle.getDriver().getId().equals(driverId)) {
            throw new ForbiddenOperationException("Ce véhicule appartient à un autre conducteur.");
        }
        return vehicle;
    }
}
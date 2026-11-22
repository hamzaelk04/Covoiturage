package org.covoiturage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.covoiturage.dto.stagingPoint.StagingPointResponse;
import org.covoiturage.dto.stagingPoint.StagingPointUpdateRequest;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.StagingPoint;
import org.covoiturage.exception.ConflictException;
import org.covoiturage.exception.ResourceNotFoundException;
import org.covoiturage.mapper.StagingPointMapper;
import org.covoiturage.repository.ReservationRepository;
import org.covoiturage.repository.RouteRepository;
import org.covoiturage.repository.StagingPointRepository;
import org.covoiturage.service.validator.StagingPointValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StagingPointService {
    private final StagingPointRepository stagingPointRepository;
    private final RouteRepository routeRepository;
    private final ReservationRepository reservationRepository;
    private final StagingPointMapper stagingPointMapper;
    private final StagingPointValidator stagingPointValidator;
    private final RouteService routeService;

    @Transactional(readOnly = true)
    public List<StagingPointResponse> findByRoute(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("Trajet introuvable (id=" + routeId + ").");
        }
        return stagingPointMapper.toResponses(stagingPointRepository.findByRouteIdOrderByStopOrderAsc(routeId));
    }

    @Transactional(readOnly = true)
    public StagingPointResponse findById(Long routeId, Long id) {
        StagingPoint point = stagingPointRepository.findByIdAndRouteId(id, routeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Point d'arrêt introuvable (id=" + id + ") sur le trajet " + routeId + "."));
        return stagingPointMapper.toResponse(point);
    }

    @Transactional
    public StagingPointResponse update(UUID driverId, Long routeId, Long id, StagingPointUpdateRequest request) {
        Route route = routeService.findOwnedEditableRoute(driverId, routeId);
        StagingPoint point = findInRoute(route, id);
        checkNoReservation(point);

        if (request.getCity() != null) {
            request.setCity(request.getCity().trim());
        }

        stagingPointMapper.updateEntity(request, point);

        List<StagingPointRequest> candidate = route.getStagingPoints().stream()
                .map(stagingPointMapper::toRequest)
                .toList();
        stagingPointValidator.validateAndSort(candidate, LocalDateTime.now());

        syncRoute(route);

        log.info("Point d'arrêt modifié : id={}, trajet={}, conducteur={}", id, routeId, driverId);
        return stagingPointMapper.toResponse(point);
    }

    @Transactional
    public void delete(UUID driverId, Long routeId, Long id) {
        Route route = routeService.findOwnedEditableRoute(driverId, routeId);
        StagingPoint point = findInRoute(route, id);
        List<StagingPoint> points = route.getStagingPoints();

        if (point.getStopOrder() == 0 || point.getStopOrder() == points.size() - 1) {
            throw new ConflictException(
                    "Le point de départ et le point d'arrivée ne peuvent pas être supprimés.");
        }
        checkNoReservation(point);

        points.remove(point);
        IntStream.range(0, points.size()).forEach(i -> points.get(i).setStopOrder(i));

        log.info("Point d'arrêt supprimé : id={}, trajet={}, conducteur={}", id, routeId, driverId);
    }

    private StagingPoint findInRoute(Route route, Long id) {
        return route.getStagingPoints().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Point d'arrêt introuvable (id=" + id + ") sur le trajet " + route.getId() + "."));
    }

    private void checkNoReservation(StagingPoint point) {
        if (reservationRepository.existsByBoardingPointIdOrDescentPointId(point.getId(), point.getId())) {
            throw new ConflictException(
                    "Ce point d'arrêt est utilisé par des réservations : il ne peut pas être modifié ou supprimé.");
        }
    }

    private void syncRoute(Route route) {
        List<StagingPoint> points = route.getStagingPoints();
        route.setStartCity(points.getFirst().getCity());
        route.setStartDate(points.getFirst().getPassingTime());
        route.setArrivalCity(points.getLast().getCity());
    }
}
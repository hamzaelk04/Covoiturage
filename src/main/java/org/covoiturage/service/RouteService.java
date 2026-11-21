package org.covoiturage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.covoiturage.dto.route.RouteCreateRequest;
import org.covoiturage.dto.route.RouteResponse;
import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.mapper.RouteMapper;
import org.covoiturage.mapper.StagingPointMapper;
import org.covoiturage.repository.RouteRepository;
import org.covoiturage.repository.VehicleRepository;
import org.covoiturage.service.validator.StagingPointValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteMapper routeMapper;
    private final StagingPointMapper stagingPointMapper;
    private final StagingPointValidator stagingPointValidator;

    @Transactional
    public RouteResponse create(UUID driverId, RouteCreateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
//                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable (id=" + request.vehicleId() + ")."));
                .orElseThrow();
//        Driver exception
        if (!vehicle.getDriver().getId().equals(driverId)) throw new RuntimeException();

        if (request.getAvailablePlaces() > vehicle.getCapacity()) {
//            throw new InvalidRequestException("Le véhicule a une capacité de " + vehicle.getCapacity()
//                    + " places : " + vehicle.getCapacity() + " places au maximum.");
            throw new RuntimeException();
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
}

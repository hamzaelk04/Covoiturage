package org.covoiturage.service;

import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.vehicle.VehicleCreateRequest;
import org.covoiturage.dto.vehicle.VehicleResponse;
import org.covoiturage.dto.vehicle.VehicleUpdateRequest;
import org.covoiturage.entity.Driver;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.exception.ConflictException;
import org.covoiturage.exception.ResourceAlreadyExistsException;
import org.covoiturage.exception.ResourceNotFoundException;
import org.covoiturage.mapper.VehicleMapper;
import org.covoiturage.repository.DriverRepository;
import org.covoiturage.repository.RouteRepository;
import org.covoiturage.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {
    private final UserService userService;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final DriverRepository driverRepository;
    private final RouteRepository routeRepository;

    private static final int MAX_PAGE_SIZE = 3;
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "brand", "model", "registrationNumber", "capacity");

    @Transactional
    public VehicleResponse create(UUID driverId, VehicleCreateRequest request) {
        checkRegistrationNumber(request.getRegistrationNumber());

        Driver driver = userService.getDriverEntity(driverId);

        Vehicle vehicle = vehicleMapper.toEntity(request);
        vehicle.setDriver(driver);
        vehicle.setBrand(request.getBrand().trim());
        vehicle.setRegistrationNumber(request.getRegistrationNumber().trim());

        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse update(UUID driverId, Long id, VehicleUpdateRequest request) {
        Vehicle vehicle = vehicleRepository.findByIdAndDriver_Id(id, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable (id=" + id + ")"));

        if (request.getBrand() != null) {
            request.setBrand(request.getBrand().trim());
        }
        if (request.getModel() != null) {
            request.setModel(request.getModel().trim());
        }
        if (request.getRegistrationNumber() != null) {
            String reg = request.getRegistrationNumber().trim().toUpperCase(Locale.ROOT);
            if (!reg.equals(vehicle.getRegistrationNumber())) checkRegistrationNumber(reg);

            request.setRegistrationNumber(reg);

        }

        return vehicleMapper.updateAndMap(request, vehicle);
    }

//    DELETE
    public void delete(Long vehicleId, UUID driverId) {
        Vehicle vehicle = vehicleRepository.findByIdAndDriver_Id(vehicleId, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable (id=" + vehicleId + ")"));

        if (routeRepository.existsByVehicleId(vehicleId)) {
            throw new ConflictException("Ce véhicule est associé à un trajet : il ne peut pas être supprimé.");
        }

        vehicleRepository.delete(vehicle);
    }

//    READ
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByDriverId(UUID driverId) {
        if (!driverRepository.existsById(driverId)) {
            throw new ResourceNotFoundException("Conducteur introuvable : " + driverId);
        }
        return vehicleRepository.findAllByDriverId(driverId)
                .stream()
                .map(vehicleMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicleByIdAndDriverId(Long id, UUID driverId) {
        Vehicle vehicle = vehicleRepository.findByIdAndDriver_Id(id, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable (id=" + id + ")"));

        return vehicleMapper.toResponse(vehicle);
    }

//    HELPER
    private void checkRegistrationNumber(String registrationNumber) {
        if (vehicleRepository.existsByRegistrationNumber(registrationNumber)) throw new ResourceAlreadyExistsException("Ce numéro d'immatriculation existe déjà");
    }
}

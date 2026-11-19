package org.covoiturage.service;

import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.vehicle.VehicleCreateRequest;
import org.covoiturage.dto.vehicle.VehicleResponse;
import org.covoiturage.entity.Driver;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.mapper.VehicleMapper;
import org.covoiturage.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {
    private final UserService userService;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Transactional
    public VehicleResponse create(UUID driverId, VehicleCreateRequest request) {
        checkRegistraionNumber(request.getRegistrationNumber());

        Driver driver = userService.getDriverEntity(driverId);

        Vehicle vehicle = vehicleMapper.toEntity(request);
        vehicle.setDriver(driver);
        vehicle.setBrand(request.getBrand().trim());
        vehicle.setRegistrationNumber(request.getRegistrationNumber().trim());

        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

//    HELPER
    private void checkRegistraionNumber(String registrationNumber) {
        if (vehicleRepository.existsByRegistrationNumber(registrationNumber)) throw new RuntimeException("This matricule already exist");
    }
}

package org.covoiturage.service;

import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.vehicle.VehicleCreateRequest;
import org.covoiturage.dto.vehicle.VehicleResponse;
import org.covoiturage.dto.vehicle.VehicleUpdateRequest;
import org.covoiturage.entity.Driver;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.mapper.VehicleMapper;
import org.covoiturage.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {
    private final UserService userService;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vehicle does not exist : " + id
                ));

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

//    HELPER
    private void checkRegistrationNumber(String registrationNumber) {
        if (vehicleRepository.existsByRegistrationNumber(registrationNumber)) throw new RuntimeException("This matricule already exist");
    }
}

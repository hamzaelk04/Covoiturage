package org.covoiturage.repository;

import org.covoiturage.entity.Vehicle;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findAllByDriverId(UUID driverId);

    Optional<Vehicle> findByIdAndDriver_Id(Long id, UUID driverId);

    void deleteById(@NonNull Long id);

    boolean existsByRegistrationNumber(String registrationNumber);
}

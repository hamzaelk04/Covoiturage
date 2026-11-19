package org.covoiturage.repository;

import org.covoiturage.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Page<Vehicle> findByDriver_Id(UUID driverId, Pageable pageable);

    Optional<Vehicle> findByIdAndDriver_Id(Long id, UUID driverId);
}

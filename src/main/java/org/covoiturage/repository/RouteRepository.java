package org.covoiturage.repository;

import jakarta.persistence.LockModeType;
import org.covoiturage.entity.Route;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteRepository extends JpaRepository<Route, Long>, JpaSpecificationExecutor<Route> {
    @Query("""
            SELECT r FROM Route r
            JOIN FETCH r.vehicle v
            JOIN FETCH v.driver
            LEFT JOIN FETCH r.stagingPoints
            WHERE r.id = :id
            """)
    Optional<Route> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"vehicle", "vehicle.driver", "stagingPoints"})
    List<Route> findByVehicleDriverIdOrderByStartDateDesc(UUID vehicleDriverId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Route r WHERE r.id = :id")
    Optional<Route> findByIdForUpdate(@Param("id") Long id);

    boolean existsByVehicleId(Long vehicleId);
}
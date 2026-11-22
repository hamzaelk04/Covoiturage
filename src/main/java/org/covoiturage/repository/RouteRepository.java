package org.covoiturage.repository;

import org.covoiturage.entity.Route;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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
}
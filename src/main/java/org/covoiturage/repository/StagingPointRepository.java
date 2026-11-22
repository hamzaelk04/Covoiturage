package org.covoiturage.repository;

import org.covoiturage.entity.StagingPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StagingPointRepository extends JpaRepository<StagingPoint, Long> {

    List<StagingPoint> findByRouteIdOrderByStopOrderAsc(Long routeId);

    Optional<StagingPoint> findByIdAndRouteId(Long id, Long routeId);
}

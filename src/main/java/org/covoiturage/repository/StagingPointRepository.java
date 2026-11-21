package org.covoiturage.repository;

import org.covoiturage.entity.StagingPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StagingPointRepository extends JpaRepository<StagingPoint, Long> {

    List<StagingPoint> findByRouteIdOrderByStopOrderAsc(Long routeId);
}

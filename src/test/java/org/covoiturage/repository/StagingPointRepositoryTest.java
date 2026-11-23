package org.covoiturage.repository;

import jakarta.persistence.EntityManager;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.StagingPoint;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.testutil.TestPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StagingPointRepositoryTest {

    @Autowired
    private StagingPointRepository stagingPointRepository;
    @Autowired
    private EntityManager em;

    private TestPersistence db;
    private Route route;
    private Route otherRoute;

    @BeforeEach
    void setUp() {
        db = new TestPersistence(em);
        Vehicle vehicle = db.vehicle(db.driver(), 4);
        route = db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        otherRoute = db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(3));
        db.flushAndClear();
    }

    @Test
    void findByRouteIdOrderByStopOrderAsc_returnsPointsOfThatRouteInOrder() {
        List<StagingPoint> points = stagingPointRepository.findByRouteIdOrderByStopOrderAsc(route.getId());

        assertThat(points).hasSize(4);
        assertThat(points).extracting(StagingPoint::getStopOrder).containsExactly(0, 1, 2, 3);
        assertThat(points).extracting(StagingPoint::getCity)
                .containsExactly("Rabat", "Casablanca", "Settat", "Marrakech");
    }

    @Test
    void findByIdAndRouteId_returnsPointWhenItBelongsToRoute() {
        Long pointId = route.getStagingPoints().get(2).getId();

        assertThat(stagingPointRepository.findByIdAndRouteId(pointId, route.getId()))
                .map(StagingPoint::getCity)
                .contains("Settat");
    }

    @Test
    void findByIdAndRouteId_returnsEmptyWhenPointBelongsToAnotherRoute() {
        Long pointId = route.getStagingPoints().get(2).getId();

        assertThat(stagingPointRepository.findByIdAndRouteId(pointId, otherRoute.getId())).isEmpty();
    }
}

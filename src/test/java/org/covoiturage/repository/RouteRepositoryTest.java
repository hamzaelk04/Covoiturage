package org.covoiturage.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.covoiturage.entity.Driver;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RouteRepositoryTest {

    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private EntityManager em;

    private TestPersistence db;

    @BeforeEach
    void setUp() {
        db = new TestPersistence(em);
    }

    @Test
    void findDetailById_fetchesVehicleDriverAndOrderedPoints() {
        Driver driver = db.driver();
        Vehicle vehicle = db.vehicle(driver, 4);
        Route route = db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        db.flushAndClear();

        Route found = routeRepository.findDetailById(route.getId()).orElseThrow();

        PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(util.isLoaded(found, "vehicle")).isTrue();
        assertThat(util.isLoaded(found.getVehicle(), "driver")).isTrue();
        assertThat(util.isLoaded(found, "stagingPoints")).isTrue();
        assertThat(found.getVehicle().getDriver().getId()).isEqualTo(driver.getId());
        assertThat(found.getStagingPoints()).extracting(StagingPoint::getCity)
                .containsExactly("Rabat", "Casablanca", "Settat", "Marrakech");
    }

    @Test
    void findDetailById_returnsEmptyWhenUnknown() {
        assertThat(routeRepository.findDetailById(99999L)).isEmpty();
    }

    @Test
    void findByVehicleDriverIdOrderByStartDateDesc_returnsOnlyThatDriversRoutesNewestFirst() {
        Driver driver = db.driver();
        Driver other = db.driver();
        Vehicle vehicle = db.vehicle(driver, 4);
        Vehicle otherVehicle = db.vehicle(other, 4);
        Route older = db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        Route newer = db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(6));
        db.route(otherVehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(4));
        db.flushAndClear();

        List<Route> routes = routeRepository.findByVehicleDriverIdOrderByStartDateDesc(driver.getId());

        assertThat(routes).extracting(Route::getId).containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findByIdForUpdate_returnsRoute() {
        Driver driver = db.driver();
        Vehicle vehicle = db.vehicle(driver, 4);
        Route route = db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        db.flushAndClear();

        Optional<Route> found = routeRepository.findByIdForUpdate(route.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAvailablePlaces()).isEqualTo(3);
    }

    @Test
    void findByIdForUpdate_returnsEmptyWhenUnknown() {
        assertThat(routeRepository.findByIdForUpdate(99999L)).isEmpty();
    }
}

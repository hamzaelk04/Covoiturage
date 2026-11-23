package org.covoiturage.specification;

import jakarta.persistence.EntityManager;
import org.covoiturage.dto.route.RouteSearchCriteria;
import org.covoiturage.entity.Passenger;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.repository.RouteRepository;
import org.covoiturage.testutil.TestPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RouteSpecificationTest {

    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private EntityManager em;

    private TestPersistence db;
    private Vehicle vehicle;
    private Route routeA;

    @BeforeEach
    void setUp() {
        db = new TestPersistence(em);
        vehicle = db.vehicle(db.driver(), 4);
        routeA = db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.of(2026, 12, 10, 17, 0));
    }

    @Test
    void search_returnsOnlyPublishedRoutes() {
        db.route(vehicle, 3, RouteStatus.CANCELED, LocalDateTime.of(2026, 12, 10, 18, 0));
        db.route(vehicle, 3, RouteStatus.FULL, LocalDateTime.of(2026, 12, 10, 19, 0));
        db.route(vehicle, 3, RouteStatus.COMPLETED, LocalDateTime.of(2026, 12, 10, 20, 0));

        Page<Route> result = search(new RouteSearchCriteria());

        assertThat(result.getContent()).extracting(Route::getId).containsExactly(routeA.getId());
    }

    @Test
    void search_findsRouteWhenBoardingComesBeforeArrival() {
        Page<Route> result = search(criteria("Settat", "Marrakech"));

        assertThat(result.getContent()).extracting(Route::getId).containsExactly(routeA.getId());
    }

    @Test
    void search_excludesRouteWhenDirectionIsReversed() {
        assertThat(search(criteria("Settat", "Casablanca")).getContent()).isEmpty();
    }

    @Test
    void search_ignoresCaseAndSurroundingSpaces() {
        Page<Route> result = search(criteria("  casablanca ", "SETTAT"));

        assertThat(result.getContent()).extracting(Route::getId).containsExactly(routeA.getId());
    }

    @Test
    void search_withDepartureOnlyMatchesAnyPointOfRoute() {
        assertThat(search(criteria("Rabat", null)).getContent()).hasSize(1);
        assertThat(search(criteria("Fes", null)).getContent()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"30,1", "29.99,0"})
    void search_filtersBySegmentPrice(String maxPrice, int expected) {
        RouteSearchCriteria criteria = criteria("Casablanca", "Settat");
        criteria.setMaxPrice(new BigDecimal(maxPrice));

        assertThat(search(criteria).getContent()).hasSize(expected);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,0"})
    void search_filtersByRemainingPlacesIgnoringCanceledReservations(int seats, int expected) {
        Passenger salma = db.passenger();
        Passenger karim = db.passenger();
        db.reservation(salma, routeA, 0, 3, 2, ReservationStatus.CONFIRMED, LocalDateTime.now());
        db.reservation(karim, routeA, 0, 3, 1, ReservationStatus.CANCELED, LocalDateTime.now());
        RouteSearchCriteria criteria = new RouteSearchCriteria();
        criteria.setSeats(seats);

        assertThat(search(criteria).getContent()).hasSize(expected);
    }

    @Test
    void search_defaultsToOneSeat() {
        Passenger salma = db.passenger();
        db.reservation(salma, routeA, 0, 3, 3, ReservationStatus.CONFIRMED, LocalDateTime.now());

        assertThat(search(new RouteSearchCriteria()).getContent()).isEmpty();
    }

    @Test
    void search_dateWithoutDepartureCityUsesRouteStartDate() {
        RouteSearchCriteria sameDay = new RouteSearchCriteria();
        sameDay.setDate(LocalDate.of(2026, 12, 10));
        RouteSearchCriteria otherDay = new RouteSearchCriteria();
        otherDay.setDate(LocalDate.of(2026, 12, 11));

        assertThat(search(sameDay).getContent()).hasSize(1);
        assertThat(search(otherDay).getContent()).isEmpty();
    }

    @Test
    void search_dateWithDepartureCityUsesBoardingPointPassingTime() {
        Route night = db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.of(2026, 12, 10, 23, 0),
                new String[]{"Rabat", "Casablanca", "Settat", "Marrakech"},
                new String[]{"0.00", "40.00", "70.00", "120.00"},
                new long[]{0, 30, 120, 300});
        RouteSearchCriteria nextDay = criteria("Settat", null);
        nextDay.setDate(LocalDate.of(2026, 12, 11));
        RouteSearchCriteria sameDay = criteria("Settat", null);
        sameDay.setDate(LocalDate.of(2026, 12, 10));

        assertThat(search(nextDay).getContent()).extracting(Route::getId).containsExactly(night.getId());
        assertThat(search(sameDay).getContent()).extracting(Route::getId).containsExactly(routeA.getId());
    }

    @Test
    void search_paginatesAndSortsByStartDate() {
        Route earliest = db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.of(2026, 12, 8, 9, 0));
        db.route(vehicle, 3, RouteStatus.PUBLISHED, LocalDateTime.of(2026, 12, 12, 9, 0));
        db.flushAndClear();
        Pageable pageable = PageRequest.of(0, 2, Sort.by("startDate").ascending());

        Page<Route> page = routeRepository.findAll(RouteSpecification.search(new RouteSearchCriteria()), pageable);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Route::getId).containsExactly(earliest.getId(), routeA.getId());
    }

    private Page<Route> search(RouteSearchCriteria criteria) {
        db.flushAndClear();
        return routeRepository.findAll(RouteSpecification.search(criteria),
                PageRequest.of(0, 10, Sort.by("startDate").ascending()));
    }

    private RouteSearchCriteria criteria(String startCity, String arrivalCity) {
        RouteSearchCriteria criteria = new RouteSearchCriteria();
        criteria.setStartCity(startCity);
        criteria.setArrivalCity(arrivalCity);
        return criteria;
    }
}

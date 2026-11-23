package org.covoiturage.testutil;

import jakarta.persistence.EntityManager;
import org.covoiturage.entity.Driver;
import org.covoiturage.entity.Passenger;
import org.covoiturage.entity.Reservation;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.enums.RouteStatus;

import java.time.LocalDateTime;

public class TestPersistence {
    private final EntityManager em;

    public TestPersistence(EntityManager em) {
        this.em = em;
    }

    public Driver driver() {
        Driver driver = TestData.driver(null);
        em.persist(driver);
        return driver;
    }

    public Passenger passenger() {
        Passenger passenger = TestData.passenger(null);
        em.persist(passenger);
        return passenger;
    }

    public Vehicle vehicle(Driver driver, int capacity) {
        Vehicle vehicle = TestData.vehicle(null, driver, capacity);
        em.persist(vehicle);
        return vehicle;
    }

    public Route route(Vehicle vehicle, int places, RouteStatus status, LocalDateTime start) {
        Route route = TestData.route(null, vehicle, places, status, start);
        em.persist(route);
        return route;
    }

    public Route route(Vehicle vehicle, int places, RouteStatus status, LocalDateTime start,
                       String[] cities, String[] prices, long[] minutes) {
        Route route = TestData.routeWith(null, vehicle, places, status, start, cities, prices, minutes);
        em.persist(route);
        return route;
    }

    public Reservation reservation(Passenger passenger, Route route, int boardingOrder, int descentOrder,
                                   int seats, ReservationStatus status, LocalDateTime date) {
        Reservation reservation = TestData.reservation(null, passenger, route, boardingOrder, descentOrder, seats, status);
        reservation.setReservationDate(date);
        em.persist(reservation);
        return reservation;
    }

    public void flushAndClear() {
        em.flush();
        em.clear();
    }
}

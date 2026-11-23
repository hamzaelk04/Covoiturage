package org.covoiturage.testutil;

import org.covoiturage.dto.route.RouteCreateRequest;
import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.covoiturage.entity.Driver;
import org.covoiturage.entity.Passenger;
import org.covoiturage.entity.Reservation;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.StagingPoint;
import org.covoiturage.entity.Vehicle;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.enums.RouteStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TestData {
    public static final String[] CITIES = {"Rabat", "Casablanca", "Settat", "Marrakech"};
    public static final String[] PRICES = {"0.00", "40.00", "70.00", "120.00"};
    public static final long[] MINUTES = {0, 75, 135, 270};

    private TestData() {
    }

    public static Driver driver(UUID id) {
        String suffix = suffix();
        return Driver.builder()
                .id(id)
                .firstname("Hamid")
                .lastname("Benali")
                .email("driver-" + suffix + "@mail.com")
                .phone("0661234567")
                .licence("PERMIS-" + suffix)
                .build();
    }

    public static Passenger passenger(UUID id) {
        String suffix = suffix();
        return Passenger.builder()
                .id(id)
                .firstname("Salma")
                .lastname("Idrissi")
                .email("passenger-" + suffix + "@mail.com")
                .phone("0663456789")
                .cin("AB" + ThreadLocalRandom.current().nextInt(100000, 1000000))
                .build();
    }

    public static Vehicle vehicle(Long id, Driver driver, int capacity) {
        return Vehicle.builder()
                .id(id)
                .brand("Dacia")
                .model("Logan")
                .registrationNumber("REG-" + suffix())
                .capacity(capacity)
                .driver(driver)
                .build();
    }

    public static Route route(Long id, Vehicle vehicle, int availablePlaces, RouteStatus status, LocalDateTime start) {
        return routeWith(id, vehicle, availablePlaces, status, start, CITIES, PRICES, MINUTES);
    }

    public static Route routeWith(Long id, Vehicle vehicle, int availablePlaces, RouteStatus status,
                                  LocalDateTime start, String[] cities, String[] prices, long[] minutes) {
        Route route = Route.builder()
                .id(id)
                .startCity(cities[0])
                .arrivalCity(cities[cities.length - 1])
                .startDate(start)
                .availablePlaces(availablePlaces)
                .status(status)
                .vehicle(vehicle)
                .build();
        for (int i = 0; i < cities.length; i++) {
            Long pointId = id == null ? null : (long) (i + 1);
            route.getStagingPoints().add(point(pointId, route, cities[i], i, start.plusMinutes(minutes[i]), prices[i]));
        }
        return route;
    }

    public static StagingPoint point(Long id, Route route, String city, int order, LocalDateTime time, String price) {
        return StagingPoint.builder()
                .id(id)
                .city(city)
                .stopOrder(order)
                .passingTime(time)
                .price(new BigDecimal(price))
                .route(route)
                .build();
    }

    public static Reservation reservation(Long id, Passenger passenger, Route route, int boardingOrder,
                                          int descentOrder, int seats, ReservationStatus status) {
        StagingPoint boarding = route.getStagingPoints().get(boardingOrder);
        StagingPoint descent = route.getStagingPoints().get(descentOrder);
        BigDecimal amount = descent.getPrice()
                .subtract(boarding.getPrice())
                .multiply(BigDecimal.valueOf(seats))
                .setScale(2);
        return Reservation.builder()
                .id(id)
                .numberOfSeats(seats)
                .totalAmount(amount)
                .reservationDate(LocalDateTime.now())
                .status(status)
                .passenger(passenger)
                .boardingPoint(boarding)
                .descentPoint(descent)
                .build();
    }

    public static List<StagingPointRequest> pointRequests(LocalDateTime start) {
        List<StagingPointRequest> requests = new ArrayList<>();
        for (int i = 0; i < CITIES.length; i++) {
            requests.add(StagingPointRequest.builder()
                    .city(CITIES[i])
                    .stopOrder(i)
                    .passingTime(start.plusMinutes(MINUTES[i]))
                    .price(new BigDecimal(PRICES[i]))
                    .build());
        }
        return requests;
    }

    public static RouteCreateRequest routeCreateRequest(Long vehicleId, int places, LocalDateTime start) {
        RouteCreateRequest request = new RouteCreateRequest();
        request.setVehicleId(vehicleId);
        request.setAvailablePlaces(places);
        request.setStagingPoints(pointRequests(start));
        return request;
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}

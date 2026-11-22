package org.covoiturage.specification;

import jakarta.persistence.criteria.*;
import org.covoiturage.dto.route.RouteSearchCriteria;
import org.covoiturage.entity.Reservation;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.StagingPoint;
import org.covoiturage.enums.ReservationStatus;
import org.covoiturage.enums.RouteStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.springframework.util.StringUtils.hasText;

public final class RouteSpecification {

    private RouteSpecification() {
    }

    public static Specification<Route> search(RouteSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), RouteStatus.PUBLISHED));

            Join<Route, StagingPoint> from = null;
            Join<Route, StagingPoint> to = null;

            if (hasText(criteria.getStartCity())) {
                from = root.join("stagingPoints");
                predicates.add(cb.equal(cb.lower(from.<String>get("city")), normalize(criteria.getStartCity())));
            }

            if (hasText(criteria.getArrivalCity())) {
                to = root.join("stagingPoints");
                predicates.add(cb.equal(cb.lower(to.<String>get("city")), normalize(criteria.getArrivalCity())));
            }

            if (from != null && to != null) {
                predicates.add(cb.lessThan(from.<Integer>get("stopOrder"), to.<Integer>get("stopOrder")));

                if (criteria.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            cb.diff(to.<BigDecimal>get("price"), from.<BigDecimal>get("price")),
                            criteria.getMaxPrice()));
                }
            }

            if (criteria.getDate() != null) {
                LocalDateTime dayStart = criteria.getDate().atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1);
                Path<LocalDateTime> departure = from != null
                        ? from.<LocalDateTime>get("passingTime")
                        : root.<LocalDateTime>get("startDate");
                predicates.add(cb.greaterThanOrEqualTo(departure, dayStart));
                predicates.add(cb.lessThan(departure, dayEnd));
            }

            int seats = criteria.getSeats() == null ? 1 : criteria.getSeats();
            predicates.add(enoughPlaces(root, query, cb, seats));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate enoughPlaces(Root<Route> root, CriteriaQuery<?> query,
                                          CriteriaBuilder cb, int seats) {
        Subquery<Long> booked = query.subquery(Long.class);
        Root<Reservation> reservation = booked.from(Reservation.class);
        booked.select(cb.coalesce(cb.sumAsLong(reservation.<Integer>get("numberOfSeats")), 0L));
        booked.where(
                cb.equal(reservation.get("boardingPoint").get("route").get("id"), root.get("id")),
                cb.equal(reservation.get("status"), ReservationStatus.CONFIRMED));

        return cb.greaterThanOrEqualTo(
                cb.diff(cb.toLong(root.<Integer>get("availablePlaces")), booked),
                Long.valueOf(seats));
    }

    private static String normalize(String city) {
        return city.trim().toLowerCase(Locale.ROOT);
    }
}
package org.covoiturage.mapper;

import org.covoiturage.dto.reservation.ReservationResponse;
import org.covoiturage.entity.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    @Mapping(target = "passengerId", source = "passenger.id")
    @Mapping(target = "routeId", source = "boardingPoint.route.id")
    @Mapping(target = "boardingPointId", source = "boardingPoint.id")
    @Mapping(target = "boardingCity", source = "boardingPoint.city")
    @Mapping(target = "descentPointId", source = "descentPoint.id")
    @Mapping(target = "descentCity", source = "descentPoint.city")
    @Mapping(target = "remainingPlaces", ignore = true)
    ReservationResponse toResponse(Reservation reservation);

    List<ReservationResponse> toResponses(List<Reservation> reservations);
}
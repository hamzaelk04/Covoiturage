package org.covoiturage.mapper;

import org.covoiturage.dto.route.RouteResponse;
import org.covoiturage.entity.Route;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = StagingPointMapper.class)
public interface RouteMapper {
    @Mapping(target = "vehicleId", source = "vehicle.id")
    RouteResponse toResponse(Route route);

    List<RouteResponse> toResponses(List<Route> routes);
}

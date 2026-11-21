package org.covoiturage.mapper;

import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.covoiturage.dto.stagingPoint.StagingPointResponse;
import org.covoiturage.entity.StagingPoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StagingPointMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "route", ignore = true)
    StagingPoint toEntity(StagingPointRequest request);

    StagingPointResponse toResponse(StagingPoint stagingPoint);

    List<StagingPointResponse> toResponses(List<StagingPoint> stagingPoints);
}

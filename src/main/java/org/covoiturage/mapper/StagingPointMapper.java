package org.covoiturage.mapper;

import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.covoiturage.dto.stagingPoint.StagingPointResponse;
import org.covoiturage.dto.stagingPoint.StagingPointUpdateRequest;
import org.covoiturage.entity.StagingPoint;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StagingPointMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "route", ignore = true)
    StagingPoint toEntity(StagingPointRequest request);

    StagingPointResponse toResponse(StagingPoint stagingPoint);

    List<StagingPointResponse> toResponses(List<StagingPoint> stagingPoints);

    StagingPointRequest toRequest(StagingPoint stagingPoint);

    @BeanMapping(
            builder = @Builder(disableBuilder = true),
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stopOrder", ignore = true)
    @Mapping(target = "route", ignore = true)
    void updateEntity(StagingPointUpdateRequest request, @MappingTarget StagingPoint stagingPoint);
}

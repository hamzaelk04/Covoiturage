package org.covoiturage.mapper;

import org.covoiturage.dto.vehicle.VehicleCreateRequest;
import org.covoiturage.dto.vehicle.VehicleResponse;
import org.covoiturage.dto.vehicle.VehicleSummaryResponse;
import org.covoiturage.dto.vehicle.VehicleUpdateRequest;
import org.covoiturage.entity.Vehicle;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleMapper {
    //    VEHICLE REQUEST
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "routes", ignore = true)
    Vehicle toEntity(VehicleCreateRequest request);

    //    VEHICLE RESPONSE
    @Mapping(source = "driver.id", target = "driverId")
    VehicleResponse toResponse(Vehicle vehicle);

    //    VEHICLE UPDATE
    @BeanMapping(
            builder = @Builder(disableBuilder = true),
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "routes", ignore = true)
    void updateEntity(VehicleUpdateRequest request, @MappingTarget Vehicle vehicle);

    default VehicleSummaryResponse toSummary(Vehicle vehicle) {
        return VehicleSummaryResponse.builder()
                .id(vehicle.getId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .registrationNumber(vehicle.getRegistrationNumber())
                .capacity(vehicle.getCapacity())
                .build();
    }

    default VehicleResponse updateAndMap(VehicleUpdateRequest request, Vehicle vehicle) {
        updateEntity(request, vehicle);
        return toResponse(vehicle);
    }
}

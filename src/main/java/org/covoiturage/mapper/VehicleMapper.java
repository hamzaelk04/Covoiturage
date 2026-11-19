package org.covoiturage.mapper;

import org.covoiturage.dto.vehicle.VehicleCreateRequest;
import org.covoiturage.dto.vehicle.VehicleResponse;
import org.covoiturage.dto.vehicle.VehicleSummaryResponse;
import org.covoiturage.entity.Vehicle;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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

    default VehicleSummaryResponse toSummary(Vehicle vehicle) {
        return VehicleSummaryResponse.builder()
                .id(vehicle.getId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .registrationNumber(vehicle.getRegistrationNumber())
                .capacity(vehicle.getCapacity())
                .build();
    }
}

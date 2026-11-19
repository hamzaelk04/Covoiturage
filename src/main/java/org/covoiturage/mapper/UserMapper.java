package org.covoiturage.mapper;

import org.covoiturage.dto.driver.DriverRequest;
import org.covoiturage.dto.passenger.PassengerRequest;
import org.covoiturage.dto.user.UserResponse;
import org.covoiturage.entity.Driver;
import org.covoiturage.entity.Passenger;
import org.covoiturage.entity.abstracts.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
//    PASSENGER REQUEST
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    Passenger toEntity(PassengerRequest request);

//    DRIVER REQUEST
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    Driver toEntity(DriverRequest request);

//    PASSENGER RESPONSE
    @Mapping(target = "role", constant = "PASSENGER")
    @Mapping(target = "licence", ignore = true)
    UserResponse passengerToResponse(Passenger passenger);

//    LICENCE RESPONSE
    @Mapping(target = "role", constant = "DRIVER")
    @Mapping(target = "cin", ignore = true)
    UserResponse driverToResponse(Driver driver);

    default UserResponse toResponse (User u) {
        if (u instanceof Passenger p) return passengerToResponse(p);
        if (u instanceof Driver d) return driverToResponse(d);
        throw new IllegalArgumentException("Type d'utilisateur inconnu : " + u.getClass());
    }

//    UPDATES
//    UPDATE PASSENGER
    void updatePassenger();
}

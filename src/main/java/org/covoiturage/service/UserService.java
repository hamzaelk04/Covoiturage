package org.covoiturage.service;

import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.driver.DriverRequest;
import org.covoiturage.dto.passenger.PassengerRequest;
import org.covoiturage.dto.user.UserResponse;
import org.covoiturage.entity.Driver;
import org.covoiturage.entity.Passenger;
import org.covoiturage.mapper.UserMapper;
import org.covoiturage.repository.DriverRepository;
import org.covoiturage.repository.PassengerRepository;
import org.covoiturage.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PassengerRepository passengerRepository;
    private final UserMapper userMapper;

//    CREATE PASSENGER
    @Transactional
    public UserResponse createPassenger(PassengerRequest request) {
        String email = checkEmail(request.getEmail());
        Passenger passenger = userMapper.toEntity(request);

        passenger.setEmail(email);
        passenger.setFirstname(request.getFirstname().trim());
        passenger.setLastname(request.getLastname().trim());

        return userMapper.toResponse(passengerRepository.save(passenger));
    }

//    CREATE THE DRIVER
    @Transactional
    public UserResponse createDriver(DriverRequest request) {
        String email = checkEmail(request.getEmail());

        Driver driver = userMapper.toEntity(request);

        driver.setEmail(email);
        driver.setFirstname(request.getFirstname().trim());
        driver.setLastname(request.getLastname().trim());

        return userMapper.toResponse(driverRepository.save(driver));
    }


//    HELPERS
    private String checkEmail(String newEmail) {
        String email = newEmail.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email existe déjà");
        }

        return email;
    }
}

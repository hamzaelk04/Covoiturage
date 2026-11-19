package org.covoiturage.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.driver.DriverRequest;
import org.covoiturage.dto.passenger.PassengerRequest;
import org.covoiturage.dto.user.UpdateUserRequest;
import org.covoiturage.dto.user.UserResponse;
import org.covoiturage.entity.Driver;
import org.covoiturage.entity.Passenger;
import org.covoiturage.entity.abstracts.User;
import org.covoiturage.mapper.UserMapper;
import org.covoiturage.repository.DriverRepository;
import org.covoiturage.repository.PassengerRepository;
import org.covoiturage.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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

    //    UPDATE THE DRIVER AND PASSENGER
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findUserOrThrow(id);

        if (request.getFirstname() != null) {
            user.setFirstname(request.getFirstname().trim());
        }
        if (request.getLastname() != null) {
            user.setLastname(request.getLastname().trim());
        }
        if (request.getEmail() != null) {
            user.setEmail(checkEmail(request.getEmail().trim().toLowerCase(Locale.ROOT)));

        }

        if (user instanceof Passenger passenger) {
            rejectIfPresent(request.getLicence(), "licence", "PASSENGER");

            if (request.getCin() != null && !request.getCin().isBlank()) {
                passenger.setCin(request.getCin());
            }

            userMapper.updatePassenger(request, passenger);
        }

        if (user instanceof Driver driver) {
            rejectIfPresent(request.getCin(), "cin", "DRIVER");

            if (request.getLicence() != null) {
                driver.setLicence(request.getLicence());
            }

            userMapper.updateDriver(request, driver);
        }

        return userMapper.toResponse(user);
    }

    //    READ
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findUserOrThrow(id));
    }

    //    HELPERS
    private String checkEmail(String newEmail) {
        String email = newEmail.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
//            ADD THE EXIST EMAIL EXCEPTION
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email existe déjà");
        }

        return email;
    }

    private void rejectIfPresent(String value, String field, String role) {
        if (value != null && !value.isBlank()) {
            throw new ValidationException(
                    "Le champ '" + field + "' n'est pas applicable pour " + role);
        }
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Utilisateur" + id));
    }

    //    HELPER FOR VEHICLES AND RESERVATIONS
    @Transactional(readOnly = true)
    public Passenger getPassengerEntity(UUID id) {
        return passengerRepository.findById(id)
//                add the resource not found exception
                .orElseThrow(() -> new RuntimeException("passager" + id));
    }

    @Transactional(readOnly = true)
    public Driver getDriverEntity(UUID id) {
        return driverRepository.findById(id)
//                add the resource not found exception
                .orElseThrow(() -> new RuntimeException("conducteur" + id));
    }
}

package org.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.covoiturage.dto.driver.DriverRequest;
import org.covoiturage.dto.passenger.PassengerRequest;
import org.covoiturage.dto.user.UserResponse;
import org.covoiturage.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController @RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestion des utilisateurs (client, organisateur, agent, admin)")
public class UserController {
    private final UserService userService;

//    CREATE PASSENGER
    @PostMapping("/passengers")
    @Operation(summary = "Create a passenger")
    public ResponseEntity<UserResponse> createPassenger(@Valid @RequestBody PassengerRequest request) {
        return created(userService.createPassenger(request));
    }

//    CREATE DRIVER
    @PostMapping("/drivers")
    @Operation(summary = "Create a driver")
    public ResponseEntity<UserResponse> createDriver(@Valid @RequestBody DriverRequest request) {
        return created(userService.createDriver(request));
    }

//    HELPER
    private ResponseEntity<UserResponse> created(UserResponse body) {
        return ResponseEntity
                .created(URI.create("/api/users/" + body.getId()))
                .body(body);
    }
}

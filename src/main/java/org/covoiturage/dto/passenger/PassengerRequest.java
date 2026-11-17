package org.covoiturage.dto.passenger;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.covoiturage.dto.user.UserRegisterRequest;

@Getter @Setter
public class PassengerRequest extends UserRegisterRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{1,2}\\d{5,6}$", message = "Invalid CIN")
    private String cin;
}

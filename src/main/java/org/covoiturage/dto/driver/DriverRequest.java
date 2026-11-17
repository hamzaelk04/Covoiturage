package org.covoiturage.dto.driver;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.covoiturage.dto.user.UserRegisterRequest;

@Getter @Setter
public class DriverRequest extends UserRegisterRequest {
    @NotBlank(message = "Le permis est obligatoire")
    private String licence;
}

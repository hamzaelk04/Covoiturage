package org.covoiturage.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class UpdateUserRequest extends UserRegisterRequest{
    @NotBlank(message = "Le permis est obligatoire")
    private String licence;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{1,2}\\d{5,6}$", message = "Invalid CIN")
    private String cin;
}

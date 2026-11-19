package org.covoiturage.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
public class UpdateUserRequest {

    @Size(max = 100, message = "Le prenom ne doit pas dépasser 100 caractères")
    private String firstname;

    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String lastname;

    @Email(message = "La forme d'email est invalide")
    @Size(max = 150, message = "L'email ne doit pas dépasser 150 caractères")
    private String email;

    @Pattern(regexp = "^(\\+212|0)[5-7]\\d{8}$", message = "Numéro de téléphone marocain invalide")
    private String phone;

    private String licence;

    @Pattern(regexp = "^[A-Za-z]{1,2}\\d{5,6}$", message = "Invalid CIN")
    private String cin;
}

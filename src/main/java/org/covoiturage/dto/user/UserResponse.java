package org.covoiturage.dto.user;

import lombok.*;
import org.covoiturage.enums.Role;

import java.util.UUID;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String cin;
    private String licence;
    private Role role;
}

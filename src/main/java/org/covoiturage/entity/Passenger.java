package org.covoiturage.entity;

import jakarta.persistence.*;
import lombok.*;
import org.covoiturage.entity.abstracts.User;

@Entity
@Table(name = "passengers")
@Getter @Setter @SuperBuilder @NoArgsConstructor
public class Passenger extends User {

    @Column(nullable = false)
    private String cin;

}
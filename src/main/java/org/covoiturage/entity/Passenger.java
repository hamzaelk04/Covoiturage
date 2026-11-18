package org.covoiturage.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.covoiturage.entity.abstracts.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "passengers")
@Getter @Setter @SuperBuilder @NoArgsConstructor
public class Passenger extends User {

    @Column(nullable = false, unique = true)
    private String cin;

    @OneToMany(mappedBy = "passenger", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();
}
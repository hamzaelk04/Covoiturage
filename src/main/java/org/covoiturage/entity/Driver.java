package org.covoiturage.entity;

import jakarta.persistence.*;
import org.covoiturage.entity.abstracts.User;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drivers")
@Getter @Setter @SuperBuilder @NoArgsConstructor
public class Driver extends User{

    @Column(nullable = false)
    private String licence;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vehicle> vehicles = new ArrayList<>();
}

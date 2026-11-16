package org.covoiturage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SuperBuilder;
import org.covoiturage.entity.abstracts.User;

@Entity
@Table(name = "passengers")
@Getter @Setter @SuperBuilder @NoArgsConstructor
public class Passenger extends User {

    @Column(nullable = false)
    private String cin;
}

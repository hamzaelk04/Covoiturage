package org.covoiturage.entity;

import org.covoiturage.entity.abstracts.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drivers")
@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
public class Driver extends User{

    @Column(nullable = false)
    private String licence;
}

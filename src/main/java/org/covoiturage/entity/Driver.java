package org.covoiturage.entity;

import org.covoiturage.entity.abstracts.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "drivers")
@Getter @Setter @SuperBuilder @NoArgsConstructor
public class Driver extends User{

    @Column(nullable = false)
    private String licence;
}

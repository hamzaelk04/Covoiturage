package org.covoiturage.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mark;

    private String model;

    private String matriculation;

    private int capacity;

//    Add the driver
}

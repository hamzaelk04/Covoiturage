package org.covoiturage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_city")
    private String starCity;


    @Column(name = "arrival_city")
    private String arrivalCity;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "available_places")
    private int availablePlaces;

//    Add the status enum
//    Add the vehicle
//    Add the list of stagingPoints
}

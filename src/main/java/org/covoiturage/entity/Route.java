package org.covoiturage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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

    @Column(name = "start_city", nullable = false)
    private String startCity;


    @Column(name = "arrival_city", nullable = false)
    private String arrivalCity;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "available_places")
    @Min(1)
    private int availablePlaces;

//    Add the status enum
//    Add the vehicle
//    Add the list of stagingPoints
}

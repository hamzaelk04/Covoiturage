package org.covoiturage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "staging_points")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StagingPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    private int order;

    @Column(name = "passing_time")
    private LocalDateTime passingTime;

    private BigDecimal price;

//    Add route
}

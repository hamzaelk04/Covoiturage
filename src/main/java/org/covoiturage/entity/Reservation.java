package org.covoiturage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "boarding_point")
    private String boardingPoint;

    @Column(name = "descent_point")
    private String descentPoint;

    private int capacity;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "reservation_date")
    private LocalDateTime reservationDate;

//    Add reservation status
//    Add passenger
//    Add stagingPoint
}

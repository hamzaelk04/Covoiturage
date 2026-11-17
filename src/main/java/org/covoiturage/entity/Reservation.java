package org.covoiturage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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

    @Column(name = "number_of_seats")
    @Min(1)
    private int numberOfSeats;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "reservation_date", nullable = false)
    private LocalDateTime reservationDate;

//    Add reservation status
//    Add passenger
//    Add stagingPoint
}

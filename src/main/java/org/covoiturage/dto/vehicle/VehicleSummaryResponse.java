package org.covoiturage.dto.vehicle;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleSummaryResponse {
    private Long id;
    private String brand;
    private String model;
    private String registrationNumber;
    private int capacity;
}

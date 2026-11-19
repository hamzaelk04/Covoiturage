package org.covoiturage.dto.vehicle;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter @Setter
public class VehicleResponse {
    private Long id;
    private UUID driverId;
    private String brand;
    private String model;
    private String registrationNumber;
    private int capacity;
//    Add the list route response
}

package vehicle_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name="vehicle")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Vehicle {
    @Id
    private String id;
    private String make;
    private String brand;
    private String model;
    private int year;
    private String plateNumber;
    private BigDecimal dailyRate;
    private int mileage;
    private String color;
    private String imageUrl;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

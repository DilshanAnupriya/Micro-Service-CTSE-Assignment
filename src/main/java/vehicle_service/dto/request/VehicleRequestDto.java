package vehicle_service.dto.request;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class VehicleRequestDto {
    private String make;
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

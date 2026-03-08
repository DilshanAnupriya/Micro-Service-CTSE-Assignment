package vehicle_service.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Builder
public class VehicleResponseDto {
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

package vehicle_service.kafka;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable event payload published to Kafka whenever a vehicle changes.
 * Keep this class stable — consuming services depend on its shape.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleEvent {

    /** One of: CREATED | UPDATED | DELETED */
    private String eventType;

    private String vehicleId;
    private String make;
    private String brand;
    private String model;
    private int    year;
    private String plateNumber;
    private BigDecimal dailyRate;
    private int    mileage;
    private String color;
    private String imageUrl;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime eventTimestamp;
}

package vehicle_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import vehicle_service.entity.Vehicle;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes vehicle domain events to Kafka topics.
 * Injected into VehicleServiceImpl to fire-and-forget after each mutation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleEventProducer {

    private final KafkaTemplate<String, VehicleEvent> kafkaTemplate;

    @Value("${app.kafka.topic.vehicle-created}")
    private String vehicleCreatedTopic;

    @Value("${app.kafka.topic.vehicle-updated}")
    private String vehicleUpdatedTopic;

    @Value("${app.kafka.topic.vehicle-deleted}")
    private String vehicleDeletedTopic;

    public void publishVehicleCreated(Vehicle vehicle) {
        publish(vehicleCreatedTopic, "CREATED", vehicle);
    }

    public void publishVehicleUpdated(Vehicle vehicle) {
        publish(vehicleUpdatedTopic, "UPDATED", vehicle);
    }

    public void publishVehicleDeleted(String vehicleId) {
        VehicleEvent event = VehicleEvent.builder()
                .eventType("DELETED")
                .vehicleId(vehicleId)
                .eventTimestamp(LocalDateTime.now())
                .build();
        send(vehicleDeletedTopic, vehicleId, event);
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private void publish(String topic, String eventType, Vehicle vehicle) {
        VehicleEvent event = toEvent(eventType, vehicle);
        send(topic, vehicle.getId(), event);
    }

    private void send(String topic, String key, VehicleEvent event) {
        CompletableFuture<SendResult<String, VehicleEvent>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] Failed to publish {} event for vehicle {}: {}",
                        event.getEventType(), event.getVehicleId(), ex.getMessage());
            } else {
                log.info("[Kafka] Published {} event → topic={} partition={} offset={}",
                        event.getEventType(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    private VehicleEvent toEvent(String eventType, Vehicle v) {
        return VehicleEvent.builder()
                .eventType(eventType)
                .vehicleId(v.getId())
                .make(v.getMake())
                .brand(v.getBrand())
                .model(v.getModel())
                .year(v.getYear())
                .plateNumber(v.getPlateNumber())
                .dailyRate(v.getDailyRate())
                .mileage(v.getMileage())
                .color(v.getColor())
                .imageUrl(v.getImageUrl())
                .description(v.getDescription())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .eventTimestamp(LocalDateTime.now())
                .build();
    }
}

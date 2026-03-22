package vehicle_service.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topics so they are auto-created on startup.
 * In production (Amazon MSK) topics may be pre-created; these beans are idempotent.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.vehicle-created}")
    private String vehicleCreatedTopic;

    @Value("${app.kafka.topic.vehicle-updated}")
    private String vehicleUpdatedTopic;

    @Value("${app.kafka.topic.vehicle-deleted}")
    private String vehicleDeletedTopic;

    @Bean
    public NewTopic vehicleCreatedTopic() {
        return TopicBuilder.name(vehicleCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic vehicleUpdatedTopic() {
        return TopicBuilder.name(vehicleUpdatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic vehicleDeletedTopic() {
        return TopicBuilder.name(vehicleDeletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

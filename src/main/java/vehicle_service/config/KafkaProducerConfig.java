package vehicle_service.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import vehicle_service.kafka.VehicleEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Explicit Kafka producer configuration.
 * Required because Spring Boot 4.x auto-configures KafkaTemplate&lt;Object, Object&gt;
 * which does not satisfy the typed injection point KafkaTemplate&lt;String, VehicleEvent&gt;.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, VehicleEvent> vehicleEventProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Do not include type headers so consumers don't need to know the exact class
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, VehicleEvent> kafkaTemplate(
            ProducerFactory<String, VehicleEvent> vehicleEventProducerFactory) {
        return new KafkaTemplate<>(vehicleEventProducerFactory);
    }
}

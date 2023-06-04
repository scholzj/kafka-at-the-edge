package cz.scholz;

import cz.scholz.model.SensorData;
import io.smallrye.common.annotation.Identifier;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.Map;

@ApplicationScoped
public class KafkaProviders {
    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> config;

    @Produces
    @SuppressWarnings("unchecked")
    KafkaConsumer<String, SensorData> getConsumer() {
        return new KafkaConsumer<String, SensorData>(config,
                new StringDeserializer(),
                new SensorData.SensorDataDeserializer());
    }
}
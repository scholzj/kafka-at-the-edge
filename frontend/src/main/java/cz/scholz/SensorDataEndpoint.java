package cz.scholz;

import cz.scholz.model.SensorData;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Path("/api/sensors")
public class SensorDataEndpoint {
    @ConfigProperty(name = "sensor.data.topic", defaultValue = "sensor-data")
    String sensorDataTopic;

    @Inject
    KafkaConsumer<String, SensorData> consumer;

    final Map<String, SensorData> sensorData = new HashMap<>();
    volatile boolean done = false;

    public void initialize(@Observes StartupEvent ev) {
        consumer.subscribe(Collections.singleton(sensorDataTopic));
        new Thread(() -> {
            while (!done) {
                final ConsumerRecords<String, SensorData> consumerRecords = consumer.poll(Duration.ofSeconds(1));

                consumerRecords.forEach(record -> {
                    sensorData.put(record.key(), record.value());
                    Log.debugf("Polled Record:(%s, %s, %d, %d)\n",
                            record.key(), record.value(),
                            record.partition(), record.offset());
                });
            }
            consumer.close();
        }).start();
    }

    public void terminate(@Observes ShutdownEvent ev) {
        done = true;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<SensorData> sensorData() {
        return sensorData.values();
    }
}
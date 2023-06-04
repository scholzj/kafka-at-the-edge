package cz.scholz;

import cz.scholz.model.SensorData;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/sensors")
public class SensorDataEndpoint {
    @ConfigProperty(name = "sensor.data.topic", defaultValue = "sensor-data")
    String sensorDataTopic;

    @Inject
    KafkaConsumer<String, SensorData> consumer;

    private final MeterRegistry registry;

    final Map<String, SensorData> sensorData = new HashMap<>();
    volatile boolean done = false;

    public SensorDataEndpoint(MeterRegistry registry) {
        this.registry = registry;
    }

    public void initialize(@Observes StartupEvent ev) {
        consumer.subscribe(Collections.singleton(sensorDataTopic));
        new Thread(() -> {
            while (!done) {
                final ConsumerRecords<String, SensorData> consumerRecords = consumer.poll(Duration.ofSeconds(1));

                consumerRecords.forEach(record -> {
                    handleNewSensorData(record.key(), record.value());
                    Log.debugf("Polled Record:(%s, %s, %d, %d)\n",
                            record.key(), record.value(),
                            record.partition(), record.offset());
                });
            }
            consumer.close();
        }).start();
    }

    private void handleNewSensorData(String key, SensorData value)  {
        // Add the data to the map queried by the REST API
        sensorData.put(key, value);

        // Add the record as a Prometheus metric
        List<Tag> tags = List.of(Tag.of("long", String.valueOf(value.getLongitude())), Tag.of("lat", String.valueOf(value.getLatitude())));
        registry.gauge("sensor.data.temperature", tags, sensorData, d -> d.get(key).getTemperature());
        registry.gauge("sensor.data.humidity", tags, sensorData, d -> d.get(key).getHumidity());
        registry.gauge("sensor.data.pressure", tags, sensorData, d -> d.get(key).getPressure());
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
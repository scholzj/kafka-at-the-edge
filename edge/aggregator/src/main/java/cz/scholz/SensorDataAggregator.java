package cz.scholz;

import cz.scholz.model.AggregatedSensorData;
import cz.scholz.model.AggregatedSensorDataSerde;
import cz.scholz.model.SensorDataSerde;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class SensorDataAggregator {
    @ConfigProperty(name = "sensor.data.source.topic", defaultValue = "sensor-data")
    String sourceSensorDataTopic;

    @ConfigProperty(name = "sensor.data.aggregated.topic", defaultValue = "sensor-data-aggregated")
    String aggregatedSensorDataTopic;

    @Produces
    public Topology streamsTopology() {
        SensorDataSerde sensorDataSerde = new SensorDataSerde();
        AggregatedSensorDataSerde aggregatedSensorDataSerde = new AggregatedSensorDataSerde();
        StreamsBuilder builder = new StreamsBuilder();

        builder.stream(sourceSensorDataTopic, Consumed.with(Serdes.String(), sensorDataSerde))
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
                .aggregate(
                        AggregatedSensorData::new,
                        (aggKey, newValue, oldValue) -> oldValue.aggregate(newValue),
                        Materialized.<String, AggregatedSensorData, WindowStore<Bytes, byte[]>>as("aggregated-sensor-data")
                                .withValueSerde(aggregatedSensorDataSerde)
                )
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map((k, v) -> new KeyValue<>(k.key(), v.result()))
                .peek((k, v) -> Log.infov("Aggregated sensor data: {0}", v))
                .to(aggregatedSensorDataTopic, Produced.with(Serdes.String(), sensorDataSerde));

        return builder.build();
    }
}
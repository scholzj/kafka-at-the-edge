package cz.scholz.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class AggregatedSensorDataSerde implements Serde<AggregatedSensorData> {
    private final ObjectMapper mapper;

    private final AggregatedSensorDataSerializer serializer;
    private final AggregatedSensorDataDeserializer deserializer;

    public AggregatedSensorDataSerde() {
        mapper = new ObjectMapper();
        serializer = new AggregatedSensorDataSerializer();
        deserializer = new AggregatedSensorDataDeserializer();
    }

    @Override
    public Serializer<AggregatedSensorData> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<AggregatedSensorData> deserializer() {
        return deserializer;
    }

    final public class AggregatedSensorDataSerializer implements Serializer<AggregatedSensorData> {
        @Override
        public byte[] serialize(String topic, AggregatedSensorData data) {
            try {
                return mapper.writeValueAsBytes(data);
            } catch (JsonProcessingException e) {
                Log.errorv("Failed to serialize SensorData {0}", data, e);
                throw new RuntimeException("Failed to serialize SensorData", e);
            }
        }
    }

    final public class AggregatedSensorDataDeserializer implements Deserializer<AggregatedSensorData> {
        @Override
        public AggregatedSensorData deserialize(String s, byte[] bytes) {
            try {
                return mapper.readValue(bytes, AggregatedSensorData.class);
            } catch (Exception e) {
                Log.error("Filed to deserializer SensorData", e);
                throw new RuntimeException("Failed to serialize SensorData", e);
            }
        }
    }
}

package cz.scholz.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class SensorDataSerde implements Serde<SensorData> {
    private final ObjectMapper mapper;

    private final SensorDataSerializer serializer;
    private final SensorDataDeserializer deserializer;

    public SensorDataSerde() {
        mapper = new ObjectMapper();
        serializer = new SensorDataSerializer();
        deserializer = new SensorDataDeserializer();
    }

    @Override
    public Serializer<SensorData> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<SensorData> deserializer() {
        return deserializer;
    }

    final public class SensorDataSerializer implements Serializer<SensorData> {
        @Override
        public byte[] serialize(String topic, SensorData data) {
            try {
                return mapper.writeValueAsBytes(data);
            } catch (JsonProcessingException e) {
                Log.errorv("Failed to serialize SensorData {0}", data, e);
                throw new RuntimeException("Failed to serialize SensorData", e);
            }
        }
    }

    final public class SensorDataDeserializer implements Deserializer<SensorData> {
        @Override
        public SensorData deserialize(String s, byte[] bytes) {
            try {
                return mapper.readValue(bytes, SensorData.class);
            } catch (Exception e) {
                Log.error("Filed to deserializer SensorData", e);
                throw new RuntimeException("Failed to serialize SensorData", e);
            }
        }
    }
}

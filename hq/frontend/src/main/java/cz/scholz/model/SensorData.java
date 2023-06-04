package cz.scholz.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class SensorData {
    double latitude;
    double longitude;
    String timestamp;

    double temperature;
    double humidity;
    double pressure;

    public SensorData() {
    }

    public SensorData(double latitude, double longitude, String timestamp, double temperature, double humidity, double pressure) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public static class SensorDataDeserializer implements Deserializer {
        @Override
        public Object deserialize(String s, byte[] bytes) {
            ObjectMapper mapper = new ObjectMapper();
            SensorData obj = null;

            try {
                obj = mapper.readValue(bytes, SensorData.class);
            } catch (Exception e) {
                Log.error("Failed to deserialize sensor data", e);
            }

            return obj;
        }
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", timestamp='" + timestamp + '\'' +
                ", temperature='" + temperature + '\'' +
                ", humidity='" + humidity + '\'' +
                ", pressure='" + pressure + '\'' +
                '}';
    }
}

package cz.scholz.model;

import io.quarkus.logging.Log;

public class AggregatedSensorData extends SensorData {
    int aggregatedEventsCount = 0;

    public AggregatedSensorData() {
        super();
    }

    public AggregatedSensorData aggregate(SensorData data)    {
        if (aggregatedEventsCount == 0) {
            this.latitude = data.latitude;
            this.longitude = data.longitude;
            this.timestamp = data.timestamp;
            this.temperature = data.temperature;
            this.humidity = data.humidity;
            this.pressure = data.pressure;
        } else {
            this.timestamp = data.timestamp;
            this.temperature = updateAverage(temperature, data.temperature);
            this.humidity = updateAverage(humidity, data.humidity);
            this.pressure = updateAverage(pressure, data.pressure);
        }

        aggregatedEventsCount++;

        return this;
    }

    private double updateAverage(double oldValue, double newValue)   {
        return (aggregatedEventsCount * oldValue + newValue) / (aggregatedEventsCount + 1);
    }

    public SensorData result()  {
        Log.info("Returning SensorDataAverage aggregation result");
        return (SensorData) this;
    }

    @Override
    public String toString() {
        return "AggregatedSensorData{" +
                "aggregatedEventsCount=" + aggregatedEventsCount +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", timestamp='" + timestamp + '\'' +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", pressure=" + pressure +
                '}';
    }
}

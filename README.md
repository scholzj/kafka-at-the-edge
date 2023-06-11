# Apache Kafka at the Edge

This repository contains my demo of [Apache Kafka®](https://kafka.apache.org/) running at the edge.
The demo shows how you can run Kubernetes with [a Strimzi-based](https://strimzi.io/) Apache Kafka cluster at the edge.
This cluster collects data from sensor devices running locally and does local processing.
The processed data are synced into the central Kafka cluster where they are collected and aggregated from multiple different edge locations.

## Slides

TODO: Slides do not yet exist.

### Images

The images used through the slides are referenced in the [`IMAGES.md` file](./IMAGES.md).

## My Environment

### IoT Sensors

The code for the IoT device with sensors is in the [`iot/esp32` directory](./iot/esp32).
In my case, the IoT device is the LOLIN32 Lite board with the BME280 sensor for temperature, humidity and atmospheric pressure.
It is written for my particular device, but it should in general be compatible with other ESP32 boards as well.

The application is written using [MicroPython](https://micropython.org/) and does the following:
* Connects to the WiFi
* Syncs the time from the NTP servers
* Periodically (every second) collects the data from the sensors, formats them to JSON, and pushes them using HTTP to the Kafka cluster.

The JSON message looks like this:
```json
{
    "temperature": 20.57,
    "timestamp": "2022-09-17 18:48:56",
    "longitude": 14.4378,
    "humidity": 54.87,
    "pressure": 981.59,
    "latitude": 50.0755
}
```

The `latitude` and `longitude` is the location of the sensor used to show it on the map.
The `timestamp` is the time when the data were taken.
And the remaining fields are taken from the sensor.

### Edge cluster

The Edge cluster is where the IoT device connects.
It is running an Apache Kafka cluster using the [Strimzi operator](https://strimzi.io).
It also runs the [Strimzi Kafka Bridge](https://github.com/strimzi/strimzi-kafka-bridge).
The IoT device sends the data using HTTP to the Bridge which passes them to the Kafka brokers.
The Edge cluster also runs software based WiFi access point which is where the IoT device connects to send the data.

On the Edge cluster, the data are aggregated.
Using Kafka Streams API and Quarkus, we aggregate the per-second data to per-minute data to conserve bandwidth and resources.

From the Edge cluster, Mirror Maker is used to forward the aggregated data to the central (HQ) Kafka cluster.

My Edge cluster is based on Raspberry Pi 4 with 8GB of RAM each.
It consists of 1 master and 3 worker nodes.
Kubernetes is installed on top of it using [K3S](https://k3s.io/).
_(The cluster is not 100% utilized -> you can run this even with less nodes)_

### HQ cluster

The HQ cluster is the central cluster that collects the data from all the different edge locations for central processing and further aggregation.
Currently, the HQ cluster runs the following components:
* Apache Kafka cluster which collects the data from the edge locations
* Prometheus for collecting the data as time series
* A frontend application that shows the data from the sensors on a Google Map and exports them from Apache Kafka as a metric for Prometheus

The _frontend_ is a [Quarkus](https://quarkus.io/) based application that serves a website and an API.
The Quarkus backend connects to Kafka, gets the data from the sensors and serves them through an REST API.
The website is based on a [Google Charts](https://developers.google.com/chart/interactive/docs/gallery/map) based map.
It gets the sensor data from the REST API and shows them on the map.

![The _frontend_ application](./assets/frontend-map.png "The _frontend_ application")

_Note: If you want to get rid of the `For developer purposes only` message, you have to register for an API key._
_For more details about getting the API key, see: https://developers.google.com/chart/interactive/docs/basic_load_libs#load-settings_

The HQ cluster could run anywhere - from some local data centers up to the cloud-
For my demo purposes, my HQ cluster runs on a bare-metal-based Kubernetes cluster.

## Deploying the demo

The steps in this section show how to deploy the demo from the attached YAML files.
They expect a similar environment to mine.

On the HQ cluster:

1. Install Strimzi.
2. Deploy the Kafka cluster using the [`hq/01-kafka.yaml`](./hq/01-kafka.yaml) YAML file.
3. Create the topic for the aggregated sensor data using the [`hq/02-sensor-data-aggregated-topic.yaml`](./hq/02-sensor-data-aggregated-topic.yaml) YAML file.
4. Create the user which will be used to mirror the data from the edge using the [`hq/03-mirror-maker-user.yaml`](./hq/03-mirror-maker-user.yaml) YAML file.
5. Deploy the frontend application using [`hq/04-frontend.yaml`](./hq/04-frontend.yaml) YAML file.
   The source codes can be found in the [`hq/frontend/`](./hq/frontend/) directory.
   Adjust the Ingress address on which it is deployed according to your environment.
6. Create the Prometheus `PodMonitor` to get Prometheus to start scraping the metrics from the frontend application using the [`hq/05-prometheus-pod-monitor.yaml`](./hq/05-prometheus-pod-monitor.yaml) YAML file.

On the Edge cluster:

7. Install Strimzi.
8. Deploy the local WiFi access point using the [`edge/00-wifi-ap.yaml`](./edge/00-wifi-ap.yaml) YAML file.
   This access point will be where the IoT device will connect.
9. Deploy the Kafka cluster using the [`edge/01-kafka.yaml`](./edge/01-kafka.yaml) YAML file.
   The container image for the WiFi access point can be found in the [`edge/wifi-ap`](./edge/wifi-ap/) directory.
   The way how the Wifi access point works is based on [https://github.com/redhat-et/AI-for-edge-microshift-demo/blob/main/wifi-ap](https://github.com/redhat-et/AI-for-edge-microshift-demo/blob/main/wifi-ap).
10. Create the Kafka topics for the raw and aggregated sensor data using the [`edge/02-sensor-data-topic.yaml`](./edge/02-sensor-data-topic.yaml) and [`edge/03-sensor-data-aggregated-topic.yaml`](./edge/03-sensor-data-aggregated-topic.yaml) YAML files.
11. Deploy the Strimzi HTTP Bridge using the [`edge/04-bridge.yaml`](./edge/04-bridge.yaml) YAML file.
12. Copy the following certificate secrets from the HQ cluster:
    * The secret `my-cluster-cluster-ca-cert` in HQ cluster copy as `hq-cluster-ca-cert` in the Edge cluster
    * The secret `edge-mirror-maker` in HQ cluster copy as `hq-cluster-user` in the Edge cluster
    To copy them, you can get the YAML using `kubectl get secret ... -o yaml` and then apply this YAML on the Edge cluster.
    If you decide to use different names, make sure to update the Mirror Maker configuration
13. Deploy the Apache Kafka Mirror Maker using the [`edge/05-mirror-maker.yaml`](./edge/05-mirror-maker.yaml) YAML file.
14. Finally, deploy the aggregator using the [`edge/06-ggregator.yaml`](./edge/06-ggregator.yaml) YAML file.
    The aggregator application is based on Apache Kafka Streams API and on the Quarkus framework.
    The source codes can be found in the [`edge/aggregator/`](./edge/aggregator/) directory.
    The aggregator is reading the data produced by the IoT sensors from the Kafka cluster and calculates average values over 1-minute windows.
    The results of these calculations are sent to another topic, which is later mirrored by the Mirror Maker to the HQ cluster.

On the IoT device:

15. Upload the MicroPython-based application to your IoT device.
    The source codes can be found in the [`iot/esp32/`](./iot/esp32/) directory.
    Before uploading it, make sure to update the WiFi credentials if needed and the address of the Strimzi HTTP Bridge running on the Edge cluster.


Once running:

16. You can check the messages being received from the sensor using the following command run on the edge cluster:
    ```
    kubectl run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.35.1-kafka-3.4.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic sensor-data
    ```
17. And the same for the aggregated data:
    ```
    kubectl run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.35.1-kafka-3.4.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic sensor-data-aggregated
    ```
18. For the aggregated topic, you can do the same as well on the HQ cluster where you can confirm that the data are properly mirrored.
    ```
    kubectl run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.35.1-kafka-3.4.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic sensor-data-aggregated
    ```
19. You can also check the map provided by the frontend application as well as the Prometheus with the historical data.
# WiFi Access Point

**Based on https://github.com/redhat-et/AI-for-edge-microshift-demo/blob/main/wifi-ap**

This container creates a WiFi access point that can be run as a Pod on your Raspberry Pi 4 based Kubernetes cluster.
It is used to provide connectivity between the IoT devices and the Kafka cluster running at the edge.

The default (hidden) SSID is `KAFKA_AT_THE_EDGE` with password `kafkaattheedge`.
If you change it, you should rebuild the container.

## Configuration

The main configuration is in the following files:
* [`hostapf.conf`](./hostapd.conf) configures the access point.
* [`wlan0-dnsmasq.conf`](./wlan0-dnsmasq.conf) configures the DHCP and DNS.
  The way it is configured here is to work with K3S Kubernetes cluster. You might need to change the IP address of the DNS server to match your environment.

# This is script that run when device boot up or wake from sleep.

import ntptime
from time import sleep
import network
import esp
import gc
from machine import Pin, I2C
import BME280

esp.osdebug(None)
gc.collect()

# Configure pin assignment on ESP32
#i2c = I2C(scl=Pin(22), sda=Pin(19), freq=10000) => this works as well as an alternative PIN configuration on LOLIN32 Lite
i2c = I2C(scl=Pin(2), sda=Pin(15), freq=10000)

# Import Wifi credentials
import wificredentials

# Configure Wifi
wifi = network.WLAN(network.STA_IF)
wifi.active(True)
wifi.connect(wificredentials.SSID, wificredentials.PASSWORD)

while wifi.isconnected() == False:
  print("Wifi is not connected")
  sleep(1)

print('Wifi connection successful')
print(wifi.ifconfig())

print('Syncing time')
ntptime.settime()
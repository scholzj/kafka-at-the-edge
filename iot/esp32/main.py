import urequests
import utime

# BME sensor
bme = BME280.BME280(i2c=i2c)

# LED
led = Pin(22, Pin.OUT)
led.value(1) # LED off

LATITUDE = 50.0755 # Latitude of the sensor
LONGITUDE = 14.4378 # Logitude of the sensor
KEY = str(LATITUDE) + "x" + str(LONGITUDE)
TOPIC = "sensor-data"
BRIDGE_URL = "http://10.43.184.200:8080/topics/" + TOPIC

while True:
  try:
    temp = bme.temperature
    hum = bme.humidity
    pres = bme.pressure

    print('Received sensor data:')
    print('    Temperature: ', temp)
    print('    Humidity: ', hum)
    print('    Pressure: ', pres)
    print()
    
    currentTime = utime.localtime()
    produceRequest = {
      "records": [
        {
          "key": KEY,
          "value": {
            "latitude": LATITUDE,
            "longitude": LONGITUDE,
            "timestamp": "{}-{:0>2}-{:0>2} {:0>2}:{:0>2}:{:0>2}".format(currentTime[0], currentTime[1], currentTime[2], currentTime[3], currentTime[4], currentTime[5]),
            "temperature": float(temp.replace("C", "")),
            "humidity": float(hum.replace("%", "")),
            "pressure": float(pres.replace("hPa", ""))
            }
          }
        ]
      }
    
    response = urequests.post(BRIDGE_URL, json=produceRequest, headers={"Content-Type": "application/vnd.kafka.json.v2+json"})
    
    if response.status_code != 200:
      print('Publishing data sensor data failed!')
      print('Status code: ' + str(response.status_code))
      print('Response: ' + response.text)
      led.value(0) # LED on
    else:
      led.value(1) # LED off
    # ...
    response.close()
  except OSError as e:
    print("Oops! Something went wrong: ", e)
    led.setValue(0) # LED on
  #...
  sleep(1)

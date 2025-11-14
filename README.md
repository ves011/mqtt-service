# mqtt-service
Android MQTT service

# Overview
This Android application implements a foreground service which communicates with my MQTT broker, for remote monitoring and control of the other devices connected to broker.
The service has 2 notifications channels: the first one is for the connection state and the second is for incoming messages.
The MQTT broker is a mosquitto broker (https://mosquitto.org/) accepting ssl connections with 2 way certificate based authentication. 
Android service is using Eclipse paho client (https://eclipse.dev/paho/clients/android/) and uses for authentication the certificates stored on the android device.
Certificate infrastructure is a self signed one with PKI and certificates generated using Easy-RSA (https://github.com/OpenVPN/easy-rsa)

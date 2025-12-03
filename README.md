# mqtt-service
Android MQTT service

# Overview
This Android application implements a foreground service which communicates with a MQTT broker, for remote monitoring and control of some other devices connected to broker.
The service has 2 notifications channels: the first one is for the connection state and the second is for incoming messages.
The MQTT broker is **mosquitto** broker (https://mosquitto.org/) accepting ssl connections with 2 way certificate based authentication. 
Android service is using Eclipse paho client (https://eclipse.dev/paho/clients/android/) and uses for authentication the certificates stored on the android device.
Certificate infrastructure is a self signed one with PKI and certificates generated using Easy-RSA (https://github.com/OpenVPN/easy-rsa)

&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;<img src="app-screen.jpg" alt="application screen ">

To connect and communicate with broker, the following parameters are required
- broker URL in the form **ssl://server-name:port**
- certificate alias, which is the alias you choose when installing the client certificate (note that CA certificate needs also to be installed to allow **KeyChain** to retrieve the full chain of certificates)
- MQTT topics - 1 to subscribe and 1 to publish

All these parameters are persistent in the app preferences

# SSL certificates
To implement 2-way authentication certificate based, it requires the client certificate and key and the root CA certificate.
All these have to be installed on your device in order to allow the service to connect to the broker.
As i said i used easyRSA to create my own PKI self-signed infrastructure and generate server certificate and key and client certificate and key.
My mosquitto broker is configured to accept TLS connection on port 1886 with the options below
> **require_certificate true<br>
> use_subject_as_username true**

On the phone the client certificate is installed using **Install from device storage --> VPN and app user certificate** option, while CA certificate is installed using **Install from device storage --> CA certificate**.
For the example in the code i used the client certificate with the alias **mqtt-cert**. 
Certificates retrieval is using **KeyChain** APIs
# Class structure
## MQTTService.java
Implements the service which is started in the foreground, connects to the broker, publish and receive messages using the 2 topics: **publishTopic** and **subscribeTopic**
The service is started by the main activity or by **MQTTReceiver** after phone reboot.
It uses 2 notification channels:
> **NOT_STATE_ID = 1** --> state notification channel.<br>
&emsp;<img src="mqtt-state.png" alt="state channel"> <br>
Display connection status: connected or disconnected to broker.

> **NOT_MSG_ID = 101** --> message received<br>
&emsp;<img src="mqtt-msg.png" alt="state channel"> <br>
when a message is received from broker on subscribed topic

NOT_STATE_ID channel supposed to be nondismissible, but unfortunately starting Android 14 this is no longer possible: 
[**"The behavior of FLAG_ONGOING_EVENT has changed to make such notifications actually dismissable by the user."**](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications)
So the user can dismiss it. However if the connection state change. the notification will be shown again. Still "Clear notifications" option does not remove it from the list. 

When the service is started, it try to connect to server by calling **connect2Server()**.
The certificates to be used for broker authentication are set by the **SSLSocketFactiory** in function **getSF()**.
The result of the connect attempt, or any change in connection state,  is notified to user using NOT_STATE_ID channel and to the app main screen via broadcastUpdate(). If connection is successful the service subscribes to **subscribeTopic**.

When a message is received via subscribed topic it is notified using NOT_MSG_ID channel and broadcasted to mainActivity via broadcastUpdate() function.

When the service is started, if the server URL and certificate alias can be retrieved from app preferences, it will try to connect to broker. If subscribe topic can be retrieved it will subscribe to it.

## MainActivity.java
<br>
Is the application launcher and implements user interface.
It includes 2 inner classes:

> &emsp; BroadcastReceiver **receiver**
It receives broadcastUpdates form MQTTService

> &emsp;  ServiceConnection **mConnection**
It handles service connection / disconnection events

The UI lets you specify the connection parameters and to update them according to the needs.
It controls broker connection state and displays last message received on the subscribed topic. It also provides possibility to send a predefined message using publish topic.
parseMessage() function is left empty here, because it depends on the specific of the received message.

## MQTTReceiver.java

Its a small class implementing a BroadcastReceiver registered in app manifest, used to automatically start the service after phone boot.
The filter allow receiving BOOT_COMPLETED and LOCKED_BOOT_COMPLETED system messages. I saw some discussions that other messages might also be broadcasted by system after boot, but the only message I got is BOOT_COMPLETED. 

## getCertificate.java

This class is needed only because android forbids selecting the certificates in the main thread of app.
In MainActivity certificate selection is done via  **KeyChain.choosePrivateKeyAlias()** which creates its own thread, but in the case the service is started at boot, without UI, the service needs to retrieve the certificates using alias stored in app preferences. So a **getCertificate** class implements a thread for this purpose.

## HeartBeat.java
Short story of this class and why is needed.
While testing the app i noticed, when the phone screen is black and no charger connected, the broker disconnect the client app for exceeding timeout.
Digging further i saw in this case (phone screen black and no charger connected) no PINGREQ/PINGRESP packets are exchanged between the client and broker as per MQTT specifications (every keepalive interval).
If screen is on (no matter is locked or not) or charger connected PINGREQ/PINGRESP packets are exchanged as expected.
Digging even further i found a missing connect parameter, **automaticReconnect**, which default is **false**
After setting it to **true**, PINGREQ/PINGRESP packets started to flow and the broker no longer disconnects the app.
Thinking this is the culprit, i tested again with **automaticReconnect = false**, and obviously the issue could not be reproduces again.
This, for me its an inconsistent behavior and my guess is that it is related more with the power management in Android than paho MQTT client implementation.
Any case having a reliable connection to receive messages from broker, this is critical for me, and the role of this class is to ensure higher reliability, even it looks redundant (and it is in a normal situation).
Basically the class implements a thread which sends a dumb message to broker every keepalive interval.
So in case PINGREQ is missed for whatever reason the broker will still receive a message from this client and will no longer complain about timeout exceeded.
Worst case is that every keepalive interval The client will sent 2 messages instead of 1: PINGREQ and dumb message.




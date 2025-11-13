package ves011.mqtt_service;

import android.Manifest;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;

import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.example.mqtt_service.R;

import javax.net.ssl.SSLSocketFactory;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTService extends Service
    {
    private String TAG = "Service";
    private String CHANNEL_ID = "MQTTChannel";
    /*
    notifications id
        service state (1): connected / disconnected
        message received (101): message received on subscribed topic(s)
     */
    private final int NOT_STATE_ID = 1, NOT_MSG_ID = 101;
    public X509Certificate[] chain = null;
    public PrivateKey privateKey = null;
    public String serverUrl = null, aliasCert = null, subscribeTopic = null, publishTopic = null;
    //up to max 5 topics
    private String[] topics = new String[5];
    public MqttClient client;

    //private final IBinder mBinder = new LocalBinder();
    public boolean isRunning = false, isConnected = false;
    //private Binder binder = new Binder();
    MqttCallback MQTTcb = new MqttCallback()
        {
        @Override
        public void connectionLost(Throwable cause)
            {
            isConnected = false;
            stateNotification();
            Intent intent = new Intent();
            intent.setAction(getString(R.string.ACTION_STATE_CHANGE));
            intent.putExtra("URL", serverUrl);
            intent.putExtra("ERROR", "connection lost");
            sendBroadcast(intent);
            }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception
            {
            Intent intent = new Intent();
            intent.setAction(getString(R.string.ACTION_NEW_MESSAGE));
            intent.putExtra("TOPIC", topic);
            intent.putExtra("PAYLOAD", message.getPayload());
            sendBroadcast(intent);
            messageNotification(topic);
            }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token)
            {
    
            }
        };
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder
        {
        MQTTService getService()
            {
            return MQTTService.this;
            }
        }

    @Override
    public void onCreate()
        {
        super.onCreate();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        serverUrl = settings.getString(getString(R.string.PREF_URL), "");
        aliasCert = settings.getString(getString(R.string.PREF_CERT_ALIAS), "");
        subscribeTopic = settings.getString(getString(R.string.TOPIC_MONITOR), "");
        publishTopic = settings.getString(getString(R.string.TOPIC_CTRL), "");

        createNotChnn();
        isRunning = true;
        topics[0] = getString(R.string.gnetdev_response);
        topics[1] = getString(R.string.wmon_state);
        Log.d(TAG, "onCreate()");
        }

    @Override
    public IBinder onBind(Intent intent)
        {
        return binder;
        }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
        {
        Intent notIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification not = new NotificationCompat.Builder(this, CHANNEL_ID)
                                   .setContentTitle("MQTT listener")
                                   .setContentText("connecting to broker...")
                                   .setSmallIcon(R.drawable.ic_mqtt)
                                   .setContentIntent(pendingIntent)
                                   .setColor(getColor(R.color.white))
                                   .build();
        startForeground(NOT_STATE_ID, not);
        connect2Server();
        Log.d(TAG, "onStartCommand()");
        isRunning = true;
        return START_STICKY;
        }
    
    @Override
    public void onDestroy()
        {
        stopForeground(true);
        super.onDestroy();
        isRunning = false;
        Log.d(TAG, "onDestroy()");
        }

    @Override
    public void onTaskRemoved(Intent rootIntent)
        {
        Log.i(TAG, "onTaskRemoved()");
        }
    
    
    private void createNotChnn()
        {
        CharSequence appName = getString(R.string.app_name);
        NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, appName, NotificationManager.IMPORTANCE_DEFAULT);
        serviceChannel.setDescription("MQTT Messages");
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
        }

    public void messageNotification(String content)
        {
        Intent notIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification not = new NotificationCompat.Builder(this, CHANNEL_ID)
                                   .setContentTitle("new MQTT message")
                                   .setContentText(content)
                                   .setSmallIcon(R.drawable.ic_mqtt_msg)
                                   .setContentIntent(pendingIntent)
                                   .setColor(getColor(R.color.white))
                                   .build();
        
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(this).notify(NOT_MSG_ID, not);
        }
    public void stateNotification()
        {
        String str = isConnected ? "connected to broker" : "disconnected";
        int color;
        if(isConnected == true)
            {
            str = "connected to broker";
            color = R.color.white;
            }
        else
            {
            str = "disconnected";
            color = R.color.red;
            }
        Intent notIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification not = new NotificationCompat.Builder(this, CHANNEL_ID)
                                   .setContentTitle("MQTT listener")
                                   .setContentText(str)
                                   .setSmallIcon(R.drawable.ic_mqtt)
                                   .setContentIntent(pendingIntent)
                                   .setColor(getColor(color))
                                   .build();
    
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(this).notify(NOT_STATE_ID, not);
        }
    public void connect2Server()
        {
        Intent intent = new Intent();
        if(isConnected == true)
            return;
        intent.setAction(getString(R.string.ACTION_STATE_CHANGE));
        intent.putExtra("URL", serverUrl);
        intent.putExtra("SUBTOPIC", subscribeTopic);
        intent.putExtra("PUBTOPIC", publishTopic);
        if(serverUrl != null && serverUrl.length() > 8) //ssl:// + :xx
            {
            if(aliasCert != null)
                {
                if(chain == null || privateKey == null)
                    {
                    new getCertificate(this, aliasCert);
                    }
                else
                    {
                    try
                        {
                        isConnected = false;
                        // generate unique clientID
                        String clientID = "android_" + System.currentTimeMillis() / 1000L;
                        client = new MqttClient(serverUrl, clientID, null);
                        client.setCallback(MQTTcb);
                        MqttConnectOptions options = new MqttConnectOptions();
                        options.setConnectionTimeout(10);
                        options.setKeepAliveInterval(60);
                        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);

                        SSLSocketFactory socketFactory = getSF();
                        if(socketFactory != null)
                            {
                            options.setSocketFactory(socketFactory);
                            Log.d(TAG, "starting connect the server...");
                            client.connect(options);
                            Log.d(TAG, "connected!");
                            if(subscribeTopic != null)
                                client.subscribe(subscribeTopic);
                            isConnected = true;
                            intent.putExtra("STATE", isConnected);
                            intent.putExtra("ERROR", "");
                            }
                        else
                            {
                            intent.putExtra("STATE", isConnected);
                            intent.putExtra("ERROR", "SSL socket factory error");
                            }
                        stateNotification();
                        sendBroadcast(intent);
                        }
                    catch(MqttException e)
                        {
                        String s = String.valueOf(e.getCause());
                        Log.d(TAG, "MQTT connect exception: " + s);
                        intent.putExtra("STATE", isConnected);
                        intent.putExtra("ERROR", s);
                        stateNotification();
                        sendBroadcast(intent);

                        //e.printStackTrace();
                        }
                    catch(Exception e)
                        {
                        Log.d(TAG, e.getMessage());
                        e.printStackTrace();
                        }
                    }
                }
            else
                {
                String s = "no certificate";
                Log.d(TAG, s);
                intent.putExtra("STATE", isConnected);
                intent.putExtra("ERROR", s);
                stateNotification();
                sendBroadcast(intent);
                }
            }
        else
            {
            String s = "host not provided";
            Log.d(TAG, s);
            intent.putExtra("STATE", isConnected);
            intent.putExtra("ERROR", s);
            stateNotification();
            sendBroadcast(intent);
            }
        }
    public void disconnect()
        {
        Intent intent = new Intent();
        intent.setAction(getString(R.string.ACTION_STATE_CHANGE));
        
        try
            {
            client.disconnect();
            isConnected = false;
            intent.putExtra("STATE", isConnected);
            intent.putExtra("URL", "");
            stateNotification();
            sendBroadcast(intent);
            }
        catch(Exception e) { e.printStackTrace();}
        }
    public void setCerts(X509Certificate[] c,    PrivateKey pk, String alias)
        {
        //if(c != null)
            chain = c;
        //if(pk != null)
            privateKey = pk;
        //if(alias != null)
            aliasCert = alias;
        }
    public void gcComplete(boolean status)
        {
        if(status)
            {
            Log.i(TAG, "getCertificate() completed successfully");
            connect2Server();
            }
        else
            {
            final Intent intent = new Intent();
            intent.setAction(getString(R.string.ACTION_STATE_CHANGE));
            intent.putExtra("URL", serverUrl);
            String s = "no certificate";
            intent.putExtra("STATE", isConnected);
            intent.putExtra("ERROR", s);
            sendBroadcast(intent);
            Log.i(TAG, "getCertificate() error");
            }
        }
/*
this piece of code was tested on some Samsung Ax phones with android 12 and 14
and a Huawei pSmart Android 10
Could not make it run on Android studio emulator
*/
    private SSLSocketFactory getSF() throws Exception
        {
        SSLSocketFactory sslsf = null;
        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", chain[1]);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate us
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        SSLContext context = SSLContext.getInstance("TLSv1.3");
        try
            {
            ks.setCertificateEntry("certificate", chain[0]);
            ks.setKeyEntry("private-key", privateKey, null, new java.security.cert.Certificate[]{chain[0]});
            //ks.setKeyEntry("private-key", privateKey.getEncoded(), new java.security.cert.Certificate[]{chain[0]});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, null);
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            sslsf = context.getSocketFactory();
            }
        catch(Exception e)
            {
            e.printStackTrace();
            }
        return sslsf;
        }
    
    }
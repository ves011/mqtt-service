package ves011.mqtt_service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MQTTReceiver extends BroadcastReceiver
    {
    @Override
    public void onReceive(Context context, Intent intent)
        {
        String action = intent.getAction();
        Log.d("BCRECEIVER", action);
        Intent intentService = new Intent(context, MQTTService.class);
        context.startForegroundService(intentService);
        }
    }

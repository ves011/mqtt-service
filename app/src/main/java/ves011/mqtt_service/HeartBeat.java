package ves011.mqtt_service;

import android.icu.text.SimpleDateFormat;
import android.os.*;
import android.os.Looper;

import android.util.Log;


public class HeartBeat extends Thread
    {
    private boolean bloop;
    public Thread t_wait = null;
    private long sleepTime;
    private MQTTService mqtts;
    private final String TAG = "HB";
    private final String topic_hb = "wmon01/hb";

    private long slt = 0, dt = 0;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public HeartBeat(MQTTService m, int st)
        {
        bloop = false;
        // it should send 5 messages between 2 pingreq
        sleepTime = st * 1000 / 5;
        //sleepTime = 10000;
        mqtts = m;
        slt = System.currentTimeMillis();
        Log.d(TAG, sdf.format(slt) + " / new HeartBeat");
        }
    public void startLoop()
        {
        if(bloop == false)
            {
            bloop = true;
            t_wait = new Thread((Runnable) this);
            t_wait.setPriority(Thread.MAX_PRIORITY);
            t_wait.start();
            slt = System.currentTimeMillis();
            mqtts.sendMessage(topic_hb, this.getId() + " / " + sdf.format(slt) + " t_wait.start()");
            Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " t_wait.start()");
            }
        }
    public void stopLoop()
        {
        bloop = false;
        }
    public void run()
        {
        int ret;
        while(bloop)
            {
            try
                {
                Thread.sleep(sleepTime);
                }
            catch(InterruptedException e)
                {
                e.printStackTrace();
                slt = System.currentTimeMillis();
                ret = mqtts.sendMessage(topic_hb, sdf.format(slt) + " / t_wait thread exception: " + e.getMessage());
                if(ret == 0)
                    Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / t_wait thread exception: " + e.getMessage());
                else if(ret == -1)
                    Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / t_wait thread exception / not connected to server");
                else if(ret == -2)
                    Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / t_wait thread exception / publish exception");
                }
            if(!bloop)
                break;
            slt = System.currentTimeMillis();
            dt = slt - dt;
            
            ret = mqtts.sendMessage(topic_hb, this.getId() + " / " + sdf.format(slt) + " / " + dt + " / HB message");
            if(ret == 0)
                Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / " + dt + " / HB message");
            else if (ret == -1)
                Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / " + dt + " / HB message / not connected to server");
            else if(ret == -2)
                Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / " + dt + " / HB message / publish exception");
            dt = slt;
            }
        slt = System.currentTimeMillis();
        ret = mqtts.sendMessage(topic_hb, sdf.format(slt) + " / " + dt + " / bloop = false");
        if(ret == 0)
            Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / " + dt + " / bloop = false");
        else if(ret == -1)
            Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / " + dt + " / bloop = false / not connected to server");
        else if(ret == -1)
            Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / " + dt + " / bloop = false / publish exception");
        }
    public void keepAlive()
        {
        slt = System.currentTimeMillis();
        dt = slt - dt;
      
        int ret = mqtts.sendMessage(topic_hb, this.getId() + " / " + sdf.format(slt) + " / " + dt + " / alarm message");
        if(ret == 0)
            Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / " + dt + " / alarm message");
        else if (ret == -1)
            Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / " + dt + " / alarm message / not connected to server");
        else if(ret == -2)
            Log.d(TAG, this.getId() + " / " + sdf.format(slt) + " / " + dt + " / alarm message / publish exception");
        dt = slt;
        }
    }

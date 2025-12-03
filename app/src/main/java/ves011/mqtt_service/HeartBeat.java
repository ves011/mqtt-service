package ves011.mqtt_service;

import android.util.Log;

public class HeartBeat extends Thread
    {
    private boolean bloop;
    private Thread t_wait;
    private int sleepTime;
    private MQTTService mqtts;
    public HeartBeat(MQTTService m, int st)
        {
        bloop = false;
        sleepTime = st * 1000;
        mqtts = m;
        }
    public void startLoop()
        {
        if(bloop == false)
            {
            bloop = true;
            t_wait = new Thread((Runnable) this);
            t_wait.setPriority(Thread.NORM_PRIORITY);
            bloop = true;
            t_wait.start();
            }
        }
    public void stopLoop()
        {
        bloop = false;
        }
    public void run()
        {
        while(bloop)
            {
            try
                {
                Thread.sleep(sleepTime);
                }
            catch(InterruptedException e)
                {
                e.printStackTrace();
                }
            if(!bloop)
                break;
            mqtts.sendMessage("wmon01/a", "dumb message");
            Log.d("HB", "dumb message");
            }
        }
    }

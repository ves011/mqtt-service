package ves011.mqtt_service;

import android.content.Context;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.util.Log;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Enumeration;

public class getCertificate extends Thread
    {
    private Thread th;
    private String certAlias;
    private Context context;
    private String TAG = "getCert";
    private MQTTService mqtts;
    public getCertificate(Context ctx, String alias)
        {
        certAlias = alias;
        context = ctx;
        mqtts = (MQTTService) ctx;
        if(certAlias != null)
            {
            th = new Thread(this);
            th.start();
            }
        
        }
    public void run()
        {
        String str0 = null;
        String str1 = null;
        try
            {
            mqtts.chain = KeyChain.getCertificateChain(context, certAlias);
            mqtts.privateKey = KeyChain.getPrivateKey(context, certAlias);
            if(mqtts.chain != null && mqtts.privateKey != null && mqtts.chain.length == 1)
                {
                KeyStore ks1 = KeyStore.getInstance("AndroidCAStore");
                ks1.load(null);
                Enumeration<String> aliases = ks1.aliases();
                String strc = null;
                while(aliases.hasMoreElements())
                    {
                    String alias = aliases.nextElement();
                    Log.i("cert", "alias name: " + alias);
                    Certificate certificate = ks1.getCertificate(alias);
                    strc = certificate.toString();
                    //Log.i("cert", strc);
                    if(strc.contains("GNet"))
                        Log.i("cert", "---- my cert ----");
                    }
                }
            }
        catch (KeyChainException e)
            {
            e.printStackTrace();
            }
        catch (InterruptedException e)
            {
            e.printStackTrace();
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }

        if (mqtts.chain != null && mqtts.privateKey != null)
            {
            Log.d(TAG, "Successfully retrieved certificate chain of length: " + mqtts.chain.length);
            Log.d(TAG, "Private Key algorithm: " + mqtts.privateKey.getAlgorithm());
            mqtts.gcComplete(true);
            }
        else
            mqtts.gcComplete(false);
        }
    }

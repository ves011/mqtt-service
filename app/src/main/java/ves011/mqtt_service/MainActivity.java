package ves011.mqtt_service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;

import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.mqtt_service.R;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements KeyChainAliasCallback
    {
    // declaring objects of Button class
    private Button ssButton;
    private TextView tvCert, tvServer, tvState, tvTopics, tvConError, tvTimeStamp, tvMsgContent;
    private final String TAG = "MA";
    AlertDialog dialog;
    MQTTService mqttservice = null;
    private String serverURL, certAlias, subscribeTopic = null, publishTopic = null;

    private final BroadcastReceiver receiver = new BroadcastReceiver()
        {
        @Override
        public void onReceive(Context context, Intent intent)
            {
            String action = intent.getAction();
            String s;
            if(action.equals(getString(R.string.ACTION_STATE_CHANGE)))
                {
                boolean sc = intent.getBooleanExtra("STATE", false);
                publishTopic = intent.getStringExtra("PUBTOPIC");
                subscribeTopic = intent.getStringExtra("SUBTOPIC");
                s = "subscribed: " + subscribeTopic + "\n" +
                                     "publish: " + publishTopic;
                tvTopics.setText(s);
                if(sc == true)
                    {
                    s = "connected: " + intent.getStringExtra("URL");
                    tvState.setText(s);
                    tvConError.setText("");
                    ssButton.setText("disconnect");
                    }
                else
                    {
                    String url = intent.getStringExtra("URL");
                    if(url != null && url.length() > 8)
                        {
                        s = "not connected: " + intent.getStringExtra("URL");
                        tvState.setText(s);
                        tvConError.setText(intent.getStringExtra("ERROR"));
                        Log.d(TAG, s);
                        }
                    else
                        {
                        tvState.setText("disconnected");
                        tvConError.setText("");
                        }
                    ssButton.setText("connect");
                    }
                }
            else if(action.equals(getString(R.string.ACTION_NEW_MESSAGE)))
                {
                Date cd = Calendar.getInstance().getTime();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String strcd = simpleDateFormat.format(cd);
                tvTimeStamp.setText(strcd);
                parseMessage(intent.getStringExtra("TOPIC"), intent.getByteArrayExtra("PAYLOAD"));
                }
            else if(action.equals(getString(R.string.ACTION_UPDATE_CERT)))
                {
                tvCert.setText(mqttservice.aliasCert);
                }
            }
        };

    private ServiceConnection mConnection = new ServiceConnection()
        {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
            {
            MQTTService.LocalBinder binder = (MQTTService.LocalBinder) iBinder;
            mqttservice = binder.getService();
            serverURL = mqttservice.serverUrl;
            certAlias = mqttservice.aliasCert;
            subscribeTopic = mqttservice.subscribeTopic;
            publishTopic = mqttservice.publishTopic;
            tvCert.setText(certAlias);
            tvServer.setText(serverURL);
            Intent intent = new Intent();
            intent.setAction(getString(R.string.ACTION_STATE_CHANGE));
            intent.putExtra("STATE", mqttservice.isConnected);
            intent.putExtra("SUBTOPIC", subscribeTopic);
            intent.putExtra("PUBTOPIC", publishTopic);
            if(mqttservice.isConnected == true)
                intent.putExtra("URL", mqttservice.serverUrl);
            else
                intent.putExtra("URL", "");

            sendBroadcast(intent);
            }
        @Override
        public void onServiceDisconnected(ComponentName componentName)
            {
            unregisterReceiver(receiver);
            }
        };
    @Override
    protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serverURL = null;
        certAlias = null;
        tvCert = findViewById(R.id.certAlias);
        tvServer = findViewById(R.id.serverURL);
        tvState = findViewById(R.id.serviceStatus);
        tvConError = findViewById(R.id.conError);
        tvTopics = findViewById(R.id.topics);
        tvTimeStamp = findViewById(R.id.timeStamp);
        tvMsgContent = findViewById(R.id.msgContent);
        tvTimeStamp.setText("");
        tvState.setText("N/A");
        tvConError.setText("");
        String str = "subscribed: " + subscribeTopic + "\n" +
                             "publish: " + publishTopic;
        tvTopics.setText(str);
        ssButton = findViewById(R.id.ssButton);
        ssButton.setText("N/A");

        
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.ACTION_NEW_MESSAGE));
        intentFilter.addAction(getString(R.string.ACTION_STATE_CHANGE));
        intentFilter.addAction(getString(R.string.ACTION_UPDATE_CERT));
        registerReceiver(receiver, intentFilter, RECEIVER_EXPORTED);
        startService(new Intent(this, MQTTService.class));
        }
    @Override
    protected void onStart()
        {
        super.onStart();
        Intent intent = new Intent(this, MQTTService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    @Override
    protected void onStop()
        {
        super.onStop();
        }
    @Override
    protected void onDestroy()
        {
        super.onDestroy();
        }
    public void editUrl(View v)
        {
        TextView tv;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.edit_url, null));
        dialog = builder.create();

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
            @Override
            public void onCancel(DialogInterface dialog)
                {
                dialog = null;
                }
            });
        dialog.show();
        tv = dialog.findViewById(R.id.subtopic);
        tv.setText(serverURL);
        }
    public void saveURL(View v)
        {
        String str = ((TextView) dialog.findViewById(R.id.subtopic)).getText().toString();
        tvServer.setText(str);
        serverURL = str;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor ed = settings.edit();
        ed.putString(getString(R.string.SERVERURL), str);
        ed.apply();
        if(mqttservice != null)
            mqttservice.serverUrl = str;//.setServerURL(str);
        dialog.cancel();
        }
    public void editTopics(View v)
        {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.edit_topics, null));
        dialog = builder.create();

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
            @Override
            public void onCancel(DialogInterface dialog)
                {
                dialog = null;
                }
            });
        dialog.show();
        TextView tv = dialog.findViewById(R.id.subtopic);
        tv.setText(subscribeTopic);
        tv = dialog.findViewById(R.id.pubtopic);
        tv.setText(publishTopic);
        }
    public void saveTopics(View v)
        {
        String st, pt;
        st = ((TextView) dialog.findViewById(R.id.subtopic)).getText().toString();
        pt = ((TextView) dialog.findViewById(R.id.pubtopic)).getText().toString();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor ed = settings.edit();
        ed.putString(getString(R.string.TOPIC_MONITOR), st);
        ed.putString(getString(R.string.TOPIC_CTRL), pt);
        ed.apply();
        if(mqttservice != null)
            {
            mqttservice.publishTopic = pt;
            mqttservice.subscribeTopic = st;
            }
        String str = "subscribed: " + st + "\n" +
                             "publish: " + pt;
        tvTopics.setText(str);
        dialog.cancel();
        }
    public void getState(View v)
        {
        Toast ts = null;
        if(publishTopic != null && publishTopic.length() > 5)
            {
            int ret = mqttservice.sendMessage(publishTopic, getString(R.string.GETSTATE_MSG));
            if(ret == -1) // not connected to server
                ts = Toast.makeText(this, "not connected to server", Toast.LENGTH_LONG);
            else if(ret == -2) // error publising message
                ts = Toast.makeText(this, "publish message error", Toast.LENGTH_LONG);
            Log.d(TAG, "publish msg - ret = " + ret);
            }
        else
            ts = Toast.makeText(this, "publish topic not provided or invalid", Toast.LENGTH_LONG);
        if(ts != null)
            ts.show();
        }
    public void selCert(View v)
        {
        KeyChain.choosePrivateKeyAlias(this,this, null,null,null,-1,certAlias);
        }
    @Override
    public void alias(final String alias)
        {
        X509Certificate[] chain = null;
        PrivateKey privateKey = null;
        if (alias != null)
            {
            Log.d(TAG, "User selected alias: " + alias);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor ed = settings.edit();
            ed.putString("certAlias", alias);
            ed.apply();
            try
                {
                chain = KeyChain.getCertificateChain(this, alias);
                privateKey = KeyChain.getPrivateKey(this, alias);
                }
            catch(KeyChainException e){e.printStackTrace();}
            catch(Exception e){e.printStackTrace();}

            if(chain != null && privateKey != null)
                {
                Log.d(TAG, "Successfully retrieved certificate chain of length: " + chain.length);
                Log.d(TAG, "Private Key algorithm: " + privateKey.getAlgorithm());
                mqttservice.setCerts(chain, privateKey, alias);
                Intent intent = new Intent();
                intent.setAction(getString(R.string.ACTION_UPDATE_CERT));
                sendBroadcast(intent);
                mqttservice.connect2Server();
                }
            }
        else
            {
            mqttservice.setCerts(null, null, null);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor ed = settings.edit();
            ed.putString("certAlias", "");
            ed.apply();
            Intent intent = new Intent();
            intent.setAction(getString(R.string.ACTION_STATE_CHANGE));
            intent.putExtra("URL", mqttservice.serverUrl);
            String s = "no certificate";
            Log.d(TAG, s);
            intent.putExtra("STATE", mqttservice.isConnected);
            intent.putExtra("ERROR", s);
            sendBroadcast(intent);
            //intent = new Intent(); // with the old intent action is not updated
            intent.setAction(getString(R.string.ACTION_UPDATE_CERT));
            sendBroadcast(intent);
            Log.d(TAG, "User cancelled the certificate selection.");
            }
        }
    public void ssClick(View view)
        {
        if(ssButton.getText().equals("connect"))
            mqttservice.connect2Server();
        else if(ssButton.getText().equals("disconnect"))
            mqttservice.disconnect();
        }
    
/*
#define DVSTATE_ERR			0x01		// error state
#define DVSTATE_OPEN		0x02		// open
#define DVSTATE_CLOSE		0x04		// closed
#define MANUAL_BIT			0x100		// manual mode bit
#define DV_POWER_ERR_BIT	0x200		// no power to dv
#define MANUAL_BIT_FAILURE	0x400		// no readings for manual switch position

#define CHARGING_NA		1
#define CHARGING_ON		2
#define CHARGING_OFF	3
 */
    public void parseMessage(String topic, byte[] payload)
        {
        
        String state, tstr;
        int f_int;
        String str = new String(payload);
        String[] fields = str.split("\1");
        if(fields.length == 15)
            {
            state = "Sensor state: " + fields[6] + "\n";
            state += "DV state: ";
            f_int = Integer.parseInt(fields[9]);
            tstr = "N/A";
            if((f_int & 2) == 2)
                tstr = "OPEN";
            if((f_int & 4) == 4)
                tstr = "CLOSE";
            state += tstr;state += "\n";
            tstr = "OK";
            state += "DV mode: ";
            if((f_int & 0x100) == 0x100)
                tstr = "manual";
            if((f_int & 0x200) == 0x200)
                tstr = "power fail";
            if((f_int & 0x400) == 0x400)
                tstr = "hall sensor error";
            state += tstr; state += "\n";
            state += "Last DV check: " + fields[10] + "\n";
            state += "Battery: " + fields[8] + " (V)\n";
            state += "Charging state: ";
            f_int = Integer.parseInt(fields[3]);
            if(f_int == 1)
                tstr = "N/A";
            if(f_int == 2)
                tstr = "charging";
            if(f_int == 3)
                tstr = "idle";
            state += tstr; state += "\n";
            state += "Last charge start: " + fields[4] + "\n";
            state += "Last charge end: " + fields[5] + "\n";
            state += "Uptime: " + fields[11] +"d " + fields[12] +"h " + fields[13] +"m " + fields[14] +"s\n";
            tvMsgContent.setText(state);
            }
        
        
        /*
        parse received messages and display the content
        */
        }
    
    }

package it.challenge_engineering.www.broadcastlistener;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class MainActivity extends Activity
{

    // private UDPListenerService mUDPListenerService = null;
    private String appNameVersion = "UDB Broadcast Listener - v. 4.04.05 - 14/3/2020";
    private ServerConnection sConn = null;
    private TextView tdata = null;
    private Intent service = null;
    private String ipServer = "";
    private String ipLocal = "";
    final MainActivity activity = this;
    public Boolean alarmReceptionSuspended = false;

    public void acknowledgeAlarmReception()
    {
        alarmReceptionSuspended = true;
        if (sConn == null)
        {
            sConn = new ServerConnection(this /*, tdata */);
        }
        if (!ipServer.equals(""))
        {
            sConn.SendCommandToURL("http://" + ipServer + ":8080/?op=ALARM_RECEIVED&client=APP",true);
        }
        else
        {
            sConn.SendCommandToURL("http://192.168.1.232:8080/?op=ALARM_RECEIVED&client=APP",true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onStart()
    {
        super.onStart();
        if (service == null)
        {
            service = new Intent(this,UDPListenerService.class);
            //Log.i("[MC2410]-MAIN", "executing onStart()");
            startService(service);
        }
        if (sConn == null)
        {
            sConn = new ServerConnection(this);
        }
    }

    @Override
    protected void onDestroy()
    {
        //Log.e("[CE-CDG-01]-MN", "Application onDestroy()");
        stopPlayingAlarm();
        unregisterReceiver(receiverFromNetworkUDPListener); //unregister my receiver...
        stopService(service);
        super.onDestroy();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        // final TextView tdata = (TextView) findViewById(R.id.textdata);
        tdata.setText("onStop()");
//        if (mUDPListenerService != null)
//        {
//            mUDPListenerService.stopPlayingAlarm();
//        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        // final TextView tdata = (TextView) findViewById(R.id.textdata);
        tdata.setText("onPause()");
    }

    public void DisableAlarmButton()
    {
        ImageButton bal = (ImageButton) findViewById(R.id.balarm);
        bal.setEnabled(false);
        bal.setImageResource(R.mipmap.alarm_off_disabled);
    }

    public void EnableAlarmButton() {
        ImageButton bal = (ImageButton) findViewById(R.id.balarm);
        bal.setEnabled(true);
        bal.setImageResource(R.mipmap.alarm_off);
    }

    public void ShowAlarm(String name, String alarm)
    {
        TextView tv = (TextView) findViewById(R.id.nomeAllarme);
        tv.setText(name);
        tv = (TextView) findViewById(R.id.messaggioAllarme);
        tv.setText(alarm);
        View p1 = (View) findViewById(R.id.page1);
        p1.setVisibility(View.INVISIBLE);
        p1 = (View) findViewById(R.id.page2);
        p1.setVisibility(View.INVISIBLE);
        p1 = (View) findViewById(R.id.page4);
        p1.setVisibility(View.INVISIBLE);
        p1 = (View) findViewById(R.id.page3);
        p1.setVisibility(View.VISIBLE);
    }

    public int selectedOspite = -1;
    public int selectedOspiteGridIOndex = 0;


    private void ReceivingDataON()
    {
        ImageButton wflBuutt = (ImageButton) findViewById(R.id.LedReceiving1);
        wflBuutt.setImageResource(R.mipmap.ic_led_green);
        wflBuutt = (ImageButton) findViewById(R.id.LedReceiving2);
        wflBuutt.setImageResource(R.mipmap.ic_led_green);
    }

    private void ReceivingDataOFF()
    {
        ImageButton wflBuutt = (ImageButton) findViewById(R.id.LedReceiving1);
        wflBuutt.setImageResource(R.mipmap.ic_led_red);
        wflBuutt = (ImageButton) findViewById(R.id.LedReceiving2);
        wflBuutt.setImageResource(R.mipmap.ic_led_red);
    }

    private void WIFION()
    {
        ImageButton wflBuutt = (ImageButton) findViewById(R.id.LedWIFI1);
        wflBuutt.setImageResource(R.mipmap.ic_led_green);
        wflBuutt = (ImageButton) findViewById(R.id.LedWIFI2);
        wflBuutt.setImageResource(R.mipmap.ic_led_green);
    }

    private void WIFIOFF()
    {
        ImageButton wflBuutt = (ImageButton) findViewById(R.id.LedWIFI1);
        wflBuutt.setImageResource(R.mipmap.ic_led_red);
        wflBuutt = (ImageButton) findViewById(R.id.LedWIFI2);
        wflBuutt.setImageResource(R.mipmap.ic_led_red);
    }

    protected void drawDetailPage(TextView ospite)
    {
        TextView oname = (TextView) findViewById(R.id.nomeospitep4);
        oname.setText((ospite.getText()));
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }


    protected void ActivateLogging()
    {

        int dbg = 3;
        /*
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            // Permission is not granted
            dbg = 34;
        }
        */
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, dbg);

        try
        {
            if (isExternalStorageWritable())
            {

                // File appDirectory = new File(Environment.getExternalStorageDirectory() + "/MyPersonalAppFolder");
                File logDirectory00 = new File(Environment.getExternalStorageDirectory(),"log");
                File logDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"log");

                Date date = Calendar.getInstance().getTime();
                DateFormat formatter = new SimpleDateFormat("yyyy_MM_dd");
                String today = formatter.format(date);
                File logFile = new File(logDirectory, today + "_logcat.txt");

                boolean ris = false;
                try {
                    if (!logDirectory.exists()) {
                        ris = logDirectory.mkdir();
                    }
                }
                catch(SecurityException sec)
                {
                    sec.printStackTrace();
                }

                try {
                    if (!logFile.exists()) {
                        ris = logFile.createNewFile();
                        FileOutputStream stream = new FileOutputStream(logFile);
                        try {
                            GregorianCalendar dt = new GregorianCalendar();
                            stream.write(dt.toString().getBytes());
                        } finally {
                            stream.close();
                        }
                    }
                }
                catch(SecurityException sec)
                {
                    sec.printStackTrace();
                }

                if (ris)
                {
                    // clear the previous logcat and then write the new one to the file
                    try {
                        Process process = Runtime.getRuntime().exec("logcat -c");
                        process = Runtime.getRuntime().exec("logcat -f " + logFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        tdata = (TextView) findViewById(R.id.textdata);

        final TextView tv = (TextView) findViewById(R.id.mainmessage);
        tv.setText(appNameVersion);

        ImageButton bp1 = (ImageButton) findViewById(R.id.bpage1);
        ImageButton bp2 = (ImageButton) findViewById(R.id.bpage2);
        ImageButton bp3 = (ImageButton) findViewById(R.id.balarm);
        ImageButton bp4 = (ImageButton) findViewById(R.id.id_minimize);
        ImageButton bp5 = (ImageButton) findViewById(R.id.info);
        Button b1 = (Button) findViewById(R.id.startLoggingButton);
        bp1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.VISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page5);
                p1.setVisibility(View.INVISIBLE);
            }
        });
        bp2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.VISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page5);
                p1.setVisibility(View.INVISIBLE);
            }
        });
        bp3.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // DisableAlarmButton();
                stopPlayingAlarm();
                if (sConn == null) {
                    sConn = new ServerConnection(activity /*, tdata */);
                }
                if (!ipServer.equals(""))
                {
                    sConn.SendCommandToURL("http://" + ipServer + ":8080/?op=SUSPEND_ALARM&client=APP", true);
                }
                else
                {
                    sConn.SendCommandToURL("http://192.168.1.232:8080/?op=SUSPEND_ALARM&client=APP", true);
                }
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.VISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page5);
                p1.setVisibility(View.INVISIBLE);

            }
        });

        bp4.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                try
                {
                    // Log.i("[CE-CDG-01]-MN", "activity.moveTaskToBack()");

                    View p1 = (View) findViewById(R.id.page1);
                    p1.setVisibility(View.INVISIBLE);
                    p1 = (View) findViewById(R.id.page2);
                    p1.setVisibility(View.VISIBLE);
                    p1 = (View) findViewById(R.id.page3);
                    p1.setVisibility(View.INVISIBLE);
                    p1 = (View) findViewById(R.id.page4);
                    p1.setVisibility(View.INVISIBLE);
                    p1 = (View) findViewById(R.id.page5);
                    p1.setVisibility(View.INVISIBLE);

                    activity.moveTaskToBack(true);

                    // ActivateLogging();

                    // ReceivingDataOFF();
                    // WIFIOFF();
                    // startPlayingAlarm();
                }
                catch (Exception e)
                {
                    // Log.e("[CE-CDG-01]-MN", "EXCEPTION on activity.moveTaskToBack(): " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        bp5.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                try
                {
                    // Log.i("[CE-CDG-01]-MN", "showSMInfo()");

                    View p1 = (View) findViewById(R.id.page1);
                    p1.setVisibility(View.INVISIBLE);
                    p1 = (View) findViewById(R.id.page2);
                    p1.setVisibility(View.INVISIBLE);
                    p1 = (View) findViewById(R.id.page3);
                    p1.setVisibility(View.INVISIBLE);
                    p1 = (View) findViewById(R.id.page4);
                    p1.setVisibility(View.INVISIBLE);
                    p1 = (View) findViewById(R.id.page5);
                    p1.setVisibility(View.VISIBLE);

                    final TextView tv = (TextView) findViewById(R.id.infoact);
                    tv.setText(getActiveApps(activity));

                }
                catch (Exception e)
                {
                    // Log.e("[CE-CDG-01]-MN", "EXCEPTION on activity.showSMInfo(): " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        Button btosp = (Button) findViewById(R.id.ospitibutton1);
        btosp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectedOspite = 0;
                // DisableAlarmButton();
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.VISIBLE);
                TextView tv1 = (TextView)findViewById(R.id.ospitinome1);
                drawDetailPage(tv1);
            }
        });

        b1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                FragmentActivity fa = new FragmentActivity();
                PasswordDialog pd = new PasswordDialog();
                pd.show(getFragmentManager(),"Start Logging");
            }
        });

        btosp = (Button) findViewById(R.id.ospitibutton2);
        btosp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectedOspite = 1;
                // DisableAlarmButton();
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.VISIBLE);
                TextView tv1 = (TextView)findViewById(R.id.ospitinome2);
                drawDetailPage(tv1);

            }
        });

        btosp = (Button) findViewById(R.id.ospitibutton3);
        btosp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectedOspite = 2;
                // DisableAlarmButton();
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.VISIBLE);
                TextView tv1 = (TextView)findViewById(R.id.ospitinome3);
                drawDetailPage(tv1);

            }
        });

        btosp = (Button) findViewById(R.id.ospitibutton4);
        btosp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectedOspite = 3;
                // DisableAlarmButton();
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.VISIBLE);
                TextView tv1 = (TextView)findViewById(R.id.ospitinome4);
                drawDetailPage(tv1);

            }
        });

        btosp = (Button) findViewById(R.id.ospitibutton5);
        btosp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectedOspite = 4;
                // DisableAlarmButton();
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.VISIBLE);
                TextView tv1 = (TextView)findViewById(R.id.ospitinome5);
                drawDetailPage(tv1);

            }
        });

        btosp = (Button) findViewById(R.id.ospitibutton6);
        btosp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectedOspite = 5;
                // DisableAlarmButton();
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.VISIBLE);
                TextView tv1 = (TextView)findViewById(R.id.ospitinome6);
                drawDetailPage(tv1);

            }
        });

        btosp = (Button) findViewById(R.id.ospitibutton7);
        btosp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectedOspite = 6;
                // DisableAlarmButton();
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.VISIBLE);
                TextView tv1 = (TextView)findViewById(R.id.ospitinome7);
                drawDetailPage(tv1);

            }
        });

        btosp = (Button) findViewById(R.id.ospitibutton8);
        btosp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectedOspite = 7;
                // DisableAlarmButton();
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.VISIBLE);
                TextView tv1 = (TextView)findViewById(R.id.ospitinome8);
                drawDetailPage(tv1);

            }
        });

        btosp = (Button) findViewById(R.id.ospitibutton9);
        btosp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectedOspite = 8;
                // DisableAlarmButton();
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.VISIBLE);
                TextView tv1 = (TextView)findViewById(R.id.ospitinome9);
                drawDetailPage(tv1);

            }
        });

        btosp = (Button) findViewById(R.id.ospitibutton10);
        btosp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectedOspite = 9;
                // DisableAlarmButton();
                View p1 = (View) findViewById(R.id.page1);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page2);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page3);
                p1.setVisibility(View.INVISIBLE);
                p1 = (View) findViewById(R.id.page4);
                p1.setVisibility(View.VISIBLE);
                TextView tv1 = (TextView)findViewById(R.id.ospitinome10);
                drawDetailPage(tv1);

            }
        });

        // DisableAlarmButton();
        View p1 = (View) findViewById(R.id.page1);
        p1.setVisibility(View.INVISIBLE);
        p1 = (View) findViewById(R.id.page3);
        p1.setVisibility(View.INVISIBLE);
        p1 = (View) findViewById(R.id.page2);
        p1.setVisibility(View.VISIBLE);
        p1 = (View) findViewById(R.id.page4);
        p1.setVisibility(View.INVISIBLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // -- registra il receiver per i dati ricevuti dall'UDPListener
        registerReceiver();
    }

    private void DisplayStatus(JSONArray elencoOspiti) throws JSONException 
    {
            TextView t1 = (TextView) findViewById(R.id.totOspitiGestiti);
            TextView t2 = (TextView) findViewById(R.id.totOspitiNonCollegati);
            TextView t3 = (TextView) findViewById(R.id.totOspitiInAllarme);

            int numOsp = 0;
            if (t2 != null)
            {
                int totNC = 0;
                int totERR = 0;
                for (int j=0;j < elencoOspiti.length();j++)
                {
                    JSONObject ospite = elencoOspiti.getJSONObject(j);
                    String nomeospite = ospite.getString("ospite_nome");
                    if (!nomeospite.equals(""))
                    {
                        numOsp++;
                    }
                    String statoOspite = ospite.getString("stato");
                    if (statoOspite.equals("0"))
                    {
                        totNC++;
                    }
                    else if (statoOspite.equals("2"))
                    {
                        totERR++;
                    }
                }
                if (t1 != null)
                {
                    t1.setText(Integer.toString(numOsp));
                }
                t2.setText(Integer.toString(totNC));
                t3.setText(Integer.toString(totERR));
            }
        }

    private void DisplayDetailsOspiti(JSONArray elencoOspiti) throws JSONException
    {
            for (int j=0;j<elencoOspiti.length();j++)
            {
                JSONObject ospite = elencoOspiti.getJSONObject(j);
                TextView tso2 = null;
                TextView tnome = null;
                TextView tfc = null;
                LinearLayout ll = null;
                Button ospitiButt = null;
                String stato = null;

                stato = ospite.getString("stato");

                switch(j)
                {
                    case 0:
                        tnome = (TextView) findViewById(R.id.ospitinome1);
                        tnome.setText(ospite.getString("ospite_nome") + " " + ospite.getString("ospite_cognome"));
                        tso2 = (TextView) findViewById(R.id.ospitiso21);
                        tso2.setText(ospite.getString("so2"));
                        tfc = (TextView) findViewById(R.id.ospitifc1);
                        tfc.setText(ospite.getString("pulse"));
                        ll = (LinearLayout) findViewById(R.id.ospitirow1);
                        ospitiButt = (Button) findViewById(R.id.ospitibutton1);
                        break;
                    case 1:
                        tnome = (TextView) findViewById(R.id.ospitinome2);
                        tnome.setText(ospite.getString("ospite_nome") + " " + ospite.getString("ospite_cognome"));
                        tso2 = (TextView) findViewById(R.id.ospitiso22);
                        tso2.setText(ospite.getString("so2"));
                        tfc = (TextView) findViewById(R.id.ospitifc2);
                        tfc.setText(ospite.getString("pulse"));
                        ll = (LinearLayout) findViewById(R.id.ospitirow2);
                        ospitiButt = (Button) findViewById(R.id.ospitibutton2);
                        break;
                    case 2:
                        tnome = (TextView) findViewById(R.id.ospitinome3);
                        tnome.setText(ospite.getString("ospite_nome") + " " + ospite.getString("ospite_cognome"));
                        tso2 = (TextView) findViewById(R.id.ospitiso23);
                        tso2.setText(ospite.getString("so2"));
                        tfc = (TextView) findViewById(R.id.ospitifc3);
                        tfc.setText(ospite.getString("pulse"));
                        ll = (LinearLayout) findViewById(R.id.ospitirow3);
                        ospitiButt = (Button) findViewById(R.id.ospitibutton3);
                        break;
                    case 3:
                        tnome = (TextView) findViewById(R.id.ospitinome4);
                        tnome.setText(ospite.getString("ospite_nome") + " " + ospite.getString("ospite_cognome"));
                        tso2 = (TextView) findViewById(R.id.ospitiso24);
                        tso2.setText(ospite.getString("so2"));
                        tfc = (TextView) findViewById(R.id.ospitifc4);
                        tfc.setText(ospite.getString("pulse"));
                        ll = (LinearLayout) findViewById(R.id.ospitirow4);
                        ospitiButt = (Button) findViewById(R.id.ospitibutton4);
                        break;
                    case 4:
                        tnome = (TextView) findViewById(R.id.ospitinome5);
                        tnome.setText(ospite.getString("ospite_nome") + " " + ospite.getString("ospite_cognome"));
                        tso2 = (TextView) findViewById(R.id.ospitiso25);
                        tso2.setText(ospite.getString("so2"));
                        tfc = (TextView) findViewById(R.id.ospitifc5);
                        tfc.setText(ospite.getString("pulse"));
                        ll = (LinearLayout) findViewById(R.id.ospitirow5);
                        ospitiButt = (Button) findViewById(R.id.ospitibutton5);
                        break;
                    case 5:
                        tnome = (TextView) findViewById(R.id.ospitinome6);
                        tnome.setText(ospite.getString("ospite_nome") + " " + ospite.getString("ospite_cognome"));
                        tso2 = (TextView) findViewById(R.id.ospitiso26);
                        tso2.setText(ospite.getString("so2"));
                        tfc = (TextView) findViewById(R.id.ospitifc6);
                        tfc.setText(ospite.getString("pulse"));
                        ll = (LinearLayout) findViewById(R.id.ospitirow6);
                        ospitiButt = (Button) findViewById(R.id.ospitibutton6);
                        break;
                    case 6:
                        tnome = (TextView) findViewById(R.id.ospitinome7);
                        tnome.setText(ospite.getString("ospite_nome") + " " + ospite.getString("ospite_cognome"));
                        tso2 = (TextView) findViewById(R.id.ospitiso27);
                        tso2.setText(ospite.getString("so2"));
                        tfc = (TextView) findViewById(R.id.ospitifc7);
                        tfc.setText(ospite.getString("pulse"));
                        ll = (LinearLayout) findViewById(R.id.ospitirow7);
                        ospitiButt = (Button) findViewById(R.id.ospitibutton7);
                        break;
                    case 7:
                        tnome = (TextView) findViewById(R.id.ospitinome8);
                        tnome.setText(ospite.getString("ospite_nome") + " " + ospite.getString("ospite_cognome"));
                        tso2 = (TextView) findViewById(R.id.ospitiso28);
                        tso2.setText(ospite.getString("so2"));
                        tfc = (TextView) findViewById(R.id.ospitifc8);
                        tfc.setText(ospite.getString("pulse"));
                        ll = (LinearLayout) findViewById(R.id.ospitirow8);
                        ospitiButt = (Button) findViewById(R.id.ospitibutton8);
                        break;
                    case 8:
                        tnome = (TextView) findViewById(R.id.ospitinome9);
                        tnome.setText(ospite.getString("ospite_nome") + " " + ospite.getString("ospite_cognome"));
                        tso2 = (TextView) findViewById(R.id.ospitiso29);
                        tso2.setText(ospite.getString("so2"));
                        tfc = (TextView) findViewById(R.id.ospitifc9);
                        tfc.setText(ospite.getString("pulse"));
                        ll = (LinearLayout) findViewById(R.id.ospitirow9);
                        ospitiButt = (Button) findViewById(R.id.ospitibutton9);
                        break;
                    case 9:
                        tnome = (TextView) findViewById(R.id.ospitinome10);
                        tnome.setText(ospite.getString("ospite_nome") + " " + ospite.getString("ospite_cognome"));
                        tso2 = (TextView) findViewById(R.id.ospitiso210);
                        tso2.setText(ospite.getString("so2"));
                        tfc = (TextView) findViewById(R.id.ospitifc10);
                        tfc.setText(ospite.getString("pulse"));
                        ll = (LinearLayout) findViewById(R.id.ospitirow10);
                        ospitiButt = (Button) findViewById(R.id.ospitibutton10);
                        break;
                }
                if (stato.equals("1"))
                {
                    ll.setBackgroundColor(Color.rgb(0,255,0));
                    ospitiButt.setVisibility(View.VISIBLE);
                }
                else if (stato.equals("0"))
                {
                    ll.setBackgroundColor(Color.rgb(255,255,0));
                    ospitiButt.setVisibility(View.INVISIBLE);
                }
                else if (stato.equals("2"))
                {
                    ll.setBackgroundColor(Color.rgb(255,0,0));
                    ospitiButt.setVisibility(View.VISIBLE);
                }
                else if (stato.equals("4"))     // -- il raspberry non invia il segnale di watchdog
                {
                    ll.setBackgroundColor(Color.rgb(214, 66, 247));
                    ospitiButt.setVisibility(View.INVISIBLE);
                }
            }
    }

    public MediaPlayer mMediaPlayer = null;
    public void startPlayingAlarm() throws IllegalArgumentException,SecurityException,IllegalStateException,IOException
    {
        // Log.i("[CE-CDG-01]-MN", "startPlayingAlarm() called...");
        if (mMediaPlayer == null)
        {
            try
            {
                AssetManager assets = this.getAssets();
                AssetFileDescriptor fd;
                fd = assets.openFd("alarm3.wav");
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                mMediaPlayer.setVolume(1,1);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            } catch (IllegalArgumentException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (SecurityException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IllegalStateException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        else if (!mMediaPlayer.isPlaying())
        {
            try
            {
                AssetManager assets = this.getAssets();
                AssetFileDescriptor fd;
                fd = assets.openFd("alarm3.wav");
                mMediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                mMediaPlayer.setVolume(1,1);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            } catch (IllegalArgumentException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (SecurityException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IllegalStateException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            mMediaPlayer.prepare();
            mMediaPlayer.start();
        }
    }

    public Boolean stopPlayingAlarm()
    {
        // Log.i("[CE-CDG-01]-MN", "stopPlayingAlarm() called...");
        if (mMediaPlayer != null)
        {
            if (mMediaPlayer.isPlaying())
            {
                mMediaPlayer.stop();
                return (true);
            }
        }
        return (false);
    }

    ReceiverFromNetworkUDPListener receiverFromNetworkUDPListener;
    // ---------------------------------------------------------------------------------------------
    // Receiver per i messaggi ricevuti dal Service
    // ---------------------------------------------------------------------------------------------
    private class ReceiverFromNetworkUDPListener extends BroadcastReceiver
    {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
        @Override
        public void onReceive(Context arg0, Intent arg1)
        {
            if (arg1.hasExtra("message"))
            {
                // Log.i("[CE-CDG-01]-MN", "MESSAGE RECEIVED FROM SERVER: " + arg1.getStringExtra("message"));

                ReceivingDataON();
                WIFION();

                JSONObject obj = null;
                JSONArray listaOspiti = null;
                String messageType = "";
                try
                {
                    obj = new JSONObject(arg1.getStringExtra("message"));
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }

                try
                {
                    messageType = obj.getString("tipoMessaggio");
                    String dispMessage = messageType;
                    GregorianCalendar tt = new GregorianCalendar();
                    if (messageType.equals("STATUS"))
                    {
                        dispMessage = "DATA RECEIVED at sec. ";
                    }
                    tdata.setText("D02 - " + dispMessage + " - " + tt.get(Calendar.SECOND));
                    if (messageType.equals("ALARM"))
                    {
                        Log.i("[" + Thread.currentThread().toString() + "][CE-CDG-01]-MA", "Broadcast received: ALARM");
                        tdata.setText("D03 - " + dispMessage + " - " + tt.get(Calendar.SECOND));
                        ipServer = obj.getString("ipServer");
                        TextView tv = (TextView) findViewById(R.id.ipAddressLabelServer);
                        tv.setText(ipServer);

                        final String testoMessaggio = obj.getString("testoMessaggio");
                        final String valoreParametro = obj.getString("valoreParametro");
                        final String nomeOspite = obj.getString("nomeOspite");
                        if (!alarmReceptionSuspended)
                        {
                            tdata.setText("D04 - " + dispMessage + " - " + tt.get(Calendar.SECOND));
                            acknowledgeAlarmReception();
                            startPlayingAlarm();
                            EnableAlarmButton();
                            ShowAlarm(nomeOspite, testoMessaggio + ": " + valoreParametro);
                        }

                    }
                    else if (messageType.equals("STATUS"))
                    {
                        ipServer = obj.getString("ipServer");
                        TextView tv = (TextView) findViewById(R.id.ipAddressLabelServer);
                        tv.setText(ipServer);

                        listaOspiti = obj.getJSONArray("elencoOspiti");
                        View page4 = (View) findViewById(R.id.page4);
                        if (page4.getVisibility() == View.VISIBLE) {
                            if (selectedOspiteGridIOndex == 0) {
                                List<Entry> entries = new ArrayList<Entry>();
                                entries.add(new Entry(1, 0));
                                entries.add(new Entry(2, 0));
                                entries.add(new Entry(3, 0));
                                entries.add(new Entry(4, 0));
                                entries.add(new Entry(5, 0));
                                entries.add(new Entry(6, 0));
                                entries.add(new Entry(7, 0));
                                entries.add(new Entry(8, 0));
                                entries.add(new Entry(9, 0));
                                entries.add(new Entry(10, 0));

                                LineDataSet dataSet = new LineDataSet(entries, "Saturazione O2"); // add entries to dataset
                                // dataSet.setColor(...);
                                // dataSet.setValueTextColor(...); // styling, ...
                                LineData lineData = new LineData(dataSet);
                                LineChart chart1 = (LineChart) findViewById(R.id.chart1);
                                chart1.setData(lineData);
                                YAxis yAxis = chart1.getAxisLeft();
                                yAxis.setAxisMinimum(83f); // start at zero
                                yAxis.setAxisMaximum(102f); // the axis maximum is 100
                                YAxis yAxis2 = chart1.getAxisRight();
                                yAxis2.setAxisMinimum(83f); // start at zero
                                yAxis2.setAxisMaximum(102f); // the axis maximum is 100

                                // LimitLine ll = new LimitLine(102f, "Valore critico");
                                LimitLine ll = new LimitLine(100f);
                                ll.setLineColor(Color.RED);
                                ll.setLineWidth(4f);
                                ll.setTextColor(Color.BLACK);
                                ll.setTextSize(12f);
                                yAxis.addLimitLine(ll);

                                LimitLine ll2 = new LimitLine(85f);
                                ll2.setLineColor(Color.RED);
                                ll2.setLineWidth(4f);
                                ll2.setTextColor(Color.BLACK);
                                ll2.setTextSize(12f);
                                yAxis.addLimitLine(ll2);

                                chart1.invalidate(); // refresh

                                List<Entry> entries2 = new ArrayList<Entry>();
                                entries2.add(new Entry(1, 0));
                                entries2.add(new Entry(2, 0));
                                entries2.add(new Entry(3, 0));
                                entries2.add(new Entry(4, 0));
                                entries2.add(new Entry(5, 0));
                                entries2.add(new Entry(6, 0));
                                entries2.add(new Entry(7, 0));
                                entries2.add(new Entry(8, 0));
                                entries2.add(new Entry(9, 0));
                                entries2.add(new Entry(10, 0));

                                LineDataSet dataSet2 = new LineDataSet(entries2, "Freq. cardiaca"); // add entries to dataset
                                LineData lineData2 = new LineData(dataSet2);
                                LineChart chart2 = (LineChart) findViewById(R.id.chart2);
                                chart2.setData(lineData2);
                                YAxis yAxis3 = chart2.getAxisLeft();
                                yAxis3.setAxisMinimum(55f);
                                yAxis3.setAxisMaximum(155f);
                                YAxis yAxis4 = chart2.getAxisRight();
                                yAxis4.setAxisMinimum(55f);
                                yAxis4.setAxisMaximum(155f);

                                // LimitLine ll = new LimitLine(102f, "Valore critico");
                                LimitLine ll3 = new LimitLine(150f);
                                ll3.setLineColor(Color.RED);
                                ll3.setLineWidth(4f);
                                ll3.setTextColor(Color.BLACK);
                                ll3.setTextSize(12f);
                                yAxis3.addLimitLine(ll3);

                                LimitLine ll4 = new LimitLine(60f);
                                ll4.setLineColor(Color.RED);
                                ll4.setLineWidth(4f);
                                ll4.setTextColor(Color.BLACK);
                                ll4.setTextSize(12f);
                                yAxis3.addLimitLine(ll4);

                                chart2.invalidate(); // refresh

                                selectedOspiteGridIOndex = 10;
                            } else {
                                // int randomNum = ThreadLocalRandom.current().nextInt(30, 80 + 1);
                                JSONObject ospite = listaOspiti.getJSONObject(selectedOspite);
                                float dv = Float.parseFloat(ospite.getString("so2"));
                                Entry e1 = new Entry(selectedOspiteGridIOndex, dv);
                                LineChart chart1 = (LineChart) findViewById(R.id.chart1);
                                chart1.getLineData().getDataSetByIndex(0).removeFirst();
                                chart1.getLineData().getDataSetByIndex(0).addEntry(e1);
                                chart1.getData().notifyDataChanged();
                                chart1.notifyDataSetChanged();
                                chart1.invalidate(); // refresh

                                float fq = Float.parseFloat(ospite.getString("pulse"));
                                Entry e12 = new Entry(selectedOspiteGridIOndex, fq);
                                LineChart chart2 = (LineChart) findViewById(R.id.chart2);
                                chart2.getLineData().getDataSetByIndex(0).removeFirst();
                                chart2.getLineData().getDataSetByIndex(0).addEntry(e12);
                                chart2.getData().notifyDataChanged();
                                chart2.notifyDataSetChanged();
                                chart2.invalidate(); // refresh

                                selectedOspiteGridIOndex++;
                            }
                        } else {
                            DisplayStatus(listaOspiti);
                            DisplayDetailsOspiti(listaOspiti);
                        }
                        // mText.setText("[" + Integer.toString(counter) + "] - " + Integer.toString(listaOspiti.length()));
                    }
                    else if (messageType.equals("INFO"))
                    {
                        TextView tv = (TextView) findViewById(R.id.ipAddressLabel);
                        tv.setText(obj.getString("ipAddress"));
                        ipLocal = obj.getString("ipAddress");
                    }
                    else if (messageType.equals("WATCHDOG"))
                    {
                        if (!ipServer.equals(""))
                        {
                            GregorianCalendar tt2 = new GregorianCalendar();
                            tdata.setText("WATCHDOG[1] - " + ipServer + " - " + tt.get(Calendar.SECOND));
                            sConn.SendCommandToURL("http://" + ipServer + ":8080/?op=WATCHDOG_SM&ipaddress=" + ipLocal + "&stato=0&client=APP",false);
                        }
                        else
                        {
                            tdata.setText("WATCHDOG[2] - " + tt.get(Calendar.SECOND));
                            sConn.SendCommandToURL("http://192.168.1.232:8080/?op=WATCHDOG_SM&ipaddress=" + ipLocal + "&stato=0&client=APP",false);
                        }
                    }
                    else if (messageType.equals("TIMEOUT"))
                    {
                        // Log.e("[CE-CDG-01]-MN", "No data from server");
                        ReceivingDataOFF();
                        WIFIOFF();
                        if (!ipServer.equals(""))
                        {
                            tdata.setText("TIMEOUT[1] - " + tt.get(Calendar.HOUR) + ":" + tt.get(Calendar.MINUTE) + ":" + tt.get(Calendar.SECOND));
                            sConn.SendCommandToURL("http://" + ipServer + ":8080/?op=TIMEOUT&ipaddress=" + ipLocal + "&stato=0&client=APP",false);
                        }
                        else
                        {
                            tdata.setText("TIMEOUT[2] - " + tt.get(Calendar.HOUR) + ":" + tt.get(Calendar.MINUTE) + ":" + tt.get(Calendar.SECOND));
                            sConn.SendCommandToURL("http://192.168.1.232:8080/?op=TIMEOUT_SM&ipaddress=" + ipLocal + "&stato=0&client=APP",false);
                        }
                    }

                    } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void registerReceiver()
    {
        receiverFromNetworkUDPListener = new ReceiverFromNetworkUDPListener();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UDPListenerService.UDP_BROADCAST);
        registerReceiver(receiverFromNetworkUDPListener, intentFilter);
    }


    // ---------------------------------------------------------------------------------------------
    // Gestione delle applicazioni attive sullo Smartphone
    // ---------------------------------------------------------------------------------------------
    private static boolean isSTOPPED(ApplicationInfo pkgInfo)
    {
        return ((pkgInfo.flags & ApplicationInfo.FLAG_STOPPED) != 0);
    }

    private static boolean isSUSPENDED(ApplicationInfo pkgInfo)
    {
        return ((pkgInfo.flags & ApplicationInfo.FLAG_SUSPENDED) != 0);
    }

    private static boolean isPERSISTENT(ApplicationInfo pkgInfo)
    {
        return ((pkgInfo.flags & ApplicationInfo.FLAG_PERSISTENT) != 0);
    }

    private static boolean isINSTALLED(ApplicationInfo pkgInfo)
    {
        return ((pkgInfo.flags & ApplicationInfo.FLAG_INSTALLED) != 0);
    }

    private static boolean isSYSTEM(ApplicationInfo pkgInfo)
    {
        return ((pkgInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    public static String getApplicationLabel(Context context, String packageName)
    {

        PackageManager        packageManager = context.getPackageManager();
        List<ApplicationInfo> packages       = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        String                label          = null;

        for (int i = 0; i < packages.size(); i++) {

            ApplicationInfo temp = packages.get(i);

            if (temp.packageName.equals(packageName))
            {
                label = packageManager.getApplicationLabel(temp).toString();
            }
        }

        return label;
    }

    public static String getActiveApps(Context context)
    {

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        String value = "";
        value += "---------------------------------\n";
        value += "App status\n";
        value += "=================================\n";

        /* */
        for (ApplicationInfo packageInfo : packages) {

            //system apps! get out
            // if (!isSTOPPED(packageInfo) && !isSYSTEM(packageInfo))
            // {
                if (getApplicationLabel(context, packageInfo.packageName).equals("3CX"))
                {
                    value += "###" + getApplicationLabel(context, packageInfo.packageName) + "###" + "\n" + packageInfo.packageName + "\n";
                    if (isINSTALLED(packageInfo)) {
                        value += "- INSTALLED\n";
                    }
                    if (isSTOPPED(packageInfo)) {
                        value += "- STOPPED\n";
                    }
                    if (isSUSPENDED(packageInfo)) {
                        value += "- SUSPENDED\n";
                    }
                    if (isPERSISTENT(packageInfo)) {
                        value += "- PERSISTENT\n";
                    }
                    // value += "MIN SDK: " + new Integer(packageInfo.minSdkVersion).toString() + "\n";
                    value += "-----------------------\n";
                }
                else if ((getApplicationLabel(context, packageInfo.packageName).equals("Host")) &&
                            (packageInfo.packageName.equals("com.teamviewer.host.market")))
                {
                    value += "###" + getApplicationLabel(context, packageInfo.packageName) + "###" + "\n" + packageInfo.packageName + "\n";
                    if (isINSTALLED(packageInfo)) {
                        value += "- INSTALLED\n";
                    }
                    if (isSTOPPED(packageInfo)) {
                        value += "- STOPPED\n";
                    }
                    if (isSUSPENDED(packageInfo)) {
                        value += "- SUSPENDED\n";
                    }
                    if (isPERSISTENT(packageInfo)) {
                        value += "- PERSISTENT\n";
                    }
                    // value += "MIN SDK: " + new Integer(packageInfo.minSdkVersion).toString() + "\n";
                    value += "-----------------------\n";
                }
            // }
        }
        /* */

        /*
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> servInfos = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (servInfos != null)
        {
            for (final ActivityManager.RunningServiceInfo serviceInfo : servInfos)
            {
                value += "###" + serviceInfo.getClass().getName() + "###\n";
                value += "###" + serviceInfo.getClass().getCanonicalName() + "###\n";
                value += "###" + serviceInfo.getClass().getSimpleName() + "###" + "\n-----------------------\n";
                if (processInfo.processName.equals(packageName)) {
                    return true;
                }
            }
        }
        */

        return value;
    }

    }


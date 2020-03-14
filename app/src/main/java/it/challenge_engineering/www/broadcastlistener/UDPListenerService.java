package it.challenge_engineering.www.broadcastlistener;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Service;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

// import org.apache.http.util.ExceptionUtils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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

import static android.os.Process.THREAD_PRIORITY_DISPLAY;
import static android.os.Process.THREAD_PRIORITY_FOREGROUND;


/**
 * Created by Marco on 31/03/2017.
 */

/*
 * Linux command to send UDP:
 * #socat - UDP-DATAGRAM:192.168.1.255:11111,broadcast,sp=11111
 */
public class UDPListenerService extends Service
{
    // TextView mText;
    Context mActivityContext;
    public int messageCounter = 0;

    final static String SENDMESAGGE = "passMessage";
    static String UDP_BROADCAST = "UDPBroadcast";
    public String appNameVersion = "";

    //Boolean shouldListenForUDPBroadcast = false;
    static DatagramSocket socket;

    private ServerConnection sConn = null;
    private String ipSmartphoneAddress = "0.0.0.0";

    private int timeOutCounter = 0;
    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)

    private void StopListen() {
        shouldRestartSocketListen = false;
        socket.close();
    }

    private void listenAndWaitAndThrowIntent(InetAddress broadcastIP, Integer port) throws Exception
    {
        /*
        if (alarmReceptionSuspended == true)
        {
            Thread.sleep(10);
            return;
        }
        */
        if (socket == null)
        {
            try
            {
                // socket = new DatagramSocket(port, broadcastIP);

                SocketAddress socketAdd = new InetSocketAddress(broadcastIP,port);
                socket = new DatagramSocket(null);

                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.bind(socketAdd);
            }
            catch (SocketException ex)
            {
                // mText.setText("[EXC001] - " + ex.getMessage());
                Log.e("[CE-CDG-01]-UL", "Socket creation exception: " + ex.getMessage());
                socket.close();
                return;
            }
        }

        if (socket.isClosed())
        {
            try
            {
                SocketAddress socketAdd = new InetSocketAddress(broadcastIP,port);
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.bind(socketAdd);
            }
            catch (BindException ex)
            {
                // mText.setText("[EXC001] - " + ex.getMessage());
                Log.e("[CE-CDG-01]-UL", "Socket binding [2] exception: " + ex.getMessage());
                socket = null;
                return;
            }
            catch (SocketException ex)
            {
                // mText.setText("[EXC001] - " + ex.getMessage());
                Log.e("[CE-CDG-01]-UL", "Socket creating [2] exception: " + ex.getMessage());
                socket.close();
                return;
            }
        }

        socket.setSoTimeout(4000);
        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
        Log.i("[" + Thread.currentThread().toString() + "][CE-CDG-01]-UL", "Waiting for UDP broadcast");

        try
        {
            socket.receive(packet);
            Log.i("[CE-CDG-01]-UL", "Packet received: " + Integer.toString(messageCounter));
            socket.close();
            messageCounter++;
            if (messageCounter > 30)
            {
                Log.i("[CE-CDG-01]-UL", "Send Main activity a request for Watchdog to server");
                messageCounter = 0;
                broadcastIntent("", "{\"tipoMessaggio\": \"WATCHDOG\"}");
            }
        }
        catch (SocketTimeoutException tex)
        {
            // messageCounter = 0;
            Log.e("[CE-CDG-01]-UL", "Socket Timeout exception: " + tex.getMessage());
            timeOutCounter++;
            if (timeOutCounter >= 15)   // se non ricevo messaggi per un minuto
            {
                Log.e("[CE-CDG-01]-UL", "Send time-out message to Main Activity");
                broadcastIntent("", "{\"tipoMessaggio\": \"TIMEOUT\"}");
                timeOutCounter = 0;
            }
            socket.close();
            return;
        }
        catch (Exception ex)
        {
            // messageCounter = 0;
            Log.e("[CE-CDG-01]-UL", "Socket generic exception: " + ex.getMessage());
            socket.close();
            return;
        }

        timeOutCounter = 0;
        final String senderIP = packet.getAddress().getHostAddress();
        final String message = new String(packet.getData()).trim();

        // Log.e("[CE-CDG-01]-UL", "Got UDB broadcast from " + senderIP + ", message: " + message);

        // messageCounter++;
        JSONObject obj = null;
        JSONArray listaOspiti = null;
        String messageType = "";
        try
        {
            obj = new JSONObject(message);
        }
        catch (JSONException e)
        {
            Log.e("[CE-CDG-01]-UL", "JSON exception: " + e.getMessage());
            e.printStackTrace();
        }

        try
        {
            Log.i("[" + Thread.currentThread().toString() + "][CE-CDG-01]-UL", "----> BROADCAST: " + message);
            broadcastIntent(senderIP, message);
        }
        catch (Exception e)
        {
            Log.e("[CE-CDG-01]-UL", "BroadcastIntent() exception: " + e.getMessage());
            e.printStackTrace();
        }

        socket.close();
    }


    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private void broadcastIntent(String senderIP, String message) throws JSONException {
        JSONObject obj = null;
        JSONArray listaOspiti = null;
        String messageType = "";
        try
        {
            obj = new JSONObject(message);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        messageType = obj.getString("tipoMessaggio");
        if (messageType.equals("ALARM"))
        {
            Log.i("[" + Thread.currentThread().toString() + "][CE-CDG-01]-UL", "Broadcasting intent: ALARM");
            // PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isDestroyed", false).commit();
            Intent dialogIntent = new Intent(this, MainActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialogIntent);
        }
        else if (messageType.equals("WATCHDOG"))
        {
            // Log.e("[CE-CDG-01]-UL", "Sending Watchdog");
            // PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isDestroyed", false).commit();
            if (sConn != null) {
                // sConn.SendCommandToURL("http://192.168.1.232:8080/?op=WATCHDOG_SM&ipaddress=" + ipLocal + "&stato=0&client=APP", false);
                sConn.SendCommandToURL("http://192.168.1.232:8080/?op=WATCHDOG_SM&ipaddress="+ipSmartphoneAddress+"&stato=0&client=APP", false);
                return;
            }
        }

    Intent intent = new Intent(UDPListenerService.UDP_BROADCAST);
        intent.putExtra("sender", senderIP);
        intent.putExtra("message", message);
        mActivityContext.sendBroadcast(intent);
    }

    Thread UDPBroadcastThread = null;


    public void setServiceParmeters(TextView text, Activity activity, String ApplicationNameAndVersion)
    {
        appNameVersion = ApplicationNameAndVersion;
        // mText = text;
        // mActivity = activity;
    }

    byte[] recvBuf;
    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    void startListenForUDPBroadcast()
    {
        Log.i("[CE-CDG-01]-UL", "--------------------------------------------");
        recvBuf = new byte[5000];
        shouldRestartSocketListen = true;

            UDPBroadcastThread = new Thread(new Runnable() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
                public void run() {
                    try {
                        Log.i("[" + Thread.currentThread().toString() + "][CE-CDG-01]-UL", "running Thread");

                        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInf = wifiMan.getConnectionInfo();
                        int ipAddress = wifiInf.getIpAddress();
                        ipSmartphoneAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

                        InetAddress broadcastIP = InetAddress.getByName(ipSmartphoneAddress);
                        broadcastIntent(ipSmartphoneAddress, "{\"tipoMessaggio\": \"INFO\",\"ipAddress\": \"" + ipSmartphoneAddress + "\"}");
                        Log.i("[CE-CDG-01]-UL", "Send IP address to Main activity: " + ipSmartphoneAddress);

                        Integer port = 11111;
                        timeOutCounter = 0;
                        while (shouldRestartSocketListen) {
                            if (Thread.currentThread().isInterrupted())
                            {
                                Log.i("[" + Thread.currentThread().toString() +  "][CE-CDG-01]-UL", "Exit Thread");
                                // StopListen();
                                return;
                            }
                            // Log.i("[CE-CDG-01]-UL", "Restart Thread");
                            try {
                                listenAndWaitAndThrowIntent(broadcastIP, port);
                            } catch (Exception e) {
                                Log.i("[CE-CDG-01]-UL", "Error listening for UDP broadcasts: " + e.getMessage());
                            }
                        }
                        //if (!shouldListenForUDPBroadcast) throw new ThreadDeath();
                    } catch (Exception e) {
                        Log.i("[CE-CDG-01]-UL", "No longer listening for UDP broadcasts cause of error " + e.getMessage());
                    }
                }
            });

        /*
        try {
            UDPBroadcastThread.setPriority(THREAD_PRIORITY_FOREGROUND);
        }
        catch (Exception e)
        {
            Log.i("UDP", "no longer listening for UDP broadcasts cause of error " + e.getMessage());
        }
        */

        UDPBroadcastThread.start();
    }

    private Boolean shouldRestartSocketListen = true;

    @Override
    public void onCreate()
    {
        mActivityContext = getApplicationContext();
    };

    @Override
    public void onDestroy()
    {
        // Log.i("CE-CDG-01", "onDestroy service...");
        // Intent broadcastIntent = new Intent("it.challenge_engineering.broadcastlistener.RESTART_SERVICE");

        if (UDPBroadcastThread != null)
        {
            UDPBroadcastThread.interrupt();
        }

        Intent broadcastIntent = new Intent(this, SensorRestarterBroadcastReceiver.class);
        sendBroadcast(broadcastIntent);

        Intent dialogIntent = new Intent(this, MainActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);

        // stopListen();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (sConn == null)
        {
            sConn = new ServerConnection(this);
        }

        shouldRestartSocketListen = true;
        // Log.i("[CE-CDG-01]-UL", "executing onStartCommand()");
        startListenForUDPBroadcast();
        return START_STICKY;
        // return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
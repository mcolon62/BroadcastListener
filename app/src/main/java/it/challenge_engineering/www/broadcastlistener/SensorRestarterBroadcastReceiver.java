package it.challenge_engineering.www.broadcastlistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Marco on 03/02/2018.
 */

public class SensorRestarterBroadcastReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i("[CE-CDG-01]-SR", "restarting app...");
        // Log.i(SensorRestarterBroadcastReceiver.class.getSimpleName(), "Service Stops! Oooooooooooooppppssssss!!!!");
        context.startService(new Intent(context, UDPListenerService.class));
    }
}

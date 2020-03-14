package it.challenge_engineering.www.broadcastlistener;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Created by Marco on 14/07/2017.
 * */

public class ServerConnection
{

    // private TextView mTextView;
    private Context mContext;
    private RequestQueue mQueue;

    public ServerConnection(Context context /*, TextView tv*/)
    {
        mContext = context;
        // mTextView = tv;
        mQueue = Volley.newRequestQueue(mContext);
    }

    public Boolean SendCommandToURL(String url, Boolean isAlarmStop)
    {
        try {
            if (mQueue == null) {
                mQueue = Volley.newRequestQueue(mContext);
            }
            if (isAlarmStop) {
                // Request a string response from the provided URL.
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                ((MainActivity) mContext).alarmReceptionSuspended = false;
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                ((MainActivity) mContext).alarmReceptionSuspended = false;
                            }
                        });
                mQueue.add(stringRequest);
            } else {
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {

                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });
                mQueue.add(stringRequest);
            }
        }
        catch (Exception ex)
        {
            //Log.e("[CE-CDG-01]-SC", "EXCEPTION in SendCommandToURL: " + ex.getMessage());
            return false;
        }
        return true;

    }

}

package com.hafiztraveltours.app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StopAzanReceiver extends BroadcastReceiver {
    private static final String TAG = "StopAzanReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Stop Azan broadcast received.");
        AzanService.stopActivePlayback(context);
    }
}
package com.hafiztraveltours.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopAzanReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent stopServiceIntent = new Intent(context, AzanService.class);
        context.stopService(stopServiceIntent);
    }
}
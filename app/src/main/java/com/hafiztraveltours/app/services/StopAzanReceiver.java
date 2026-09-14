package com.hafiztraveltours.app.services;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


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
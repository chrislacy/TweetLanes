package com.tweetlanes.android.core.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tweetlanes.android.core.Notifier;

public class DeviceBootReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Notifier.setNotificationAlarm(context);
        }
    }
}


package com.tweetlanes.android;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import com.tweetlanes.android.view.HomeActivity;

public class Notifier {

    public static void Notify(String title, String text, Boolean autoCancel, int id,
            String accountKey, Context context) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setTicker(text)
                .setSmallIcon(R.drawable.notification_default)
                .setAutoCancel(autoCancel)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(text);

        Intent resultIntent = new Intent(context, HomeActivity.class);
        resultIntent.putExtra("account_key", accountKey);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(HomeActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context
                .NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, mBuilder.build());

    }
}

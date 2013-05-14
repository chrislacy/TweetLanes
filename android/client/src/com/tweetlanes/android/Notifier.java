package com.tweetlanes.android;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import com.tweetlanes.android.view.AlarmReceiver;
import com.tweetlanes.android.view.DeleteNotificationsReceiver;
import com.tweetlanes.android.view.HomeActivity;

public class Notifier {

    public static final String SHARED_PREFERENCES_KEY_NOTIFICATION_LAST_ACTIONED_MENTION_ID =
            "notification_last_actioned_mention_id_v1_";
    public static final String SHARED_PREFERENCES_KEY_NOTIFICATION_LAST_DISPLAYED_MENTION_ID =
            "notification_last_displayed_mention_id_v1_";

    public static void notify(String title, String text, String bigText, Boolean autoCancel, int id,
            String accountKey, long postId, Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setTicker(text)
                .setSmallIcon(R.drawable.notification_default)
                .setAutoCancel(autoCancel)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));

        Uri ringtone = AppSettings.get().getRingtoneUri();
        if (ringtone != null) {
            builder.setSound(ringtone);
        }

        Intent resultIntent = new Intent(context, HomeActivity.class);
        resultIntent.putExtra("account_key", accountKey);
        resultIntent.putExtra("post_id", postId);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(HomeActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        int requestCode = (int)(Math.random() * Integer.MAX_VALUE);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(requestCode,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);

        Intent deleteIntent = new Intent(context, DeleteNotificationsReceiver.class);
        deleteIntent.putExtra("account_key", accountKey);
        deleteIntent.putExtra("post_id", postId);
        requestCode = (int)(Math.random() * Integer.MAX_VALUE);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, requestCode, deleteIntent, 0);

        builder.setDeleteIntent(deletePendingIntent);

        saveLastNotificationDisplayed(context, accountKey, postId);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context
                .NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    public static void cancelAll(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context
                .NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public static void setupNotificationAlarm(Context context) {
        //Create a new PendingIntent and add it to the AlarmManager
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 12345, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager)context.getSystemService(Activity.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
    }

    public static void cancelNotificationAlarm(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 12345, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager)context.getSystemService(Activity.ALARM_SERVICE);
        am.cancel(pendingIntent);
    }

    public static void saveLastNotificationActioned(Context context, String accountKey, long postId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putLong(SHARED_PREFERENCES_KEY_NOTIFICATION_LAST_ACTIONED_MENTION_ID  + accountKey, postId);
        edit.commit();
    }

    public static void saveLastNotificationDisplayed(Context context, String accountKey, long postId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putLong(SHARED_PREFERENCES_KEY_NOTIFICATION_LAST_DISPLAYED_MENTION_ID  + accountKey, postId);
        edit.commit();
    }
}

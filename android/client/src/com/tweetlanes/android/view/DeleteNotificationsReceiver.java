package com.tweetlanes.android.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

/**
 * Created with IntelliJ IDEA.
 * User: Jason
 * Date: 4/8/13
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class DeleteNotificationsReceiver extends BroadcastReceiver {

    final String SHARED_PREFERENCES_KEY_NOTIFICATION_LAST_MENTION_ID = "notification_last_mention_id_v1_";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String accountKey = extras.getString("account_key");
            long postId = extras.getLong("post_id");

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor edit = preferences.edit();
            edit.putLong(SHARED_PREFERENCES_KEY_NOTIFICATION_LAST_MENTION_ID + accountKey, postId);
            edit.commit();
        }
    }
}

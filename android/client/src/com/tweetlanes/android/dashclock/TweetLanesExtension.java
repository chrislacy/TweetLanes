package com.tweetlanes.android.dashclock;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.tweetlanes.android.R;
import com.tweetlanes.android.view.BootActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class TweetLanesExtension extends DashClockExtension {

    private static final String TAG = "ExampleExtension";

    public static final String PREF_NAME = "pref_name";
	
	@Override
	protected void onUpdateData(int arg0) {
        // Get preference value.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        String unreadTweets = "99 unread tweets";	
        
        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(true)
                .icon(R.drawable.ic_launcher)
                .status("99")
                .expandedTitle("99 unread messages")
                .expandedBody("Last updated 45 minutes ago")
                .clickIntent(new Intent(this, BootActivity.class)));
        }

}
/*
 * Copyright (C) 2013 Chris Lacy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tweetlanes.android.service;

import java.util.ArrayList;

import com.tweetlanes.android.R;
import com.tweetlanes.android.view.HomeActivity;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class BackgroundService extends Service {

	
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_SET_INT_VALUE = 3;
	public static final int MSG_SET_STRING_VALUE = 4;
	
	private static boolean mIsRunning = false;
	
	ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    int mValue = 0; // Holds last value set by a client.
    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.

	
	//private final ServiceStub mBinder = new ServiceStub(this);

	Context mContext;
	
	private NotificationManager mNotificationManager;
	private BroadcastReceiver mStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			mContext = context;
			/*
			if (BROADCAST_REFRESHSTATE_CHANGED.equals(action)) {
				if (!mAsyncTaskManager.hasActivatedTask() && mShouldShutdown) {
					stopSelf();
				}
			} else if (BROADCAST_NOTIFICATION_CLEARED.equals(action)) {
				final Bundle extras = intent.getExtras();
				if (extras != null && extras.containsKey(INTENT_KEY_NOTIFICATION_ID)) {
					clearNotification(extras.getInt(INTENT_KEY_NOTIFICATION_ID));
				}
			}*/
		}

	};

	private boolean mShouldShutdown = false;

	int mCount = 0;
	
	private static final int ACTION_AUTO_REFRESH = 1;

	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case ACTION_AUTO_REFRESH: {
					/*
					final long[] activated_ids = getActivatedAccountIds(TwidereService.this);
					if (mPreferences.getBoolean(PREFERENCE_KEY_REFRESH_ENABLE_HOME_TIMELINE, false)) {
						if (!isHomeTimelineRefreshing()) {
							getHomeTimeline(activated_ids, null, true);
						}
					}
					if (mPreferences.getBoolean(PREFERENCE_KEY_REFRESH_ENABLE_MENTIONS, false)) {
						if (!isMentionsRefreshing()) {
							getMentions(activated_ids, null, true);
						}
					}
					if (mPreferences.getBoolean(PREFERENCE_KEY_REFRESH_ENABLE_DIRECT_MESSAGES, false)) {
						if (!isReceivedDirectMessagesRefreshing()) {
							getReceivedDirectMessages(activated_ids, null, true);
						}
					}*/
					//Log.d("Notifications", "Tick [" + mCount + "]");
					mCount += 1;
					mHandler.removeMessages(ACTION_AUTO_REFRESH);
					
					sendMessageToUI(mCount);
					//showNotification();
					
					//Toast.makeText(mContext, "Toast!!!!", Constant.DEFAULT_TOAST_DISPLAY_TIME).show();
					
					//final long update_interval = parseInt(mPreferences.getString(PREFERENCE_KEY_REFRESH_INTERVAL, "30")) * 60 * 1000;
					final long update_interval = 20 * 1000;
					//if (update_interval <= 0 || !mPreferences.getBoolean(PREFERENCE_KEY_AUTO_REFRESH, false)) {
					//	break;
					//}
					mHandler.sendEmptyMessageDelayed(ACTION_AUTO_REFRESH, update_interval);
					break;
				}
			}
		}
	};

	public void clearNotification(int id) {
		switch (id) {
		/*
			case NOTIFICATION_ID_HOME_TIMELINE: {
				mNewStatusesCount = 0;
				break;
			}
			case NOTIFICATION_ID_MENTIONS: {
				mNewMentionsCount = 0;
				break;
			}
			case NOTIFICATION_ID_DIRECT_MESSAGES: {
				mNewMessagesCount = 0;
				break;
			}*/
		}
		mNotificationManager.cancel(id);
	}


	//@Override
	//public IBinder onBind(Intent intent) {
	//	return mBinder;
	//}
	
	@Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    
    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case MSG_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            case MSG_SET_INT_VALUE:
                //incrementby = msg.arg1;
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    private void sendMessageToUI(int intvaluetosend) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send data as an Integer
                mClients.get(i).send(Message.obtain(null, MSG_SET_INT_VALUE, intvaluetosend, 0));

                //Send data as a String
                Bundle b = new Bundle();
                b.putString("str1", "ab" + intvaluetosend + "cd");
                Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
                msg.setData(b);
                mClients.get(i).send(msg);

            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

	@Override
	public void onCreate() {
		super.onCreate();
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		//mAsyncTaskManager = ((TwidereApplication) getApplication()).getAsyncTaskManager();
		//mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		//final IntentFilter filter = new IntentFilter(BROADCAST_REFRESHSTATE_CHANGED);
		//filter.addAction(BROADCAST_NOTIFICATION_CLEARED);
		//registerReceiver(mStateReceiver, filter);
		mIsRunning = true;
		startAutoRefresh();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mStateReceiver);
		mNotificationManager.cancelAll();
		//if (mPreferences.getBoolean(PREFERENCE_KEY_AUTO_REFRESH, false)) {
			// Auto refresh enabled, so I will try to start service after it was
			// stopped.
			//startService(new Intent(INTENT_ACTION_SERVICE));
		//}
		mIsRunning = false;
		super.onDestroy();
	}

	public void shutdownService() {
		// Auto refresh is enabled, so this service cannot be shut down.
		/*
		if (mPreferences.getBoolean(PREFERENCE_KEY_AUTO_REFRESH, false)) return;
		if (!mAsyncTaskManager.hasActivatedTask()) {
			stopSelf();
		} else {
			mShouldShutdown = true;
		}*/
	}

	public boolean startAutoRefresh() {
		//if (mPreferences.getBoolean(PREFERENCE_KEY_AUTO_REFRESH, false)) {
			//final long update_interval = parseInt(mPreferences.getString(PREFERENCE_KEY_REFRESH_INTERVAL, "30")) * 60 * 1000;
			Log.d("Notifications", "START");
			//Toast.makeText(mContext, "Start!!!!", Constant.DEFAULT_TOAST_DISPLAY_TIME).show();
			final long update_interval = 20 * 1000;
			if (update_interval <= 0) return false;
			mHandler.sendEmptyMessageDelayed(ACTION_AUTO_REFRESH, update_interval);
			return true;
		//}
		//return false;
	}

	public void stopAutoRefresh() {
		mHandler.removeMessages(ACTION_AUTO_REFRESH);
	}

	/*
	 * 
	 */
	public static boolean isRunning() {
        return mIsRunning;
    }

	private Notification buildNotification(String message, int icon, Intent content_intent, Intent delete_intent) {
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setTicker(message);
		//builder.setContentTitle(getString(R.string.new_notifications));
		builder.setContentTitle("New notifications");
		builder.setContentText(message);
		builder.setAutoCancel(true);
		builder.setWhen(System.currentTimeMillis());
		builder.setSmallIcon(icon);
		builder.setDeleteIntent(PendingIntent.getBroadcast(this, 0, delete_intent, PendingIntent.FLAG_UPDATE_CURRENT));
		builder.setContentIntent(PendingIntent.getActivity(this, 0, content_intent, PendingIntent.FLAG_UPDATE_CURRENT));
		int defaults = 0;
		//if (mPreferences.getBoolean(PREFERENCE_KEY_NOTIFICATIONS_HAVE_SOUND, false)) {
			defaults |= Notification.DEFAULT_SOUND;
		//}
		//if (mPreferences.getBoolean(PREFERENCE_KEY_NOTIFICATIONS_HAVE_VIBRATION, false)) {
			defaults |= Notification.DEFAULT_VIBRATE;
		//}
		//if (mPreferences.getBoolean(PREFERENCE_KEY_NOTIFICATIONS_HAVE_LIGHTS, false)) {
			defaults |= Notification.DEFAULT_LIGHTS;
		//}
		builder.setDefaults(defaults);
		return builder.getNotification();
	}
	
	private NotificationManager nm;
	private void showNotification() {
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // In this sample, we'll use the same text for the ticker and the expanded notification
        //CharSequence text = getText(R.string.service_started);
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, "Message preview here", System.currentTimeMillis());
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, HomeActivity.class), 0);
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, "Count = " + mCount, "Full message here", contentIntent);
        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        nm.notify(112344, notification);
    }

	private void showErrorToast(Exception e, boolean long_message) {
		//Utils.showErrorToast(this, e, long_message);
	}
}

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

package com.tweetlanes.android;

import com.tweetlanes.android.model.AccountDescriptor;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;

public class NotificationHelper {
	
	/*
	 * 
	 */
	public class Handle {
		private Handle(int handle) 	{ mHandle = handle; }
		private int get()		{ return mHandle; }
		private int mHandle;
	}
	
	/*
	 * 
	 */
	public class Builder {
		Notification.Builder mBuilder;
		
		public Builder(Activity activity, boolean setAppDefaults) {
			mBuilder = new Notification.Builder(activity.getApplicationContext());
			
			if (setAppDefaults) {
				setSmallIcon(R.drawable.notification_default);
				setWhen(System.currentTimeMillis());
				
				AccountDescriptor currentAccount = ((App)(activity.getApplication())).getCurrentAccount();
				if (currentAccount != null) {
					Bitmap largeIcon = currentAccount.getProfileImage();
		        	if (largeIcon != null) {
		        		setLargeIcon(largeIcon);
		        	}
				}
			}
			
		}
		
		private Notification.Builder 	get() 	{ return mBuilder; }
		
		public Builder setAutoCancel(boolean autoCancel)			{ mBuilder.setAutoCancel(autoCancel);	return this; }
		public Builder setContentIntent(PendingIntent intent)		{ mBuilder.setContentIntent(intent); 	return this; }
		public Builder setContentText(CharSequence text)			{ mBuilder.setContentText(text);		return this; }
		public Builder setContentTitle(CharSequence title)			{ mBuilder.setContentTitle(title);		return this; }
		public Builder setLargeIcon(Bitmap icon)					{ mBuilder.setLargeIcon(icon);			return this; }
		public Builder setSmallIcon(int icon)						{ mBuilder.setSmallIcon(icon); 			return this; }
		public Builder setTicker(CharSequence tickerText)			{ mBuilder.setTicker(tickerText); 		return this; }
		public Builder setOngoing(boolean ongoing)					{ mBuilder.setOngoing(ongoing);			return this; }
		public Builder setWhen(long when)							{ mBuilder.setWhen(when);				return this; }
		 //.setContentTitle("Tweet Posted")
        //.setContentText("Content of tweet goes here");
		
	}
	
	/*
	 * TODO: This is probably too C++ ish. Will come back to this later...
	 */
	public static void initModule() 		{ mInstance = new NotificationHelper(); }
	public static void deinitModule() 		{ mInstance = null;	}
	public static NotificationHelper get() { return mInstance; }
	private static NotificationHelper mInstance = null;

	private NotificationHelper() {
		mLastHandleIndex = 0;
	}
	
	/*
	 * 
	 */
	private Handle getNextHandle() {
		Handle handle = new Handle(mLastHandleIndex);
		mLastHandleIndex++;
		return handle;
	}
	
	/*
	 * 
	 */
	public Handle notify(Activity activity, Builder builder) {

		NotificationManager notificationManager = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = builder.get().getNotification();
        notificationManager.notify(mLastHandleIndex, notification);
        return getNextHandle();
	}
	
	/*
	 * 
	 */
	public void cancel(Activity activity, Handle handle) {
		NotificationManager manager = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(handle.get());
	}
	
	
	private int mLastHandleIndex;
}

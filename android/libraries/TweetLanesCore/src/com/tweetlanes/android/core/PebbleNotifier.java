package com.tweetlanes.android.core;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.tweetlanes.android.core.Constant.SystemEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PebbleNotifier {
	public PebbleNotifier(App app){
	this.app = app;	
	}
   public App app;
   private final static UUID PEBBLE_APP_UUID = UUID.fromString("55705D2D-0D80-4F18-A1F1-772B18BE2C13");
   private final static int MSG_CMD_OUT_KEY = 0x00;

   private final static int MSG_CMD_VIBRATE = 0x01;

	public void notify(String title, String text){
	    boolean isVibrationsActive = false;
	    boolean isNotificationsActive = AppSettings.get().isPebbleNotificationsEnabled();
	    
	    if(isVibrationsActive == true){
	    vibrate();	
	    }
	    if(isNotificationsActive == true){
		final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

	    final Map<String, String> data = new HashMap<String, String>();
	    data.put("title", title);
	    data.put("body", text);
	    final JSONObject jsonData = new JSONObject(data);
	    final String notificationData = new JSONArray().put(jsonData).toString();

	    i.putExtra("messageType", "PEBBLE_ALERT");
	    i.putExtra("sender", "TweetLanes");
	    i.putExtra("notificationData", notificationData);
	    
	    System.out.println("About to send a modal alert to Pebble: " + notificationData);	 
	    app.sendBroadcast(i);	
	    }
	}
	public void vibrate(){
	PebbleKit.startAppOnPebble(app, PEBBLE_APP_UUID);
	PebbleDictionary data = new PebbleDictionary();
    data.addUint8(MSG_CMD_OUT_KEY, (byte) MSG_CMD_VIBRATE);
    PebbleKit.sendDataToPebble(app, PEBBLE_APP_UUID, data);
    PebbleKit.closeAppOnPebble(app, PEBBLE_APP_UUID);
	}
	

	public static void startWatchApp(Context c) {
        PebbleKit.startAppOnPebble(c, PEBBLE_APP_UUID);
    }
	public void onUpButton(){
		System.out.println("up");
		if(AppSettings.get().isPebbleVolscrollEnabled() == true){
		Intent intent = new Intent(""
                + SystemEvent.VOLUME_UP_KEY_DOWN);
        LocalBroadcastManager.getInstance(app).sendBroadcast(
                intent);
		}
		}
	public void onDownButton(){
		System.out.println("down");
		if(AppSettings.get().isPebbleVolscrollEnabled() == true){
		Intent intent = new Intent(""
                + SystemEvent.VOLUME_DOWN_KEY_DOWN);
        LocalBroadcastManager.getInstance(app).sendBroadcast(
                intent);
		}
		}
}

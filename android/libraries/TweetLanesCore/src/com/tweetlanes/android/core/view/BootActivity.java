/*
 * Copyright (C) 2013 Chris Lacy Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.tweetlanes.android.core.view;

import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.crittercism.app.Crittercism;
import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;

public class BootActivity extends Activity {

    Class<?> mLastStartedClass;

    public App getApp() {
        return (App) getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(AppSettings.get().getCurrentThemeStyle());

        if (Constant.ENABLE_CRASH_TRACKING) {
            Crittercism.init(getApplicationContext(),
                    Constant.CRITTERCISM_APP_ID);
        }

        // LocalBroadcastManager.getInstance(this).registerReceiver(mOAuthLoginStateChangeReceiver,
        // new IntentFilter("" + SystemEvent.OAuthLoginStateChange));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        jumpToNext();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        jumpToNext();

    }

    @Override
    protected void onResume() {
        super.onResume();
        jumpToNext();
    }

    @Override
    protected void onDestroy() {
        // LocalBroadcastManager.getInstance(this).unregisterReceiver(mOAuthLoginStateChangeReceiver);
        super.onDestroy();
    }

    /*
     *
	 */
    /*
     * private BroadcastReceiver mOAuthLoginStateChangeReceiver = new
     * BroadcastReceiver() {
     *
     * @Override public void onReceive(Context context, Intent intent) {
     * jumpToNext(); } };
     */

    /*
	 *
	 */
    void jumpToNext() {

        int accountCount = getApp().getAccountCount();
        if (accountCount == 0) {
            Intent intent = new Intent(getApplicationContext(), NewAccountActivity.class);
            overridePendingTransition(0, 0);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        } else {
            if (TwitterManager.get().hasValidTwitterInstance() == true) {
                if (mLastStartedClass != HomeActivity.class) {
                    mLastStartedClass = HomeActivity.class;
                    // We don't want to come back here, so remove from the
                    // activity stack
                    finish();

                    Class<?> nextClass = HomeActivity.class;
                    if (getApp().getTutorialCompleted() == false) {
                        nextClass = TutorialActivity.class;
                    }
                    Intent intent = new Intent(getApplicationContext(),
                            nextClass);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    overridePendingTransition(0, 0);
                    startActivity(intent);
                }
            } else {
                // TODO: Handle this case
            }
        }
    }
}

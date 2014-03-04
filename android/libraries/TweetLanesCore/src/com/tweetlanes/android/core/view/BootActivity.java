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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.crittercism.app.Crittercism;
import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.ConsumerKeyConstants;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.model.AccountDescriptor;

import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterManager;

public class BootActivity extends Activity {

    private Class<?> mLastStartedClass;

    App getApp() {
        return (App) getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Constant.ENABLE_CRASH_TRACKING) {
            Crittercism.initialize(getApplicationContext(),
                    ConsumerKeyConstants.CRITTERCISM_APP_ID);
        }

        setTheme(AppSettings.get().getCurrentThemeStyle());
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
            if (TwitterManager.get().hasValidTwitterInstance()) {

                final Uri uriData = getIntent().getData();
                if (uriData != null) {

                    if (!ReadUrl(uriData, false)) {
                        startHomeActivity("", uriData.toString());
                    }
                    getIntent().setData(null);
                } else if (mLastStartedClass != HomeActivity.class) {
                    mLastStartedClass = HomeActivity.class;
                    // We don't want to come back here, so remove from the
                    // activity stack
                    finish();


                    Class<?> nextClass = HomeActivity.class;
                    if (!getApp().getTutorialCompleted()) {
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

    private boolean ReadUrl(Uri uriData, boolean recuse) {
        String host = uriData.getHost();
        String urlPath = uriData.getPath();
        boolean urlValid = false;
        finish();

        if (host.contains("twitter"))
        {
            if (getApp().getCurrentAccount().getSocialNetType() != SocialNetConstant.Type.Twitter) {
                changeToFirstAccountOfType(SocialNetConstant.Type.Twitter);
            }


            if (urlPath.contains("/status/")) {
                String statusId = getUriPartAfterText(uriData, "status");
                startTweetSpotlight(statusId);
                urlValid = true;
            } else if (urlPath.contains("/intent/tweet")) {
                if (uriData.getQueryParameterNames().contains("url")) {
                    String statusId = uriData.getQueryParameter("in_reply_to");
                    startTweetSpotlight(statusId);
                } else {
                    String statusText = uriData.getQueryParameter("text");
                    if (uriData.getQueryParameterNames().contains("url")) {
                        statusText = statusText + " " + uriData.getQueryParameter("url");
                    }
                    if (uriData.getQueryParameterNames().contains("hashtags")) {
                        String[] hashtags = uriData.getQueryParameter("hashtags").split(",");
                        for (String hashtag : hashtags) {
                            statusText = statusText + " #" + hashtag;
                        }
                    }
                    startHomeActivity(statusText, "");
                }
                urlValid = true;
            } else if (urlPath.contains("/i/redirect") && recuse == false) {
                String innerUrl = uriData.getQueryParameter("url");
                urlValid = ReadUrl(Uri.parse(innerUrl), true);
            } else if (urlPath.contains("/intent/follow") || urlPath.contains("/intent/user")) {
                String userName = uriData.getQueryParameter("screen_name");
                startProfileSpotlight(userName);
                urlValid = true;
            } else if (urlPath.contains("/intent/follow") || urlPath.contains("/intent/user")) {
                String userName = uriData.getQueryParameter("screen_name");
                startProfileSpotlight(userName);
                urlValid = true;
            } else if (urlPath.lastIndexOf("/") == 0 ||
                    (urlPath.indexOf("/") == 0 && urlPath.lastIndexOf("/") == urlPath.length() && CountInstancesOfChar(urlPath, '/') == 2)) {
                String userName = urlPath.substring(1);
                startProfileSpotlight(userName);
                urlValid = true;
            }
        }
        else if (host.contains("app.net"))
        {
            if (getApp().getCurrentAccount().getSocialNetType() != SocialNetConstant.Type.Appdotnet) {
                changeToFirstAccountOfType(SocialNetConstant.Type.Appdotnet);
            }

            if (urlPath.contains("/post/")) {
                String statusId = getUriPartAfterText(uriData, "post");
                startTweetSpotlight(statusId);
                urlValid = true;
            } else if (urlPath.lastIndexOf("/") == 0 ||
                    (urlPath.indexOf("/") == 0 && urlPath.lastIndexOf("/") == urlPath.length() && CountInstancesOfChar(urlPath, '/') == 2)) {
                String userName = urlPath.substring(1);
                startProfileSpotlight(userName);
                urlValid = true;
            }
        }

        return urlValid;
    }

    private int CountInstancesOfChar(String testString, Character CharInstance) {
        int counter = 0;
        for (int i = 0; i < testString.length(); i++) {
            if (testString.charAt(i) == CharInstance) {
                counter++;
            }
        }
        return counter;
    }

    private String getUriPartAfterText(Uri uriData, String partBefore) {
        boolean nextPartStatus = false;
        for (String uriPart : uriData.getPathSegments()) {
            if (nextPartStatus == true) {
                return uriPart;
            }
            if (uriPart.toLowerCase().equals(partBefore)) {
                nextPartStatus = true;
            }
        }

        return "";
    }

    private void changeToFirstAccountOfType(SocialNetConstant.Type socialNetType) {
        for (AccountDescriptor account : getApp().getAccounts()) {
            if (account.getSocialNetType() == socialNetType) {
                getApp().setCurrentAccount(account.getId());
                return;
            }
        }
    }

    private void startTweetSpotlight(String statusId) {
        Intent tweetSpotlightIntent = new Intent(this, TweetSpotlightActivity.class);
        tweetSpotlightIntent.putExtra("statusId", statusId);
        tweetSpotlightIntent.putExtra("clearCompose", "true");
        overridePendingTransition(0, 0);
        startActivity(tweetSpotlightIntent);
    }

    private void startHomeActivity(String composeText, String urlToLoad) {
        Intent homeIntent = new Intent(this, HomeActivity.class);
        homeIntent.setAction(Intent.ACTION_SEND);
        homeIntent.setType("text/plain");
        if (!composeText.isEmpty()) {
            homeIntent.putExtra(Intent.EXTRA_TEXT, composeText);
        }
        if (!urlToLoad.isEmpty()) {
            homeIntent.putExtra("urlToLoad", urlToLoad);
        }
        overridePendingTransition(0, 0);
        startActivity(homeIntent);
    }

    private void startProfileSpotlight(String userName) {
        Intent profileIntent = new Intent(this, ProfileActivity.class);
        profileIntent.putExtra("userScreenName", userName);
        profileIntent.putExtra("clearCompose", "true");
        overridePendingTransition(0, 0);
        startActivity(profileIntent);
    }
}

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

package com.tweetlanes.android.view;

import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.tweetlanes.android.*;
import org.socialnetlib.android.AppdotnetApi;
import org.socialnetlib.android.SocialNetConstant;

import com.crittercism.app.Crittercism;

import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterUser;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AppDotNetAuthActivity extends Activity {

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Constant.ENABLE_CRASH_TRACKING) {
            Crittercism.init(getApplicationContext(),
                    Constant.CRITTERCISM_APP_ID);
        }

        setTheme(AppSettings.get().getCurrentThemeStyle());

        getActionBar().setTitle(R.string.authorize_appdotnet_account);

        TwitterManager.get().setSignInSocialNetType(ConsumerKeyConstants.APPDOTNET_CONSUMER_KEY,
                ConsumerKeyConstants.APPDOTNET_CONSUMER_SECRET, SocialNetConstant.Type.Appdotnet);

        String url = "https://account.app.net/oauth/authenticate?client_id="
                + ConsumerKeyConstants.APPDOTNET_CONSUMER_KEY
                + "&response_type=token&redirect_uri=tweetlanes-auth-callback:///&scope=stream,write_post," +
                "follow,messages";

        setContentView(R.layout.twitter_auth_signin);

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        cookieManager.setAcceptCookie(true);

        WebView webView = (WebView) findViewById(R.id.twitter_auth_signin_webview);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("#access_token")) {
                    String accessToken = url.replace(
                            "tweetlanes-auth-callback:///#access_token=", "");

                    setContentView(R.layout.loading);
                    try {
                        TwitterUser user = new VerifyCredentialsTask().execute(
                                accessToken).get();

                        TwitterManager.get().setSocialNetType(
                                SocialNetConstant.Type.Appdotnet,
                                ConsumerKeyConstants.APPDOTNET_CONSUMER_KEY,
                                ConsumerKeyConstants.APPDOTNET_CONSUMER_SECRET,
                                user.getScreenName().toLowerCase() + "_appdotnet");

                        onSuccessfulLogin(user, accessToken);
                    } catch (Exception e) {
                        return false;
                    }
                }
                return false;
            }

        });

        webView.loadUrl(url);
    }

    /*
	 *
	 */
    void onSuccessfulLogin(TwitterUser user, String accessToken) {
        App app = (App) getApplication();
        app.onPostSignIn(user, accessToken, null,
                SocialNetConstant.Type.Appdotnet);
        app.restartApp(this);
    }

    private class VerifyCredentialsTask extends
            AsyncTask<String, Void, TwitterUser> {
        @Override
        protected TwitterUser doInBackground(String... accessTokens) {
            if (accessTokens.length == 0) {
                return null;
            }
            return new AppdotnetApi(SocialNetConstant.Type.Appdotnet,
                    ConsumerKeyConstants.APPDOTNET_CONSUMER_KEY,
                    ConsumerKeyConstants.APPDOTNET_CONSUMER_SECRET,
                    null).verifyCredentialsSync(
                    accessTokens[0], null);
        }
    }
}

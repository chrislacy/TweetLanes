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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.crittercism.app.Crittercism;
import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.ConsumerKeyConstants;
import com.tweetlanes.android.core.R;

import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterSignIn.GetAuthUrlCallback;
import org.tweetalib.android.TwitterSignIn.GetOAuthAccessTokenCallback;
import org.tweetalib.android.model.TwitterUser;

import twitter4j.auth.RequestToken;

public class TwitterAuthActivity extends Activity {

    private RequestToken mRequestToken;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Constant.ENABLE_CRASH_TRACKING) {
            Crittercism.initialize(getApplicationContext(), ConsumerKeyConstants.CRITTERCISM_APP_ID);
        }

        setTheme(AppSettings.get().getCurrentThemeStyle());

        setContentView(R.layout.loading);

        TwitterManager.get().setSignInSocialNetType(ConsumerKeyConstants.TWITTER_CONSUMER_KEY, ConsumerKeyConstants.TWITTER_CONSUMER_SECRET,
                SocialNetConstant.Type.Twitter);

        TwitterManager.get().getAuthUrl(mGetAuthUrlCallback);

        getActionBar().setTitle(R.string.authorize_twitter_account);
    }

    /*
     *
	 */ private final GetAuthUrlCallback mGetAuthUrlCallback = TwitterManager.get().getSignInInstance().new GetAuthUrlCallback() {

        @Override
        public void finished(boolean successful, String url, RequestToken requestToken) {
            getAuthUrlCallback(url, requestToken);

        }
    };

    /*
     *
	 */ private final GetOAuthAccessTokenCallback mGetOAuthAccessTokenCallback =
            TwitterManager.get().getSignInInstance().new GetOAuthAccessTokenCallback() {

                @Override
                public void finished(boolean successful, TwitterUser user, String accessToken,
                                     String accessTokenSecret) {
                    if (successful) {
                        onSuccessfulLogin(user, accessToken, accessTokenSecret);
                    }
                }
            };

    /*
     *
	 */
    void onSuccessfulLogin(TwitterUser user, String accessToken, String accessTokenSecret) {
        getApp().onPostSignIn(user, accessToken, accessTokenSecret, SocialNetConstant.Type.Twitter);
        getApp().restartApp(this);
    }

    /*
     *
	 */
    App getApp() {
        return (App) getApplication();
    }

    /*
     *
	 */
    void getAuthUrlCallback(String url, RequestToken requestToken) {

        mRequestToken = requestToken;

        setContentView(R.layout.twitter_auth_signin);

        WebView webView = (WebView) findViewById(R.id.twitter_auth_signin_webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // String scheme = getResources().getString(
                // R.string.twitter_callback );
                String path = "tweetlanes-auth-callback";
                if (url.contains(path)) {
                    Uri uri = Uri.parse(url);
                    String oauthVerifier = uri.getQueryParameter("oauth_verifier");

                    TwitterManager.get().setSocialNetType(SocialNetConstant.Type.Twitter, ConsumerKeyConstants.TWITTER_CONSUMER_KEY,
                            ConsumerKeyConstants.TWITTER_CONSUMER_SECRET, null);

                    onOAuthVerifier(oauthVerifier);

                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView webView, String url) {
                // Auto scroll to the bottom to save the user having to do this
                // themselves.
                webView.pageDown(true);
                super.onPageFinished(webView, url);
            }
        });

        webView.loadUrl(url);

    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        int TWITTER_AUTH_REQUEST_CODE = 443343;
        if (requestCode == TWITTER_AUTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String oauthVerifier = (String) data.getExtras().get("oauth_verifier");
                TwitterManager.get().getOAuthAccessToken(mRequestToken, oauthVerifier, mGetOAuthAccessTokenCallback);
            }
        }
    }

    /*
	 *
	 */
    void onOAuthVerifier(String oauthVerifier) {
        setContentView(R.layout.loading);
        TwitterManager.get().getOAuthAccessToken(mRequestToken, oauthVerifier, mGetOAuthAccessTokenCallback);
    }
}

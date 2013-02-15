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

package com.tweetlanes.android.view;

import org.twitter4j.auth.RequestToken;

import com.crittercism.app.Crittercism;

import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterSignIn.GetOAuthAccessTokenCallback;
import org.tweetalib.android.model.TwitterUser;
import com.tweetlanes.android.App;
import com.tweetlanes.android.AppSettings;
import com.tweetlanes.android.Constant;
import com.tweetlanes.android.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AppDotNetAuthActivity extends Activity {

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (Constant.ENABLE_CRASH_TRACKING) {
			Crittercism.init(getApplicationContext(), Constant.CRITTERCISM_APP_ID);
		}
		
		setTheme(AppSettings.get().getCurrentThemeStyle());
		
		getActionBar().setTitle(R.string.authorize_twitter_account);
		
		String url = "https://alpha.app.net/oauth/authenticate?client_id=xQpysWPFrnqrhHFnNGgrbcR6utBFgzpk&response_type=token&redirect_uri=http://lacytest.appspot.com/twitter/oauth_callback&scope=stream,email,write_post,follow,messages";
		
		setContentView(R.layout.twitter_auth_signin);
	    
	    WebView webView = (WebView)findViewById(R.id.twitter_auth_signin_webview);
	    webView.setWebViewClient( new WebViewClient()
	    {
	        @Override
	        public boolean shouldOverrideUrlLoading(WebView view, String url)
	        {
	        	if (url.contains("#access_token")) {
	            	String accessToken = url.replace("http://lacytest.appspot.com/twitter/oauth_callback#access_token=", "");
	                
	                setContentView(R.layout.loading);
	        		TwitterManager.get().getOAuthAccessToken(null, accessToken, mGetOAuthAccessTokenCallback);
	            }
	            return false;
	        }

	    });
	    
	    webView.loadUrl(url);
	}
	
	/*
	 * 
	 */
	void onSuccessfulLogin(TwitterUser user, String accessToken, String accessTokenSecret) {
		App app = (App) getApplication();
		app.onPostSignIn(user, accessToken, accessTokenSecret);
		app.restartApp(this);
	}
	
	/*
	 * 
	 */
	GetOAuthAccessTokenCallback mGetOAuthAccessTokenCallback = TwitterManager.get().getSignInInstance().new GetOAuthAccessTokenCallback() {
		
		@Override
		public void finished(boolean successful, TwitterUser user, String accessToken, String accessTokenSecret) {
			if (successful) {
				onSuccessfulLogin(user, accessToken, accessTokenSecret);
			}
		}
	};
}

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

package org.socialnetlib.android;

import java.util.List;

import org.tweetalib.android.model.TwitterUser;
import org.twitter4j.Twitter;
import org.twitter4j.TwitterException;
import org.twitter4j.TwitterFactory;
import org.twitter4j.auth.OAuthAuthorization;
import org.twitter4j.conf.Configuration;
import org.twitter4j.conf.ConfigurationBuilder;
import org.twitter4j.internal.http.HttpParameter;
import org.twitter4j.media.MediaProvider;

public class TwitterApi extends SocialNetApi {

	public static final String TWITTER_VERIFY_CREDENTIALS_JSON = "https://api.twitter.com/1/account/verify_credentials.json";
	
	private Twitter mSocNetApi;
	private OAuthAuthorization mOAuth;
	
	
	public TwitterApi(SocialNetConstant.Type type, String consumerKey, String consumerSecret) {
		super(type, consumerKey, consumerSecret);
	}	

	@Override
	/*
	 * Warning: SYNCHRONOUS call. This exists just so we can get the screename after a sign in
	 */
	public TwitterUser verifyCredentialsSync(String token, String secret) {
		
		TwitterUser twitterUser = null;
		
		clearApiInstance();
		
		try {
			setOAuthTokenWithSecret(token, secret, true);
			Twitter twitter = getAndConfigureApiInstance();
			twitterUser = new TwitterUser(twitter.verifyCredentials());
			
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return twitterUser;
	}

	@Override
	public void init() {
		mSocNetApi = null;
		mOAuth = null;
	}

	Twitter getAndConfigureApiInstance() {
		
		if (mCurrentOAuthToken == null || mCurrentOAuthSecret == null) {
			mSocNetApi = null;
			mOAuth = null;
		} else if (mSocNetApi == null) {

			ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
			configurationBuilder.setDebugEnabled(true)
			  					.setOAuthConsumerKey(mAppConsumerKey)
			  					.setOAuthConsumerSecret(mAppConsumerSecret)
			  					.setOAuthAccessToken(mCurrentOAuthToken)
			  					.setOAuthAccessTokenSecret(mCurrentOAuthSecret)
			  					.setMediaProvider(MediaProvider.TWITTER.getName())
								//.setJSONStoreEnabled(true)
			  					.setIncludeEntitiesEnabled(true);
			
			Configuration configuration = configurationBuilder.build();
			mSocNetApi = new TwitterFactory(configuration).getInstance();
			mOAuth = new TwitterFactory(configuration).getOAuthAuthorization();
		}
		return mSocNetApi;
	}

	@Override
	void clearApiInstance() {
		mSocNetApi = null;
		mOAuth = null;
	}

	/*
	 * Used for https://dev.twitter.com/docs/auth/oauth/oauth-echo
	 */
	public String generateTwitterVerifyCredentialsAuthorizationHeader() {
		String verifyCredentialsUrl = TWITTER_VERIFY_CREDENTIALS_JSON;
        List<HttpParameter> oauthSignatureParams = mOAuth.generateOAuthSignatureHttpParams("GET", verifyCredentialsUrl);
        return "OAuth realm=\"http://api.twitter.com/\"," + OAuthAuthorization.encodeParameters(oauthSignatureParams, ",", true);
    }
}

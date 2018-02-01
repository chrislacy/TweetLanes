/*
 * Copyright (C) 2013 Chris Lacy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.tweetalib.android;

import org.asynctasktex.AsyncTaskEx;
import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.model.TwitterUser;

import java.util.HashMap;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class TwitterSignIn {

    private SignInWorkerCallbacks mCallbacks;
    private Integer mGetAuthUrlCallbackHandle;
    private final HashMap<Integer, GetAuthUrlCallback> mGetAuthUrlCallbackMap;
    private Integer mGetOAuthAccessTokenCallbackHandle;
    private final HashMap<Integer, GetOAuthAccessTokenCallback> mGetOAuthAccessTokenCallbackMap;


    /*
     *
	 */
    public interface SignInWorkerCallbacks {

        public SocialNetConstant.Type getType();

        public String getConsumerKey();

        public String getConsumerSecret();

        public TwitterUser verifyCredentials(String accessToken,
                                             String accessTokenSecret);
    }

    /*
     *
	 */
    public void setWorkerCallbacks(SignInWorkerCallbacks callbacks) {

        mCallbacks = callbacks;
    }

    /*
	 *
	 */
    public interface GetAuthUrlCallbackInterface {

        public void finished(boolean successful, String url,
                             RequestToken requestToken);

    }

    /*
	 *
	 */
    public abstract class GetAuthUrlCallback implements
            GetAuthUrlCallbackInterface {

        static final int kInvalidHandle = -1;

        public GetAuthUrlCallback() {
            mHandle = kInvalidHandle;
        }

        private int mHandle;
    }

    /*
	 *
	 */
    public interface GetOAuthAccessTokenCallbackInterface {

        public void finished(boolean successful, TwitterUser user,
                             String accessToken, String accessTokenSecret);

    }

    /*
	 *
	 */
    public abstract class GetOAuthAccessTokenCallback implements
            GetOAuthAccessTokenCallbackInterface {

        static final int kInvalidHandle = -1;

        public GetOAuthAccessTokenCallback() {
            mHandle = kInvalidHandle;
        }

        void setHandle(int handle) {
            mHandle = handle;
        }

        private int mHandle;
    }

    /*
	 *
	 */
    public TwitterSignIn() {
        mGetAuthUrlCallbackMap = new HashMap<Integer, GetAuthUrlCallback>();
        mGetAuthUrlCallbackHandle = 0;

        mGetOAuthAccessTokenCallbackHandle = 0;
        mGetOAuthAccessTokenCallbackMap = new HashMap<Integer, GetOAuthAccessTokenCallback>();
    }

    /*
	 *
	 */
    GetAuthUrlCallback getAuthUrlCallback(Integer callbackHandle) {
        return mGetAuthUrlCallbackMap
                .get(callbackHandle);
    }

    /*
	 *
	 */
    void removeAuthUrlCallback(GetAuthUrlCallback callback) {
        if (mGetAuthUrlCallbackMap.containsValue(callback)) {
            mGetAuthUrlCallbackMap.remove(callback.mHandle);
        }
    }

    /*
	 *
	 */
    GetOAuthAccessTokenCallback getOAuthAccessTokenCallback(
            Integer callbackHandle) {
        return mGetOAuthAccessTokenCallbackMap
                .get(callbackHandle);
    }

    /*
	 *
	 */
    void removeGetOAuthAccessTokenCallback(GetOAuthAccessTokenCallback callback) {
        if (mGetOAuthAccessTokenCallbackMap.containsValue(callback)) {
            mGetOAuthAccessTokenCallbackMap.remove(callback.mHandle);
        }
    }

    /*
	 *
	 */
    public void getAuthUrl(GetAuthUrlCallback callback) {

        assert (!mGetAuthUrlCallbackMap.containsValue(callback));

        mGetAuthUrlCallbackMap.put(mGetAuthUrlCallbackHandle, callback);
        new FetchAuthUrlTask().execute(AsyncTaskEx.PRIORITY_HIGHEST,
                "Get Auth URL", new FetchAuthUrlTaskInput(
                mGetAuthUrlCallbackHandle));

        mGetAuthUrlCallbackHandle += 1;
    }

    /*
	 *
	 */
    public void getOAuthAccessToken(RequestToken requestToken,
                                    String oauthVerifier, GetOAuthAccessTokenCallback callback) {

        assert (!mGetOAuthAccessTokenCallbackMap.containsValue(callback));

        mGetOAuthAccessTokenCallbackMap.put(mGetOAuthAccessTokenCallbackHandle,
                callback);
        new FetchOAuthAccessTokenTask().execute(AsyncTaskEx.PRIORITY_HIGHEST,
                "Get OAuth AccessToken", new FetchOAuthAccessTokenTaskInput(
                requestToken, oauthVerifier,
                mGetOAuthAccessTokenCallbackHandle));

        mGetOAuthAccessTokenCallbackHandle += 1;
    }

    /*
	 *
	 */
    class FetchAuthUrlTaskInput {

        FetchAuthUrlTaskInput(Integer callbackHandle) {
            mCallbackHandle = callbackHandle;
        }

        final Integer mCallbackHandle;
    }

    /*
	 *
	 */
    class FetchAuthUrlTaskOutput {

        FetchAuthUrlTaskOutput(Integer callbackHandle,
                               RequestToken requestToken, String url) {
            mCallbackHandle = callbackHandle;
            mRequestToken = requestToken;
            mUrl = url;
        }

        final Integer mCallbackHandle;
        final RequestToken mRequestToken;
        final String mUrl;
    }

    /*
	 *
	 */
    class FetchAuthUrlTask extends
            AsyncTaskEx<FetchAuthUrlTaskInput, Void, FetchAuthUrlTaskOutput> {

        @Override
        protected FetchAuthUrlTaskOutput doInBackground(
                FetchAuthUrlTaskInput... inputArray) {

            FetchAuthUrlTaskInput input = inputArray[0];

            String url = null;
            Twitter twitter = new TwitterFactory().getInstance();
            RequestToken requestToken = null;
            twitter.setOAuthConsumer(mCallbacks.getConsumerKey(),
                    mCallbacks.getConsumerSecret());
            // String callbackURL =
            // App.getContext().getString(R.string.twitter_callback);
            String callbackUrl = "tweetlanes-auth-callback:///";
            try {
                requestToken = twitter.getOAuthRequestToken(callbackUrl);
                url = requestToken.getAuthorizationURL();
            } catch (TwitterException e) {
                e.printStackTrace();
            }

            return new FetchAuthUrlTaskOutput(input.mCallbackHandle,
                    requestToken, url);
        }

        @Override
        protected void onPostExecute(FetchAuthUrlTaskOutput output) {

            GetAuthUrlCallback callback = getAuthUrlCallback(output.mCallbackHandle);
            if (callback != null) {
                callback.finished(true, output.mUrl, output.mRequestToken);
                removeAuthUrlCallback(callback);
            }

            super.onPostExecute(output);
        }
    }

    /*
	 *
	 */
    class FetchOAuthAccessTokenTaskInput {

        FetchOAuthAccessTokenTaskInput(RequestToken requestToken,
                                       String oauthVerifier, Integer callbackHandle) {

            mRequestToken = requestToken;
            mOAuthVerifier = oauthVerifier;
            mCallbackHandle = callbackHandle;
        }

        final RequestToken mRequestToken;
        final String mOAuthVerifier;
        final Integer mCallbackHandle;
    }

    /*
	 *
	 */
    class FetchOAuthAccessTokenTaskOutput {

        FetchOAuthAccessTokenTaskOutput(TwitterUser user, String accessToken,
                                        String accessTokenSecret, Integer callbackHandle) {

            mUser = user;
            mAccessToken = accessToken;
            mAccessTokenSecret = accessTokenSecret;
            mCallbackHandle = callbackHandle;
        }

        final TwitterUser mUser;
        final String mAccessToken;
        final String mAccessTokenSecret;
        final Integer mCallbackHandle;
    }

    /*
	 *
	 */
    class FetchOAuthAccessTokenTask
            extends
            AsyncTaskEx<FetchOAuthAccessTokenTaskInput, Void, FetchOAuthAccessTokenTaskOutput> {

        @Override
        protected FetchOAuthAccessTokenTaskOutput doInBackground(
                FetchOAuthAccessTokenTaskInput... inputArray) {

            FetchOAuthAccessTokenTaskInput input = inputArray[0];

            try {
                String accessToken = null;
                String accessTokenSecret = null;

                switch (mCallbacks.getType()) {
                    case Twitter:
                        Twitter twitter = new TwitterFactory().getInstance();
                        twitter.setOAuthConsumer(mCallbacks.getConsumerKey(),
                                mCallbacks.getConsumerSecret());
                        AccessToken at = twitter.getOAuthAccessToken(
                                input.mRequestToken, input.mOAuthVerifier);
                        accessToken = at.getToken();
                        accessTokenSecret = at.getTokenSecret();
                        break;

                    case Appdotnet:
                        accessToken = input.mOAuthVerifier;
                        break;
                }

                TwitterUser user = mCallbacks.verifyCredentials(accessToken,
                        accessTokenSecret);

                return new FetchOAuthAccessTokenTaskOutput(user, accessToken,
                        accessTokenSecret, input.mCallbackHandle);
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (TwitterException e) {
                e.printStackTrace();
            }

            return new FetchOAuthAccessTokenTaskOutput(null, null, null,
                    input.mCallbackHandle);
        }

        @Override
        protected void onPostExecute(FetchOAuthAccessTokenTaskOutput output) {

            GetOAuthAccessTokenCallback callback = getOAuthAccessTokenCallback(output.mCallbackHandle);
            if (callback != null) {
                if (output.mUser != null && output.mAccessToken != null) {
                    callback.finished(true, output.mUser, output.mAccessToken,
                            output.mAccessTokenSecret);
                } else {
                    callback.finished(false, null, null, null);
                }
                removeGetOAuthAccessTokenCallback(callback);
            }

            super.onPostExecute(output);
        }
    }

}

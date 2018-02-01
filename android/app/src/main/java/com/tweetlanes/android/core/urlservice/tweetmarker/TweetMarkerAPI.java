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

package com.tweetlanes.android.core.urlservice.tweetmarker;

import android.net.Uri;

import com.tweetlanes.android.core.model.AccountDescriptor;
import com.tweetlanes.android.core.urlservice.ApiService;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.asynctasktex.AsyncTaskEx;
import org.socialnetlib.android.TwitterApi;
import org.tweetalib.android.ConnectionStatus;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

class TweetMarkerAPI extends ApiService {

    private static final String BASE_URL = "http://api.tweetmarker.net/v2/";

    private static final String API_LAST_READ = "lastread";

    public static HttpResponse getRequest(String url, String debugName) {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet();
        request.addHeader("X-Auth-Service-Provider",
                TwitterApi.TWITTER_VERIFY_CREDENTIALS_JSON);
        request.addHeader("X-Verify-Credentials-Authorization", TwitterManager
                .get().generateTwitterVerifyCredentialsAuthorizationHeader());
        HttpResponse response = null;
        try {
            request.setURI(new URI(url));
            // Log.d("tweetlanes url fetch", url);
            response = client.execute(request);
            // Log.d(TAG, debugName + " complete");
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return response;
    }

    /*
     *
	 */
    public interface APICallback {

        public void finished(TwitterFetchResult fetchResult, String response);
    }

    /*
     *
	 */
    public static void getLastRead(AccountDescriptor account,
                                   final ConnectionStatus.Callbacks connectionStatus,
                                   final APICallback callback) {

        class TaskOutput {

            TaskOutput(TwitterFetchResult fetchResult, String response) {
                mFetchResult = fetchResult;
                mResponse = response;
            }

            TwitterFetchResult mFetchResult;
            String mResponse;
        }

        /*
         *
		 */
        AsyncTaskEx<AccountDescriptor, Void, TaskOutput> worker = new AsyncTaskEx<AccountDescriptor, Void, TaskOutput>() {

            @Override
            protected TaskOutput doInBackground(AccountDescriptor... inputArray) {

                String screenName = "chrismlacy";// inputArray[0];
                String errorDescription = null;

                if (connectionStatus != null && !connectionStatus.isOnline()) {
                    return new TaskOutput(new TwitterFetchResult(false,
                            connectionStatus.getErrorMessageNoConnection()),
                            null);
                }

                String url = String.format(BASE_URL + API_LAST_READ
                        + "?api_key=%s&username=%s&collection=timeline",
                        Uri.encode("TW-2C4324C62DF4"), Uri.encode(screenName));
                HttpResponse response = getRequest(url, "freeForLife");
                String jsonAsString = null;
                try {
                    if (response != null) {
                        jsonAsString = EntityUtils.toString(response
                                .getEntity());
                        // JSONObject jsonObject = new JSONObject(jsonAsString);
                        // success = jsonObject.getBoolean("success");
                    }
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                return new TaskOutput(new TwitterFetchResult(
                        errorDescription == null,
                        errorDescription), jsonAsString);
            }

            @Override
            protected void onPostExecute(TaskOutput output) {

                if (callback != null) {
                    callback.finished(output.mFetchResult, output.mResponse);
                }

                super.onPostExecute(output);
            }
        };

        worker.execute(AsyncTaskEx.PRIORITY_HIGH, "Get TwMrkr Last Read",
                account);
    }

}

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

package com.tweetlanes.android.core.urlservice;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public abstract class ApiService {

    // NOTE: Look at this:
    // http://turbomanage.wordpress.com/2012/06/12/a-basic-http-client-for-android-and-more/

    /*
     *
	 */
    public static HttpResponse getRequest(String url, String debugName) {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet();
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
    public static HttpResponse postRequest(String url, String debugName) {
        HttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost();
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

}

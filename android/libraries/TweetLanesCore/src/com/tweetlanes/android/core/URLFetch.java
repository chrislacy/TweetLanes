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

package com.tweetlanes.android.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class URLFetch {

    /*
     *
	 */
    public interface FetchBitmapCallback {

        public void finished(boolean successful, Bitmap bitmap);

    }

    /*
	 * 
	 */
    public static void fetchBitmap(final String urlAsString,
                                   final FetchBitmapCallback callback) {

        Thread t = new Thread() {

            public void run() {
                URL url;
                Bitmap bitmap = null;
                try {
                    url = new URL(urlAsString);
                    // Log.d("tweetlanes url fetch", urlAsString);
                    URLConnection connection = url.openConnection();
                    connection.setUseCaches(true);
                    Object response = connection.getContent();
                    if (response instanceof Bitmap) {
                        bitmap = (Bitmap) response;
                    } else {
                        InputStream inputStream = connection.getInputStream();
                        bitmap = BitmapFactory.decodeStream(inputStream);
                    }
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if (callback != null) {
                    callback.finished(bitmap != null, bitmap);
                }
            }
        };
        t.start();
    }

    /*
	 * 
	 */
    public String inputStreamToString(InputStream inputStream) {
        final char[] buffer = new char[0x10000];
        StringBuilder out = new StringBuilder();
        Reader in;
        try {
            in = new InputStreamReader(inputStream, "UTF-8");
            int read;
            do {
                read = in.read(buffer, 0, buffer.length);
                if (read > 0) {
                    out.append(buffer, 0, read);
                }
            } while (read >= 0);

            return out.toString();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /*
	 * 
	 */
    public static InputStream retrieveStream(String url) {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(url);
        try {
            HttpResponse getResponse = client.execute(getRequest);
            final int statusCode = getResponse.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                Log.w("abtApp", "Error " + statusCode + " for URL " + url);
                return null;
            }

            HttpEntity getResponseEntity = getResponse.getEntity();
            return getResponseEntity.getContent();
        } catch (IOException e) {
            getRequest.abort();
            Log.w("abtApp", "Error for URL " + url, e);
        }
        return null;
    }
}

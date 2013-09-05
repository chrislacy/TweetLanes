
package com.tweetlanes.android.core.widget.urlimageviewhelper;

import android.content.Context;
import android.os.AsyncTask;

import com.tweetlanes.android.core.util.Util;
import com.tweetlanes.android.core.widget.urlimageviewhelper.UrlImageViewHelper.RequestPropertiesCallback;

import org.apache.http.NameValuePair;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class HttpUrlDownloader implements UrlDownloader {
    private RequestPropertiesCallback mRequestPropertiesCallback;

    @Override
    public void download(final Context context, final String url, final String filename, final UrlDownloaderCallback callback, final Runnable completion) {
        final AsyncTask<Void, Void, Void> downloader = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                HttpURLConnection urlConnection = null;
                InputStream is = null;
                try {
                    String thisUrl = url;
                    while (true) {
                        final URL u = new URL(thisUrl);
                        urlConnection = (HttpURLConnection) u.openConnection();
                        urlConnection.setInstanceFollowRedirects(true);

                        if (mRequestPropertiesCallback != null) {
                            final ArrayList<NameValuePair> props = mRequestPropertiesCallback.getHeadersForRequest(context, url);
                            if (props != null) {
                                for (final NameValuePair pair : props) {
                                    urlConnection.addRequestProperty(pair.getName(), pair.getValue());
                                }
                            }
                        }

                        if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_MOVED_TEMP && urlConnection.getResponseCode() != HttpURLConnection.HTTP_MOVED_PERM)
                            break;
                        thisUrl = urlConnection.getHeaderField("Location");
                    }

                    if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        UrlImageViewHelper.clog("Response Code: " + urlConnection.getResponseCode());
                        return null;
                    }
                    is = urlConnection.getInputStream();
                    callback.onDownloadComplete(HttpUrlDownloader.this, is, null);
                    return null;
                } catch (final Throwable e) {
                    e.printStackTrace();
                    return null;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                        urlConnection = null;
                    }
                    Util.closeQuietly(is);
                }
            }

            @Override
            protected void onPostExecute(final Void result) {
                completion.run();
            }
        };

        UrlImageViewHelper.executeTask(downloader);
    }

    @Override
    public boolean doNotCache() {
        return false;
    }

    @Override
    public boolean canDownloadUrl(String url) {
        return url.startsWith("http");
    }
}

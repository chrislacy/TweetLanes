package com.tweetlanes.android.core.widget.urlimageviewhelper;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.InputStream;

public class ContentUrlDownloader implements UrlDownloader {
    @Override
    public void download(final Context context, final String url, final String filename, final UrlDownloaderCallback callback, final Runnable completion) {
        final AsyncTask<Void, Void, Void> downloader = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                try {
                    final ContentResolver cr = context.getContentResolver();
                    InputStream is = cr.openInputStream(Uri.parse(url));
                    callback.onDownloadComplete(ContentUrlDownloader.this, is, null);
                    return null;
                } catch (final Throwable e) {
                    e.printStackTrace();
                    return null;
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
        return true;
    }

    @Override
    public boolean canDownloadUrl(String url) {
        return url.startsWith(ContentResolver.SCHEME_CONTENT);
    }
}

package com.tweetlanes.android.widget.urlimageviewhelper;

import android.graphics.drawable.Drawable;

public final class URLImageCache extends
        SoftReferenceHashTable<String, Drawable> {

    private static URLImageCache mInstance = new URLImageCache();

    public static URLImageCache getInstance() {
        return mInstance;
    }

    private URLImageCache() {
    }
}

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

package com.tweetlanes.android.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Environment;
import android.util.DisplayMetrics;

public final class Util {

    /*
     *
	 */
    public static boolean isValidString(String s) {
        return s != null && s.equals("") == false;
    }

    /*
	 * 
	 */
    public static String getFullDate(Date date) {
        SimpleDateFormat formatted = new SimpleDateFormat(
                "hh:mm aa - dd MMM yy");
        return formatted.format(date);
    }

    /*
     * Given 1234567890, return "1,234,567,890"
     */
    public static String getPrettyCount(int count) {
        String regex = "(\\d)(?=(\\d{3})+$)";
        return Integer.toString(count).replaceAll(regex, "$1,");
    }

    /*
	 * 
	 */
    public static String getPrettyDate(Date createdAt) {

        return getPrettyDate(createdAt, new Date());
    }

    /*
	 * 
	 */
    public static String getPrettyDate(Date olderDate, Date newerDate) {

        String result;

        int diffInDays = (int) ((newerDate.getTime() - olderDate.getTime()) / (1000 * 60 * 60 * 24));
        if (diffInDays > 365) {
            SimpleDateFormat formatted = new SimpleDateFormat("dd MMM yy");
            result = formatted.format(olderDate);
        } else if (diffInDays > 0) {
            if (diffInDays == 1) {
                result = "1d";
            } else if (diffInDays < 8) {
                result = diffInDays + "d";
            } else {
                SimpleDateFormat formatted = new SimpleDateFormat("dd MMM");
                result = formatted.format(olderDate);
            }
        } else {
            int diffInHours = (int) ((newerDate.getTime() - olderDate.getTime()) / (1000 * 60 * 60));
            if (diffInHours > 0) {
                if (diffInHours == 1) {
                    result = "1h";
                } else {
                    result = diffInHours + "h";
                }
            } else {
                int diffInMinutes = (int) ((newerDate.getTime() - olderDate
                        .getTime()) / (1000 * 60));
                if (diffInMinutes > 0) {
                    if (diffInMinutes == 1) {
                        result = "1m";
                    } else {
                        result = diffInMinutes + "m";
                    }
                } else {
                    int diffInSeconds = (int) ((newerDate.getTime() - olderDate
                            .getTime()) / (1000));
                    if (diffInSeconds < 5) {
                        result = "now";
                    } else {
                        result = diffInSeconds + "s";
                    }
                }
            }
        }

        return result;
    }

    // http://stackoverflow.com/a/9563438/328679

    /**
     * This method convets dp unit to equivalent device specific value in
     * pixels.
     *
     * @param dp      A value in dp(Device independent pixels) unit. Which we need
     *                to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent Pixels equivalent to dp according to
     *         device
     */
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    /**
     * This method converts device specific pixels to device independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent db equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

    /*
	 * 
	 */
    public static String trimLeft(String s) {
        return s.replaceAll("^\\s+", "");
    }

    /*
	 * 
	 */
    public static String trimRight(String s) {
        return s.replaceAll("\\s+$", "");
    }

    /*
	 * 
	 */
    public static String trimLeftRight(String s) {
        return trimRight(trimLeft(s));
    }

    /*
	 * 
	 */
    public static boolean tagEquals(Object tag, int comparisonId) {

        if (tag != null) {
            String tagAsString = tag.toString();
            if (tagAsString != null && tagAsString.equals("") == false) {
                try {
                    int tagAsInt = Integer.parseInt(tagAsString);
                    if (tagAsInt == comparisonId) {
                        return true;
                    }
                } catch (NumberFormatException e) {

                }
            }
        }
        return false;
    }

    /*
	 * 
	 */
    public static ArrayList<String> getUrlsInString(String string) {

        ArrayList<String> result = new ArrayList<String>();

        String[] parts = string.split("\\s");

        // Attempt to convert each item into an URL.
        for (String item : parts)
            try {
                URL url = new URL(item);
                String u = url.toString();
                result.add(u);

            } catch (MalformedURLException e) {
                // If there was an URL that was not it!...
            }

        if (result.size() > 0) {
            return result;
        }
        return null;
    }

    /*
	 * 
	 */
    public static URL parseURL(String url_string) {
        if (url_string == null) return null;
        try {
            return new URL(url_string);
        } catch (final MalformedURLException e) {
            // This should not happen.
        }
        return null;
    }

    /*
	 * 
	 */
    public static Proxy getProxy(Context context) {
        if (context == null) return null;
        /*
         * final SharedPreferences prefs =
         * context.getSharedPreferences(SHARED_PREFERENCES_NAME,
         * Context.MODE_PRIVATE); final boolean enable_proxy =
         * prefs.getBoolean(PREFERENCE_KEY_ENABLE_PROXY, false); if
         * (!enable_proxy) return Proxy.NO_PROXY; final String proxy_host =
         * prefs.getString(PREFERENCE_KEY_PROXY_HOST, null); final int
         * proxy_port = parseInt(prefs.getString(PREFERENCE_KEY_PROXY_PORT,
         * "-1")); if (!isNullOrEmpty(proxy_host) && proxy_port > 0) { final
         * SocketAddress addr = InetSocketAddress.createUnresolved(proxy_host,
         * proxy_port); return new Proxy(Proxy.Type.HTTP, addr); }
         */
        return Proxy.NO_PROXY;
    }

    public static void setIgnoreSSLError(URLConnection conn) {
        /*
         * if (conn instanceof HttpsURLConnection) { ((HttpsURLConnection)
         * conn).setHostnameVerifier(ALLOW_ALL_HOSTNAME_VERIFIER); if
         * (IGNORE_ERROR_SSL_FACTORY != null) { ((HttpsURLConnection)
         * conn).setSSLSocketFactory(IGNORE_ERROR_SSL_FACTORY); } }
         */
    }

    /*
     * via https://developer.android.com/training/camera/photobasics.html#
     * TaskCaptureIntent
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private static File getAlbumStorageDir(String albumName) {
        // TODO Auto-generated method stub
        return new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                albumName);
    }

    /*
	 * 
	 */
    public static File getAlbumDir(String albumName) {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {

            storageDir = getAlbumStorageDir(albumName);

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        // Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            // Log.v(getString(R.string.app_name),
            // "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    /*
	 * 
	 */
    public static void copyFile(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}

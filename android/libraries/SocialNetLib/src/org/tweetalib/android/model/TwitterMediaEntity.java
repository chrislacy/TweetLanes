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

package org.tweetalib.android.model;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.DirectMessage;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;

public class TwitterMediaEntity {
    // https://dev.twitter.com/docs/tweet-entities

    /*
     *
	 */
    public enum Source {
        TWITTER, INSTAGRAM, TWITPIC, LOCKERZ, // Note that Plixi and Lockerz use
        // the same API, so
        // Plixi is treated as Lockerz
        YFROG, IMGUR, YOUTUBE,
    }

    /*
     *
	 */
    public enum Size {
        THUMB, SMALL, MEDIUM, LARGE,
    }

    /*
	 * 
	 */
    public static TwitterMediaEntity createFromString(String jsonString) {
        try {
            return new TwitterMediaEntity(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
	 * 
	 */
    public static TwitterMediaEntity createMediaEntity(Status status) {
        MediaEntity[] mediaEntities;
        URLEntity[] urlEntities;

        if (status.isRetweet()) {
            mediaEntities = status.getRetweetedStatus().getMediaEntities();
            urlEntities = status.getRetweetedStatus().getURLEntities();
        } else {
            mediaEntities = status.getMediaEntities();
            urlEntities = status.getURLEntities();
        }


        if (mediaEntities != null && mediaEntities.length > 0) {
            return new TwitterMediaEntity(mediaEntities[0]);
        } else if (urlEntities != null) {
            for (URLEntity urlEntity : urlEntities) {
                // This shouldn't be necessary, but is
                String expandedUrl = urlEntity.getExpandedURL();
                if (expandedUrl == null) {
                    continue;
                }

                TwitterMediaEntity entity = getTwitterMediaEntityFromUrl(
                        urlEntity.getURL(), urlEntity.getExpandedURL());
                if (entity != null) {
                    return entity;
                }
            }
        }

        return null;
    }

    public static TwitterMediaEntity createMediaEntity(DirectMessage status) {
        MediaEntity[] mediaEntities;
        URLEntity[] urlEntities;

        mediaEntities = status.getMediaEntities();
        urlEntities = status.getURLEntities();

        if (mediaEntities != null && mediaEntities.length > 0) {
            return new TwitterMediaEntity(mediaEntities[0]);
        } else if (urlEntities != null) {
            for (URLEntity urlEntity : urlEntities) {
                // This shouldn't be necessary, but is
                String expandedUrl = urlEntity.getExpandedURL();
                if (expandedUrl == null) {
                    continue;
                }

                TwitterMediaEntity entity = getTwitterMediaEntityFromUrl(
                        urlEntity.getURL(), urlEntity.getExpandedURL());
                if (entity != null) {
                    return entity;
                }
            }
        }

        return null;
    }


    private static TwitterMediaEntity getTwitterMediaEntityFromUrl(
            String tinyUrl, String expandedUrl) {

        String mediaUrl = getInstagramMediaUrl(expandedUrl);
        if (mediaUrl != null) {
            return new TwitterMediaEntity(Source.INSTAGRAM, mediaUrl, tinyUrl,
                    expandedUrl);
        }

        mediaUrl = getTwitpicMediaUrl(expandedUrl);
        if (mediaUrl != null) {
            return new TwitterMediaEntity(Source.TWITPIC, mediaUrl, tinyUrl,
                    expandedUrl);
        }

        mediaUrl = getLockerzMediaUrl(expandedUrl);
        if (mediaUrl != null) {
            return new TwitterMediaEntity(Source.LOCKERZ, mediaUrl, tinyUrl,
                    expandedUrl);
        }

        mediaUrl = getYfrogMediaUrl(expandedUrl);
        if (mediaUrl != null) {
            return new TwitterMediaEntity(Source.YFROG, mediaUrl, tinyUrl,
                    expandedUrl);
        }

        mediaUrl = getImgurUrl(expandedUrl);
        if (mediaUrl != null) {
            return new TwitterMediaEntity(Source.IMGUR, mediaUrl, tinyUrl,
                    expandedUrl);
        }

        mediaUrl = getYouTubeUrl(expandedUrl);
        if (mediaUrl != null) {
            return new TwitterMediaEntity(Source.YOUTUBE, mediaUrl, tinyUrl,
                    expandedUrl);
        }

        return null;
    }

    /*
	 * 
	 */
    private static String getInstagramMediaUrl(String url) {

        String lowerCaseUrl = url.toLowerCase();
        if (lowerCaseUrl.indexOf("://instagr.am/p/") > -1) {
            String instagramMatch = "://instagr.am/p/";
            int startIndex = lowerCaseUrl.indexOf(instagramMatch);
            if (startIndex > -1) {
                startIndex += instagramMatch.length();
                int endIndex = lowerCaseUrl.indexOf("/", startIndex);
                if (endIndex > -1) {
                    String code = url.substring(startIndex, endIndex);
                    return "http://instagr.am/p/" + code + "/media/";
                }
            }
        }
        if (lowerCaseUrl.indexOf("://instagram.com/p/") > -1) {
            String instagramMatch = "://instagram.com/p/";
            int startIndex = lowerCaseUrl.indexOf(instagramMatch);
            if (startIndex > -1) {
                startIndex += instagramMatch.length();
                int endIndex = lowerCaseUrl.indexOf("/", startIndex);
                if (endIndex > -1) {
                    String code = url.substring(startIndex, endIndex);
                    return "http://instagram.com/p/" + code + "/media/";
                }
            }
        }

        return null;
    }

    /*
	 * 
	 */
    private static String getTwitpicMediaUrl(String url) {
        String lowerCaseUrl = url.toLowerCase();
        String match = "://twitpic.com/";
        int startIndex = lowerCaseUrl.indexOf(match);
        if (startIndex > -1) {
            startIndex += match.length();
            return url.substring(startIndex);
        }

        return null;
    }

    /*
	 * 
	 */
    private static String getLockerzMediaUrl(String url) {

        String lowerCaseUrl = url.toLowerCase();
        if (lowerCaseUrl.indexOf("://lockerz.com/") > -1) {
            return "http://api.plixi.com/api/tpapi.svc/imagefromurl?url=" + url;
        }

        if (lowerCaseUrl.indexOf("://plixi.com/") > -1) {
            return "http://api.plixi.com/api/tpapi.svc/imagefromurl?url=" + url;
        }

        return null;
    }

    /*
	 * 
	 */
    private static String getImgurUrl(String url) {

        String lowerCaseUrl = url.toLowerCase();
        String match = "imgur.com/";
        int startIndex = lowerCaseUrl.indexOf(match);
        if (startIndex > -1) {
            String code = url.substring(startIndex + match.length());
            return code.replace(".png", "").replace(".gif", "")
                    .replace(".jpg", "");
        }

        return null;
    }

    /*
	 * 
	 */
    private static String getYfrogMediaUrl(String url) {

        String lowerCaseUrl = url.toLowerCase();
        if (lowerCaseUrl.indexOf("://yfrog.com/") > -1) {
            return url;
        }

        return null;
    }

    /*
	 * 
	 */
    private static String getYouTubeUrl(String url) {

        String lowerCaseUrl = url.toLowerCase();
        if (lowerCaseUrl.indexOf("youtube.com/watch?") > -1) {
            Uri uri = Uri.parse(url);
            String videoId = uri.getQueryParameter("v");
            if (videoId != null) {
                return videoId;
            }
        }

        String prefix = "youtu.be/";
        int startIndex = lowerCaseUrl.indexOf(prefix);
        if (startIndex > -1) {
            startIndex += prefix.length();
            int endIndex = lowerCaseUrl.indexOf('?', startIndex);
            if (endIndex > -1) {
                return url.substring(startIndex, endIndex);
            } else {
                return url.substring(startIndex);
            }
        }

        return null;
    }

    /*
	 * 
	 */
    private TwitterMediaEntity(Source source, String mediaCode, String url,
                               String expandedUrl) {
        mSource = source;
        mMediaCode = mediaCode;
        mUrl = url;
        mExpandedUrl = expandedUrl;
    }

    /*
	 * 
	 */
    private TwitterMediaEntity(MediaEntity mediaEntity) {
        mSource = Source.TWITTER;
        mMediaCode = mediaEntity.getMediaURL();
        mUrl = mediaEntity.getURL();
        mExpandedUrl = mediaEntity.getExpandedURL();

        /*
         * mSizeThumb = new
         * SizeInfo(mediaEntity.getSizes().get(MediaEntity.Size.THUMB));
         * mSizeSmall = new
         * SizeInfo(mediaEntity.getSizes().get(MediaEntity.Size.SMALL));
         * mSizeMedium = new
         * SizeInfo(mediaEntity.getSizes().get(MediaEntity.Size.MEDIUM));
         * mSizeLarge = new
         * SizeInfo(mediaEntity.getSizes().get(MediaEntity.Size.LARGE));
         */
    }

    /*
	 * 
	 */
    private TwitterMediaEntity(String jsonAsString) throws JSONException {
        JSONObject object = new JSONObject(jsonAsString);
        mSource = Source.valueOf(object.getString(KEY_SOURCE));
        mMediaCode = object.getString(KEY_MEDIA_CODE);
        mUrl = object.getString(KEY_URL);
        mExpandedUrl = object.getString(KEY_EXPANDED_URL);
    }

    public TwitterMediaEntity(TwitterMediaEntity other) {

        mSource = other.mSource;
        mMediaCode = other.mMediaCode;
        mUrl = other.mUrl;
        mExpandedUrl = other.mExpandedUrl;
    }

    /*
	 * 
	 */
    public String toString() {

        JSONObject object = new JSONObject();
        try {
            object.put(KEY_SOURCE, mSource.toString());
            object.put(KEY_MEDIA_CODE, mMediaCode);
            object.put(KEY_URL, mUrl);
            object.put(KEY_EXPANDED_URL, mExpandedUrl);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    public String getExpandedUrl() {
        return mExpandedUrl;
    }

    public Source getSource() {
        return mSource;
    }

    /*
	 * 
	 */
    public String getMediaUrl(Size size) {

        switch (mSource) {
            case TWITTER:
                switch (size) {
                    case THUMB:
                        return mMediaCode + ":thumb";
                    case SMALL:
                        return mMediaCode + ":small";
                    case MEDIUM:
                        return mMediaCode + ":medium";
                    case LARGE:
                        return mMediaCode + ":large";
                }
                break;

            case INSTAGRAM:
                switch (size) {
                    case THUMB:
                        return mMediaCode + "?size=t";
                    case SMALL:
                        return mMediaCode + "?size=t";
                    case MEDIUM:
                        return mMediaCode + "?size=m";
                    case LARGE:
                        return mMediaCode + "?size=l";
                }
                break;

            case TWITPIC:
                switch (size) {
                    case THUMB:
                        return "http://twitpic.com/show/mini/" + mMediaCode;
                    case SMALL:
                        return "http://twitpic.com/show/thumb/" + mMediaCode;
                    case MEDIUM:
                        return "http://twitpic.com/show/thumb/" + mMediaCode;
                    case LARGE:
                        return "http://twitpic.com/show/full/" + mMediaCode;
                }
                break;

            case LOCKERZ:
                switch (size) {
                    case THUMB:
                        return mMediaCode + "&size=thumbnail";
                    case SMALL:
                        return mMediaCode + "&size=small";
                    case MEDIUM:
                        return mMediaCode + "&size=medium";
                    case LARGE:
                        return mMediaCode + "&size=big";
                }

            case YFROG:
                switch (size) {
                    // http://yfrog.com/page/api#a5
                    case THUMB:
                        return mMediaCode + ":small";
                    case SMALL:
                        return mMediaCode + ":small";
                    case MEDIUM:
                        return mMediaCode + ":iphone";
                    case LARGE:
                        return mMediaCode + ":medium";
                }

            case IMGUR:
                switch (size) {
                    // http://webapps.stackexchange.com/a/16104
                    case THUMB:
                        return "http://i.imgur.com/" + mMediaCode + "s.png";
                    case SMALL:
                        return "http://i.imgur.com/" + mMediaCode + "b.png";
                    case MEDIUM:
                        return "http://i.imgur.com/" + mMediaCode + "m.png";
                    case LARGE:
                        return "http://i.imgur.com/" + mMediaCode + ".png";
                }

            case YOUTUBE: {
                switch (size) {
                    case THUMB:
                    case SMALL:
                        return "http://img.youtube.com/vi/" + mMediaCode
                                + "/default.jpg";
                    case MEDIUM:
                        return "http://img.youtube.com/vi/" + mMediaCode
                                + "/mqdefault.jpg";
                    case LARGE:
                        return "http://img.youtube.com/vi/" + mMediaCode
                                + "/hqdefault.jpg";
                }
            }
        }

        return null;
    }

    /*
	 * 
	 */
    private Source mSource;
    private String mMediaCode;
    private String mUrl;
    private String mExpandedUrl;

    /*
	 * 
	 */
    private final String KEY_SOURCE = "mSource";
    private final String KEY_MEDIA_CODE = "mMediaCode";
    private final String KEY_URL = "mUrl";
    private final String KEY_EXPANDED_URL = "mExpandedUrl";

    /*
	 * 
	 */
    /*
     * public class SizeInfo {
     * 
     * public SizeInfo(MediaEntity.Size size) { mWidth = size.getWidth();
     * mHeight = size.getHeight(); }
     * 
     * public int getWidth() { return mWidth; } public int getHeight() { return
     * mHeight; }
     * 
     * int mWidth; int mHeight; }
     * 
     * public SizeInfo getSize(Size size) { switch (size) { case THUMB: return
     * mSizeThumb; case SMALL: return mSizeSmall; case MEDIUM: return
     * mSizeMedium; case LARGE: return mSizeLarge; }
     * 
     * return mSizeThumb; }
     * 
     * private SizeInfo mSizeThumb; private SizeInfo mSizeSmall; private
     * SizeInfo mSizeMedium; private SizeInfo mSizeLarge;
     */
}

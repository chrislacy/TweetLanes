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

package org.tweetalib.android.model;

import android.text.Html;
import android.text.Spanned;

import org.appdotnet4j.model.AdnMedia;
import org.appdotnet4j.model.AdnPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterUtil;
import org.tweetalib.android.widget.URLSpanNoUnderline;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserMentionEntity;

public class TwitterStatus implements Comparable<TwitterStatus> {

    /*
     *
	 */
    public TwitterStatus(TwitterStatus other) {
        mAuthorId = other.mAuthorId;
        mAuthorName = other.mAuthorName;
        mAuthorScreenName = other.mAuthorScreenName;
        mCreatedAt = other.mCreatedAt;
        mFavoriteCount = other.mFavoriteCount;
        mId = other.mId;
        mOriginalRetweetId = other.mOriginalRetweetId;
        mInReplyToStatusId = other.mInReplyToStatusId;
        mInReplyToUserId = other.mInReplyToUserId;
        mInReplyToUserScreenName = other.mInReplyToUserScreenName;
        mIsFavorited = other.mIsFavorited;
        mIsRetweet = other.mIsRetweet;
        mIsRetweetedByMe = other.mIsRetweetedByMe;
        if (other.mMediaEntity != null) {
            mMediaEntity = new TwitterMediaEntity(other.mMediaEntity);
        }
        mAdnMedia = other.mAdnMedia;
        mProfileImageOriginalUrl = other.mProfileImageOriginalUrl;
        mProfileImageBiggerUrl = other.mProfileImageBiggerUrl;
        mProfileImageMiniUrl = other.mProfileImageMiniUrl;
        mProfileImageNormalUrl = other.mProfileImageNormalUrl;
        mRetweetCount = other.mRetweetCount;
        mSource = other.mSource;
        mStatus = other.mStatus;
        // setStatusMarkup(other.mStatusSlimMarkup, other.mStatusFullMarkup);
        setStatusMarkup(other.mStatusFullMarkup);
        mUserId = other.mUserId;
        mUserName = other.mUserName;
        mUserScreenName = other.mUserScreenName;
        mUserMentions = other.mUserMentions;
    }

    /*
     *
	 */
    /*
     * public TwitterStatus(Status tweet) { mId = tweet.getId(); mUserId =
     * tweet.getUser().getId(); mUserScreenName = tweet.getUser().getName();
     * mAuthorId = tweet.getUser().getId(); mSource =
     * TwitterUtil.stripMarkup(tweet.getSource()); mStatus = tweet.getText();
     * setStatusMarkup(tweet); mCreatedAt = tweet.getCreatedAt(); mUserMentions
     * = TwitterUtil.getUserMentions(tweet.getUserMentionEntities());
     * mProfileImageUrl = tweet.getUser().getProfileImageURL(); }
     */

    /*
	 *
	 */
    public TwitterStatus(Status status) {
        User statusUser = status.getUser();

        mCreatedAt = status.getCreatedAt();
        mId = status.getId();
        if (status.getInReplyToStatusId() != -1) {
            mInReplyToStatusId = status.getInReplyToStatusId();
        }
        if (status.getInReplyToUserId() != -1) {
            mInReplyToUserId = status.getInReplyToUserId();
        }
        mInReplyToUserScreenName = status.getInReplyToScreenName();
        mIsFavorited = status.isFavorited();
        mIsRetweet = status.isRetweet();
        mIsRetweetedByMe = status.isRetweetedByMe();

        mSource = TwitterUtil.stripMarkup(status.getSource());

        if (statusUser != null) {
            mUserId = statusUser.getId();
            mUserName = statusUser.getName();
            mUserScreenName = statusUser.getScreenName();
        }

        mMediaEntity = TwitterMediaEntity.createMediaEntity(status);

        boolean useDefaultAuthor = true;
        if (mIsRetweet) {
            if (status.getRetweetedStatus() != null && status.getRetweetedStatus().getUser() != null) {
                SetProfileImagesFromUser(new TwitterUser(status.getRetweetedStatus().getUser()));
            }
            mOriginalRetweetId = status.getRetweetedStatus().getId();

            // You'd think this check wasn't necessary, but apparently not...
            UserMentionEntity[] userMentions = status.getUserMentionEntities();
            if (userMentions != null && userMentions.length > 0) {
                useDefaultAuthor = false;
                UserMentionEntity authorMentionEntity = status
                        .getUserMentionEntities()[0];
                mAuthorId = authorMentionEntity.getId();
                mAuthorName = authorMentionEntity.getName();
                mAuthorScreenName = authorMentionEntity.getScreenName();


                Status retweetedStatus = status.getRetweetedStatus();
                mStatus = retweetedStatus.getText();
                setStatusMarkup(retweetedStatus);
                mRetweetCount = retweetedStatus.getRetweetCount();
                mUserMentions = TwitterUtil.getUserMentions(retweetedStatus.getUserMentionEntities());
                mIsRetweetedByMe = retweetedStatus.isRetweetedByMe();
            }
        } else {
            if (statusUser != null) {
                SetProfileImagesFromUser(new TwitterUser(statusUser));
            }
        }

        if (useDefaultAuthor) {
            if (statusUser != null) {
                mAuthorId = statusUser.getId();
            }
            mStatus = status.getText();
            setStatusMarkup(status);
            mRetweetCount = status.getRetweetCount();
            mUserMentions = TwitterUtil.getUserMentions(status
                    .getUserMentionEntities());
        }

        /*
         * if (status.getId() == 171546910249852928L) { mStatus =
         * "<a href=\"http://a.com\">@chrismlacy</a> You've been working on Tweet Lanes for ages. Is it done yet?"
         * ; mStatusMarkup =
         * "<a href=\"http://a.com\">@chrismlacy</a> You've been working on Tweet Lanes for ages. Is it done yet?"
         * ; mAuthorScreenName = "emmarclarke"; mStatusMarkup = mStatus; } else
         * if (status.getId() == 171444098698457089L) { mStatus =
         * "<a href=\"http://a.com\">@chrismlacy</a> How's that app of yours coming along?"
         * ; mStatusMarkup =
         * "<a href=\"http://a.com\">@chrismlacy</a> How's that app of yours coming along?"
         * ; mStatusMarkup = mStatus; }
         */
    }

    public TwitterStatus(AdnPost post) {
        mId = post.mId;
        mCreatedAt = post.mCreatedAt;
        mInReplyToStatusId = post.mInReplyTo;

        mSource = post.mSource;
        if (post.mUser != null) {
            mUserId = post.mUser.mId;
            mUserName = post.mUser.mName;
            mUserScreenName = post.mUser.mUserName;
        }
        mIsFavorited = post.mIsFavorited;
        mIsRetweetedByMe = post.mIsRetweetedByMe;

        if (post.mIsRetweet) {
            mAuthorId = post.mOriginalAuthor.mId;
            mAuthorName = post.mOriginalAuthor.mName;
            mAuthorScreenName = post.mOriginalAuthor.mUserName;
            SetProfileImagesFromUser(new TwitterUser(post.mOriginalAuthor));
            mOriginalRetweetId = post.mOriginalId;
        } else {
            SetProfileImagesFromUser(new TwitterUser(post.mUser));
        }


        mIsRetweet = post.mIsRetweet;

        mAdnMedia = post.mEmbeddedMedia;
        mStatus = post.mText;
        setStatusMarkup(post);

    }

    private void SetProfileImagesFromUser(TwitterUser user) {
        if (user.getProfileImageUrlOriginal() != null) {
            mProfileImageOriginalUrl = user.getProfileImageUrlOriginal();
        }

        if (user.getProfileImageUrlNormal() != null) {
            mProfileImageNormalUrl = user.getProfileImageUrlNormal();
        }

        if (user.getProfileImageUrlBigger() != null) {
            mProfileImageBiggerUrl = user.getProfileImageUrlBigger();
        }

        if (user.getProfileImageUrlMini() != null) {
            mProfileImageMiniUrl = user.getProfileImageUrlMini();
        }
    }

    /*
	 *
	 */
    public TwitterStatus(String jsonAsString) {

        try {
            JSONObject object = new JSONObject(jsonAsString);
            if (object.has(KEY_AUTHOR_ID)) {
                mAuthorId = object.getLong(KEY_AUTHOR_ID);
            }
            if (object.has(KEY_AUTHOR_NAME)) {
                mAuthorName = object.getString(KEY_AUTHOR_NAME);
            }
            if (object.has(KEY_AUTHOR_SCREEN_NAME)) {
                mAuthorScreenName = object.getString(KEY_AUTHOR_SCREEN_NAME);
            }
            if (object.has(KEY_CREATED_AT)) {
                long createdAt = object.getLong(KEY_CREATED_AT);
                mCreatedAt = new Date(createdAt);
            }
            // private final String KEY_CREATED_AT = "mCreatedAt";

            if (object.has(KEY_PROFILE_IMAGE_ORIGINAL_URL)) {
                String url = object.getString(KEY_PROFILE_IMAGE_ORIGINAL_URL);
                if (url != null) {
                    mProfileImageOriginalUrl = url;
                }
            }

            if (object.has(KEY_PROFILE_IMAGE_NORMAL_URL)) {
                String url = object.getString(KEY_PROFILE_IMAGE_NORMAL_URL);
                if (url != null) {
                    mProfileImageNormalUrl = url;
                }
            }

            if (object.has(KEY_PROFILE_IMAGE_MINI_URL)) {
                String url = object.getString(KEY_PROFILE_IMAGE_MINI_URL);
                if (url != null) {
                    mProfileImageMiniUrl = url;
                }
            }

            if (object.has(KEY_PROFILE_IMAGE_BIGGER_URL)) {
                String url = object.getString(KEY_PROFILE_IMAGE_BIGGER_URL);
                if (url != null) {
                    mProfileImageBiggerUrl = url;
                }
            }

            mId = object.getLong(KEY_ID);
            if (object.has(KEY_ORIGINAL_ID)) {
                mOriginalRetweetId = object.getLong(KEY_ORIGINAL_ID);
            }
            if (object.has(KEY_IN_REPLY_TO_STATUS_ID)) {
                mInReplyToStatusId = object.getLong(KEY_IN_REPLY_TO_STATUS_ID);
            }
            if (object.has(KEY_IN_REPLY_TO_USER_ID)) {
                mInReplyToUserId = object.getLong(KEY_IN_REPLY_TO_USER_ID);
            }
            if (object.has(KEY_IN_REPLY_TO_USER_SCREEN_NAME)) {
                mInReplyToUserScreenName = object
                        .getString(KEY_IN_REPLY_TO_USER_SCREEN_NAME);
            }
            if (object.has(KEY_IS_FAVORITED)) {
                mIsFavorited = object.getBoolean(KEY_IS_FAVORITED);
            }
            if (object.has(KEY_IS_RETWEET)) {
                mIsRetweet = object.getBoolean(KEY_IS_RETWEET);
            }
            if (object.has(KEY_IS_RETWEETED_BY_ME)) {
                mIsRetweetedByMe = object.getBoolean(KEY_IS_RETWEETED_BY_ME);
            }
            if (object.has(KEY_RETWEET_COUNT)) {
                mRetweetCount = object.getInt(KEY_RETWEET_COUNT);
            }
            if (object.has(KEY_STATUS)) {
                mStatus = object.getString(KEY_STATUS);
            }
            if (object.has(KEY_SOURCE)) {
                mSource = object.getString(KEY_SOURCE);
            }
            mUserId = object.getLong(KEY_USER_ID);
            mUserScreenName = object.getString(KEY_USER_SCREEN_NAME);
            mUserName = object.getString(KEY_USER_NAME);
            if (object.has(KEY_USER_MENTIONS)) {

                ArrayList<String> mentions = new ArrayList<String>();

                String mentionsAsString = object.getString(KEY_USER_MENTIONS);
                JSONArray jsonArray = new JSONArray(mentionsAsString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    String mention = jsonArray.getString(i);
                    mentions.add(mention);
                }

                if (mentions.size() > 0) {
                    String[] stringArray = new String[mentions.size()];
                    mentions.toArray(stringArray);
                    mUserMentions = stringArray;
                }
            }

            if (object.has(KEY_MEDIA_ENTITY)) {
                mMediaEntity = TwitterMediaEntity.createFromString(object
                        .getString(KEY_MEDIA_ENTITY));
            }
            if (object.has(KEY_ADN_MEDIA_ENTITY)) {
                mAdnMedia = new AdnMedia(object.getString(KEY_ADN_MEDIA_ENTITY));
            }

            if (object.has(KEY_STATUS_SLIM_MARKUP)) {
                String statusFullMarkup = null;
                String KEY_STATUS_FULL_MARKUP = "mStatusFullMarkup";
                if (object.has(KEY_STATUS_FULL_MARKUP)) {
                    statusFullMarkup = object.getString(KEY_STATUS_FULL_MARKUP);
                }
                if (statusFullMarkup == null) {
                    statusFullMarkup = object.getString(KEY_STATUS_SLIM_MARKUP);
                }
                // setStatusMarkup(object.getString(KEY_STATUS_SLIM_MARKUP),
                // statusFullMarkup);
                setStatusMarkup(statusFullMarkup);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
	 *
	 */
    public String toString() {

        JSONObject object = new JSONObject();
        try {
            object.put(KEY_AUTHOR_ID, mAuthorId);
            if (mAuthorName != null) {
                object.put(KEY_AUTHOR_NAME, mAuthorName);
            }
            if (mAuthorScreenName != null) {
                object.put(KEY_AUTHOR_SCREEN_NAME, mAuthorScreenName);
            }
            object.put(KEY_CREATED_AT, mCreatedAt.getTime());
            if (mProfileImageOriginalUrl != null) {
                String url = mProfileImageOriginalUrl;
                object.put(KEY_PROFILE_IMAGE_ORIGINAL_URL, url);
            }
            if (mProfileImageMiniUrl != null) {
                String url = mProfileImageMiniUrl;
                object.put(KEY_PROFILE_IMAGE_MINI_URL, url);
            }
            if (mProfileImageNormalUrl != null) {
                String url = mProfileImageNormalUrl;
                object.put(KEY_PROFILE_IMAGE_NORMAL_URL, url);
            }
            if (mProfileImageBiggerUrl != null) {
                String url = mProfileImageBiggerUrl;
                object.put(KEY_PROFILE_IMAGE_BIGGER_URL, url);
            }
            object.put(KEY_ID, mId);
            if (mInReplyToStatusId != null) {
                object.put(KEY_IN_REPLY_TO_STATUS_ID, mInReplyToStatusId);
            }
            object.put(KEY_ORIGINAL_ID, mOriginalRetweetId);

            if (mInReplyToUserId != null) {
                object.put(KEY_IN_REPLY_TO_USER_ID, mInReplyToUserId);
            }
            if (mInReplyToUserScreenName != null) {
                object.put(KEY_IN_REPLY_TO_USER_SCREEN_NAME,
                        mInReplyToUserScreenName);
            }
            object.put(KEY_IS_FAVORITED, mIsFavorited);
            object.put(KEY_IS_RETWEET, mIsRetweet);
            object.put(KEY_IS_RETWEETED_BY_ME, mIsRetweetedByMe);
            object.put(KEY_RETWEET_COUNT, mRetweetCount);
            object.put(KEY_STATUS, mStatus);
            object.put(KEY_STATUS_SLIM_MARKUP, mStatusFullMarkup);
            // if (mStatusFullMarkup != null) {
            // object.put(KEY_STATUS_FULL_MARKUP, mStatusFullMarkup);
            // }
            object.put(KEY_SOURCE, mSource);
            object.put(KEY_USER_ID, mUserId);
            object.put(KEY_USER_SCREEN_NAME, mUserScreenName);
            object.put(KEY_USER_NAME, mUserName);

            if (mUserMentions != null && mUserMentions.length > 0) {
                JSONArray mentions = new JSONArray();
                for (String mention : mUserMentions) {
                    mentions.put(mention);
                }
                object.put(KEY_USER_MENTIONS, mentions);
            }

            if (mMediaEntity != null) {
                object.put(KEY_MEDIA_ENTITY, mMediaEntity.toString());
            }
            if (mAdnMedia != null) {
                object.put(KEY_ADN_MEDIA_ENTITY, mAdnMedia.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();

    }

    /*
	 *
	 */
    private final String KEY_AUTHOR_ID = "mAuthorId";
    private final String KEY_AUTHOR_NAME = "mAuthorName";
    private final String KEY_AUTHOR_SCREEN_NAME = "mAuthorScreenName";
    // private final String KEY_"mFavoriteCount";
    private final String KEY_CREATED_AT = "mCreatedAt";
    private final String KEY_PROFILE_IMAGE_ORIGINAL_URL = "mProfileImageOriginalUrl";
    private final String KEY_PROFILE_IMAGE_MINI_URL = "mProfileImageMiniUrl";
    private final String KEY_PROFILE_IMAGE_NORMAL_URL = "mProfileImageNormalUrl";
    private final String KEY_PROFILE_IMAGE_BIGGER_URL = "mProfileImageBiggerUrl";
    private final String KEY_ID = "mId";
    private final String KEY_ORIGINAL_ID = "mOriginalRetweetId";
    private final String KEY_IN_REPLY_TO_STATUS_ID = "mInReplyToStatusId";
    private final String KEY_IN_REPLY_TO_USER_ID = "mInReplyToUserId";
    private final String KEY_IN_REPLY_TO_USER_SCREEN_NAME = "mInReplyToUserScreenName";
    private final String KEY_IS_FAVORITED = "mIsFavorited";
    private final String KEY_IS_RETWEET = "mIsRetweet";
    private final String KEY_IS_RETWEETED_BY_ME = "mIsRetweetedByMe";
    private final String KEY_RETWEET_COUNT = "mRetweetCount";
    private final String KEY_STATUS = "mStatus";
    private final String KEY_STATUS_SLIM_MARKUP = "mStatusMarkup";
    private final String KEY_SOURCE = "mSource";
    private final String KEY_USER_ID = "mUserId";
    private final String KEY_USER_SCREEN_NAME = "mUserScreenName";
    private final String KEY_USER_NAME = "mUserName";
    private final String KEY_USER_MENTIONS = "mUserMentions";
    private final String KEY_MEDIA_ENTITY = "mMediaEntity";
    private final String KEY_ADN_MEDIA_ENTITY = "mAdnMedia";

    /*
	 *
	 */
    public long mAuthorId;
    private String mAuthorName;
    private String mAuthorScreenName;
    public long mFavoriteCount;
    public Date mCreatedAt;
    private String mProfileImageOriginalUrl;
    private String mProfileImageMiniUrl;
    private String mProfileImageNormalUrl;
    private String mProfileImageBiggerUrl;
    public long mId;
    public long mOriginalRetweetId;
    public Long mInReplyToStatusId;
    private Long mInReplyToUserId;
    private String mInReplyToUserScreenName;
    public boolean mIsFavorited;
    public boolean mIsRetweet;
    public boolean mIsRetweetedByMe;
    public int mRetweetCount;
    public String mStatus;
    // public String mStatusSlimMarkup; // 'Slim' has the first media link
    // removed from the text
    // public Spanned mStatusSlimSpanned;
    private String mStatusFullMarkup; // 'Full' contains all media links. Used in
    // conversation view.
    public Spanned mStatusFullSpanned;
    public String mSource;
    public long mUserId;
    public String mUserScreenName;
    public String mUserName;
    public String[] mUserMentions;
    public TwitterMediaEntity mMediaEntity;
    public AdnMedia mAdnMedia;

    public String getAuthorName() {
        return mAuthorName != null ? mAuthorName : mUserName;
    }

    public String getAuthorScreenName() {
        return mAuthorScreenName != null ? mAuthorScreenName : mUserScreenName;
    }

    // public String getStatusFullMarkup() { return mStatusFullMarkup != null ?
    // mStatusFullMarkup : mStatusSlimMarkup; }
    // public Spanned getStatusFullSpanned() { return mStatusFullSpanned != null
    // ? mStatusFullSpanned : mStatusSlimSpanned; }

    /*
     * public long getAuthorId() { return mAuthorId; } public Date
     * getCreatedAt() { return mCreatedAt; } public long getFavoriteCount() {
     * return mFavoriteCount; } public long getId() { return mId; } public Long
     * getInReplyToStatusId() { return mInReplyToStatusId; } public Long
     * getInReplyToUserId() { return mInReplyToUserId; } public String
     * getInReplyToUserScreenName() { return mInReplyToUserScreenName; } public
     * boolean getIsFavorited() { return mIsFavorited; } public boolean
     * getIsRetweet() { return mIsRetweet; } public boolean getIsRetweetedByMe()
     * { return mIsRetweetedByMe; } public URL getProfileImageUrl() { return
     * mProfileImageUrl; } public long getRetweetCount() { return mRetweetCount;
     * } public String getSource() { return mSource; } public String
     * getStatusMarkup() { return mStatusMarkup; } public Spanned
     * getStatusSpanned() { return mStatusSpanned; } public String getStatus() {
     * return mStatus; } public long getUserId() { return mUserId; } public
     * String getUserName() { return mUserName; } public String
     * getUserScreenName() { return mUserScreenName; } public String[]
     * getUserMentions() { return mUserMentions; } public TwitterMediaEntity
     * getMediaEntity() { return mMediaEntity; }
     */

    /*
	 *
	 */
    public String getTwitterComStatusUrl() {
        return "https://twitter.com/" + getAuthorScreenName() + "/status/" + mId;
    }

    public String getAdnStatusUrl() {
        return "https://alpha.app.net/" + getAuthorScreenName() + "/post/" + mId;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(TwitterStatus other) {

        if (mId < other.mId) {
            return 1;
        }
        return -1;
    }

    /*
	 *
	 */
    private static boolean compareString(String string1, String string2) {
        if (string1 == null) {
            if (string2 == null) {
                return true;
            }
        } else {
            if (string2 != null && string1.equals(string2)) {
                return true;
            }
        }

        return false;
    }

    private static boolean compareLong(Long long1, Long long2) {
        if (long1 == null) {
            if (long2 == null) {
                return true;
            }
        } else {
            if (long2 != null && long1.longValue() == long2.longValue()) {
                return true;
            }
        }

        return false;
    }

    /*
	 *
	 */
    public boolean isEqualTo(TwitterStatus other) {

        if (mAuthorId != other.mAuthorId) return false;
        if (!compareString(mAuthorName, other.mAuthorName))
            return false;
        if (!compareString(mAuthorScreenName, other.mAuthorScreenName))
            return false;
        if (mFavoriteCount != other.mFavoriteCount) return false;
        if (mCreatedAt.getTime() != other.mCreatedAt.getTime()) return false;
        if (!mProfileImageOriginalUrl.equals(
                other.mProfileImageOriginalUrl)) return false;
        if (!mProfileImageNormalUrl.equals(
                other.mProfileImageNormalUrl)) return false;
        if (!mProfileImageMiniUrl.equals(
                other.mProfileImageMiniUrl)) return false;
        if (!mProfileImageBiggerUrl.equals(
                other.mProfileImageBiggerUrl)) return false;
        if (mId != other.mId) return false;
        if (!compareLong(mInReplyToStatusId, other.mInReplyToStatusId))
            return false;
        if (!compareLong(mInReplyToUserId, other.mInReplyToUserId))
            return false;
        if (!compareString(mInReplyToUserScreenName,
                other.mInReplyToUserScreenName)) return false;
        if (mIsFavorited != other.mIsFavorited) return false;
        if (mIsRetweet != other.mIsRetweet) return false;
        if (mIsRetweetedByMe != other.mIsRetweetedByMe) return false;
        if (mOriginalRetweetId != other.mOriginalRetweetId) return false;
        if (mRetweetCount != other.mRetweetCount) return false;
        if (!compareString(mStatus, other.mStatus)) return false;
        // if (compareString(mStatusSlimMarkup, other.mStatusSlimMarkup) ==
        // false)
        // return false;
        if (!compareString(mStatusFullMarkup, other.mStatusFullMarkup))
            return false;
        if (!compareString(mSource, other.mSource)) return false;
        if (mUserId != other.mUserId) return false;
        if (!compareString(mUserScreenName, other.mUserScreenName))
            return false;
        if (!compareString(mUserName, other.mUserName)) return false;

        if (mUserMentions != null) {
            if (other.mUserMentions == null) {
                return false;
            }
            if (mUserMentions.length != other.mUserMentions.length) {
                return false;
            }
        } else if (other.mUserMentions != null) {
            return false;
        }

        return true;
    }

    /*
	 *
	 */
    void setStatusMarkup(Status status) {
        mStatusFullMarkup = TwitterUtil.getStatusMarkup(status);
        mStatusFullSpanned = URLSpanNoUnderline.stripUnderlines(Html
                .fromHtml(mStatusFullMarkup.replace("\n", "<br/>") + " "));
    }

    void setStatusMarkup(String full) {
        mStatusFullMarkup = full;
        if (mStatusFullMarkup != null) {
            mStatusFullSpanned = URLSpanNoUnderline.stripUnderlines(Html
                    .fromHtml(mStatusFullMarkup.replace("\n", "<br/>") + " "));
        }
    }

    void setStatusMarkup(AdnPost post) {
        mStatusFullMarkup = TwitterUtil.getStatusMarkup(post);
        mStatusFullSpanned = URLSpanNoUnderline.stripUnderlines(Html
                .fromHtml(mStatusFullMarkup.replace("\n", "<br/>") + " "));
    }

    /*
     * HACKALERT: This exists only so that we can accurately reflect the
     * mIsFavorited value immediately after we set the value. For reasoning, see
     * here:
     * "This process invoked by this method is asynchronous. The immediately returned status may not indicate the resultant favorited status of the tweet."
     * https://dev.twitter.com/docs/api/1/post/favorites/create/:id
     */
    public void setFavorite(Boolean isFavorited) {
        mIsFavorited = isFavorited;
    }

    public void setRetweet(Boolean isRetweetedByMe) {
        mIsRetweetedByMe = isRetweetedByMe;
    }

    public String getProfileImageUrl(TwitterManager.ProfileImageSize size) {

        switch (size) {
            case MINI:
                return mProfileImageMiniUrl;
            case NORMAL:
                return mProfileImageNormalUrl;
            case BIGGER:
                return mProfileImageBiggerUrl;
            case ORIGINAL:
                return mProfileImageOriginalUrl;
        }
        return "";
    }

    private static final Pattern HASHTAG_PATTERN =
            Pattern.compile("(?:^|\\s|[\\p{Punct}&&[^/]])(#[\\p{L}0-9-_]+)");

    public List<String> getHashtags() {
        ArrayList<String> list = new ArrayList<String>();

        Matcher matcher = HASHTAG_PATTERN.matcher(mStatus);

        while (matcher.find()) {
            list.add(matcher.group().trim());
        }

        return list;
    }
}

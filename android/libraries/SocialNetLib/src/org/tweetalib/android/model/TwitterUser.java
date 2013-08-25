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

import org.appdotnet4j.model.AdnUser;
import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterManager.ProfileImageSize;

import java.util.ArrayList;
import java.util.Arrays;

import twitter4j.URLEntity;
import twitter4j.User;

public class TwitterUser {

    public TwitterUser(User user) {
        mId = user.getId();
        mScreenName = user.getScreenName();
        mName = user.getName();
        mDescription = user.getDescription();
        ArrayList<URLEntity> urlEntityArrayList = new ArrayList<URLEntity>();
        if (user.getDescriptionURLEntities() != null) {
            urlEntityArrayList = new ArrayList<URLEntity>(Arrays.asList(user.getDescriptionURLEntities()));
        }

        if (user.getURL() != null) {
            mUrl = user.getURL();
            urlEntityArrayList.add(user.getURLEntity());
        }

        mUrlEntities = urlEntityArrayList.toArray(new URLEntity[urlEntityArrayList.size()]);

        if (user.getLocation() != null
                && !user.getLocation().equals("")) {
            mLocation = user.getLocation();
        }

        if (user.getOriginalProfileImageURLHttps() != null) {
            mProfileImageUrlOriginal = user.getOriginalProfileImageURLHttps();
        }

        if (user.getBiggerProfileImageURLHttps() != null) {
            mProfileImageUrlBigger = user.getBiggerProfileImageURLHttps();
        }

        if (user.getProfileImageURLHttps() != null) {
            mProfileImageUrlNormal = user.getProfileImageURLHttps();
        }

        if (user.getMiniProfileImageURLHttps() != null) {
            mProfileImageUrlMini = user.getMiniProfileImageURLHttps();
        }

        mStatusesCount = user.getStatusesCount();
        mFriendsCount = user.getFriendsCount();
        mFollowersCount = user.getFollowersCount();
        mFavoritesCount = user.getFavouritesCount();
        mListedCount = user.getListedCount();
        mVerified = user.isVerified();
        mProtected = user.isProtected();
        mSocialNetType = SocialNetConstant.Type.Twitter;
    }

    public TwitterUser(AdnUser user) {
        mId = user.mId;
        mScreenName = user.mUserName;
        mName = user.mName;
        mFollowersCount = user.mFollowersCount;
        mFriendsCount = user.mFollowingCount;
        mStatusesCount = user.mPostCount;
        mCoverImageUrl = user.mCoverUrl;
        mDescription = user.mDescription;
        mSocialNetType = SocialNetConstant.Type.Appdotnet;
        mCurrentUserFollows = user.mCurrentUserFollows;
        mFollowsCurrentUser = user.mFollowsCurrentUser;
        mFavoritesCount = user.mFavoritesCount;
        mProfileImageUrlMini = "https://alpha-api.app.net/stream/0/users/@" + user.mUserName + "/avatar?w=32";
        mProfileImageUrlNormal = "https://alpha-api.app.net/stream/0/users/@" + user.mUserName + "/avatar?w=48";
        mProfileImageUrlBigger = "https://alpha-api.app.net/stream/0/users/@" + user.mUserName + "/avatar?w=73";
        mProfileImageUrlOriginal = "https://alpha-api.app.net/stream/0/users/@" + user.mUserName + "/avatar";
    }

    public TwitterUser(TwitterUser user) {
        mId = user.getId();
        mScreenName = user.getScreenName();
        mName = user.getName();
        mDescription = user.getDescription();
        mUrlEntities = user.getUrlEntities();
        mLocation = user.getLocation();
        mUrl = user.getUrl();

        mProfileImageUrlMini = user.getProfileImageUrlMini();
        mProfileImageUrlNormal = user.getProfileImageUrlNormal();
        mProfileImageUrlBigger = user.getProfileImageUrlBigger();
        mProfileImageUrlOriginal = user.getProfileImageUrlOriginal();

        mStatusesCount = user.getStatusesCount();
        mFriendsCount = user.getFriendsCount();
        mFollowersCount = user.getFollowersCount();
        mFavoritesCount = user.getFavoritesCount();
        mListedCount = user.getListedCount();
        mVerified = user.getVerified();
        mProtected = user.getProtected();
        mSocialNetType = user.getSocialNetType();
    }

    public long getId() {
        return mId;
    }

    public String getScreenName() {
        return mScreenName;
    }

    public String getName() {
        return mName;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getCoverImageUrl() {
        return mCoverImageUrl;
    }

    public String getLocation() {
        return mLocation;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getProfileImageUrlMini() {
        return mProfileImageUrlMini;
    }

    public String getProfileImageUrlNormal() {
        return mProfileImageUrlNormal;
    }

    public String getProfileImageUrlBigger() {
        return mProfileImageUrlBigger;
    }

    public String getProfileImageUrlOriginal() {
        return mProfileImageUrlOriginal;
    }

    public int getStatusesCount() {
        return mStatusesCount;
    }

    public int getFriendsCount() {
        return mFriendsCount;
    }

    public int getFollowersCount() {
        return mFollowersCount;
    }

    public int getFavoritesCount() {
        return mFavoritesCount;
    }

    int getListedCount() {
        return mListedCount;
    }

    public boolean getVerified() {
        return mVerified;
    }

    public boolean getProtected() {
        return mProtected;
    }

    public boolean getFollowsCurrentUser() {
        return mFollowsCurrentUser;
    }

    public boolean getCurrentUserFollows() {
        return mCurrentUserFollows;
    }

    public SocialNetConstant.Type getSocialNetType() {
        return mSocialNetType;
    }

    public String getProfileImageUrl(ProfileImageSize size) {

        switch (size) {
            case MINI:
                return mProfileImageUrlMini;
            case NORMAL:
                return mProfileImageUrlNormal;
            case BIGGER:
                return mProfileImageUrlBigger;
            case ORIGINAL:
                return mProfileImageUrlOriginal;
        }
        return "";
    }

    public URLEntity[] getUrlEntities() {
        return mUrlEntities;
    }

    private final long mId;
    private final String mScreenName;
    private final String mName;
    private final String mDescription;
    private URLEntity[] mUrlEntities;
    private String mCoverImageUrl;
    private String mLocation;
    private String mProfileImageUrlMini;
    private String mProfileImageUrlNormal;
    private String mProfileImageUrlBigger;
    private String mProfileImageUrlOriginal;
    private String mUrl;
    private final int mStatusesCount;
    private final int mFriendsCount;
    private final int mFollowersCount;
    private final int mFavoritesCount;
    private int mListedCount;
    private boolean mVerified;
    private boolean mProtected;
    private final SocialNetConstant.Type mSocialNetType;
    private boolean mFollowsCurrentUser;
    private boolean mCurrentUserFollows;
}

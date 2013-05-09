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

package com.tweetlanes.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.tweetlanes.android.view.SettingsActivity;

public class AppSettings {

    public static final boolean DEFAULT_DOWNLOAD_IMAGES = true;
    public static final boolean DEFAULT_VOLSCROLL = true;
    public static final boolean DEFAULT_SHOW_TABLET_MARGIN = true;
    public static final boolean DEFAULT_SHOW_TWEET_SOURCE = false;
    public static final boolean DEFAULT_SHOW_NOTIFICATIONS = false;

    private static final String STATUS_SIZE_EXTRA_SMALL = "Extra Small";
    private static final String STATUS_SIZE_SMALL = "Small";
    private static final String STATUS_SIZE_MEDIUM = "Medium";
    private static final String STATUS_SIZE_LARGE = "Large";
    private static final String STATUS_SIZE_EXTRA_LARGE = "Extra Large";
    private static final String STATUS_SIZE_DEFAULT = STATUS_SIZE_MEDIUM;

    private static final String PROFILE_IMAGE_SIZE_SMALL = "Small";
    private static final String PROFILE_IMAGE_SIZE_MEDIUM = "Medium";
    private static final String PROFILE_IMAGE_SIZE_LARGE = "Large";
    private static final String PROFILE_IMAGE_SIZE_DEFAULT = PROFILE_IMAGE_SIZE_MEDIUM;

    public static final String THEME_LIGHT = "Holo Light";
    public static final String THEME_DEFAULT = THEME_LIGHT;

    public static final String QUOTE_TYPE_STANDARD = "standard";
    public static final String QUOTE_TYPE_RT = "rt";
    public static final String QUOTE_TYPE_VIA = "via";
    public static final String QUOTE_TYPE_DEFAULT = QUOTE_TYPE_STANDARD;

    public static final String NAME_DISPLAY_USERNAME = "username";
    public static final String NAME_DISPLAY_NAME = "name";
    public static final String NAME_DISPLAY_USERNAME_NAME = "username_name";
    public static final String NAME_DISPLAY_NAME_USERNAME = "name_username";

    /*
	 *
	 */
    public enum Theme {
        Holo_Dark, Holo_Light
    };

    /*
	 *
	 */
    public enum StatusSize {
        ExtraSmall, Small, Medium, Large, ExtraLarge,
    }

    /*
	 *
	 */
    public enum ProfileImageSize {
        Small, Medium, Large,
    }

    /*
	 *
	 */
    public enum NameDisplay {
        Username, Name, Username_Name, Name_Username,
    }

    /*
	 *
	 */
    public enum QuoteType {
        Standard, RT, Via,
    }

    /*
	 *
	 */
    private SharedPreferences mSharedPreferences;
    private Context mContext;
    private boolean mIsDirty = false;
    private int mRefreshCount = 0;

    private Theme mCurrentTheme;
    private StatusSize mStatusSize;
    private ProfileImageSize mProfileImageSize;
    private NameDisplay mNameDisplay;
    private QuoteType mQuoteType;

    /*
	 *
	 */
    AppSettings(Context context) {
        mContext = context;
        refresh(null);
    }

    /*
	 *
	 */
    public boolean isDirty() {
        boolean old = mIsDirty;
        mIsDirty = false;
        return old;
    }

    /*
	 *
	 */
    public void refresh(String preferenceKey) {
        mIsDirty = false;

        Theme oldTheme = mCurrentTheme;
        StatusSize oldStatusSize = mStatusSize;
        ProfileImageSize oldProfileImageSize = mProfileImageSize;

        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);

        String theme = mSharedPreferences.getString(
                SettingsActivity.KEY_THEME_PREFERENCE, THEME_DEFAULT);
        setCurrentTheme(theme.equals(THEME_LIGHT) ? Theme.Holo_Light
                : Theme.Holo_Dark);

        String statusSize = mSharedPreferences.getString(
                SettingsActivity.KEY_STATUS_SIZE_PREFERENCE,
                STATUS_SIZE_DEFAULT);
        setCurrentStatusSize(statusSize);

        String profileImageSize = mSharedPreferences.getString(
                SettingsActivity.KEY_PROFILE_IMAGE_SIZE_PREFERENCE,
                PROFILE_IMAGE_SIZE_DEFAULT);
        setCurrentProfileImageSize(profileImageSize);

        String quoteType = mSharedPreferences.getString(
                SettingsActivity.KEY_QUOTE_TYPE_PREFERENCE, QUOTE_TYPE_DEFAULT);
        setCurrentQuoteType(quoteType);

        if (mRefreshCount > 0) {
            if (oldTheme != mCurrentTheme || oldStatusSize != mStatusSize
                    || oldProfileImageSize != mProfileImageSize) {
                mIsDirty = true;
            } else if (preferenceKey != null) {
                if (preferenceKey
                        .equalsIgnoreCase(SettingsActivity.KEY_SHOW_TABLET_MARGIN_PREFERENCE)
                        || preferenceKey
                                .equalsIgnoreCase(SettingsActivity.KEY_CLEAR_IMAGE_CACHE_PREFERENCE)
                        || preferenceKey
                                .equalsIgnoreCase(SettingsActivity.KEY_CUSTOMIZE_LANES_PREFERENCE)
                        || preferenceKey
                                .equalsIgnoreCase(SettingsActivity.KEY_SHOW_TWEET_SOURCE_PREFERENCE)) {
                    mIsDirty = true;
                }
            }

        }
        mRefreshCount += 1;
    }

    /*
	 *
	 */
    public boolean downloadFeedImages() {
        return mSharedPreferences.getBoolean(
                SettingsActivity.KEY_DOWNLOADIMAGES_PREFERENCE,
                DEFAULT_DOWNLOAD_IMAGES);
    }

    /*
	 *
	 */
    public boolean showTweetSource() {
        return mSharedPreferences.getBoolean(
                SettingsActivity.KEY_SHOW_TWEET_SOURCE_PREFERENCE,
                DEFAULT_SHOW_TWEET_SOURCE);
    }

    /*
	 *
	 */
    public boolean isVolScrollEnabled() {
        return mSharedPreferences.getBoolean(
                SettingsActivity.KEY_VOLSCROLL_PREFERENCE, DEFAULT_VOLSCROLL);
    }

    /*
	 *
	 */
    public boolean isDimScreenEnabled() {
        return true;
    }

    public Uri getRingtoneUri() {
        String uri = mSharedPreferences.getString(SettingsActivity.KEY_RINGTONE_PREFERENCE, null);
        if (uri == null) {
            return null;
        }
        return Uri.parse(uri);
    }

    /*
	 *
	 */
    public boolean showTabletMargin() {
        if (mSharedPreferences != null
                && mSharedPreferences
                        .contains(SettingsActivity.KEY_SHOW_TABLET_MARGIN_PREFERENCE)) {
            return mSharedPreferences.getBoolean(
                    SettingsActivity.KEY_SHOW_TABLET_MARGIN_PREFERENCE,
                    DEFAULT_SHOW_TABLET_MARGIN);
        }

        return DEFAULT_SHOW_TABLET_MARGIN;
    }

    /*
	 *
	 */
    public int getCurrentThemeStyle() {
        return mCurrentTheme == Theme.Holo_Dark ? R.style.Theme_TweetLanes
                : R.style.Theme_TweetLanes_Light;
    }

    /*
	 *
	 */
    public int getCurrentBorderColor() {
        return mCurrentTheme == Theme.Holo_Dark ? 0xff4d4d4d : 0xffcccccc;
    }

    /*
	 *
	 */
    public Theme getCurrentTheme() {
        return mCurrentTheme;
    }

    /*
	 *
	 */
    private void setCurrentTheme(Theme theme) {
        mCurrentTheme = theme;
    }

    /*
	 *
	 */
    void setCurrentStatusSize(String statusSize) {
        if (statusSize != null) {
            if (statusSize.equals(STATUS_SIZE_EXTRA_SMALL)) {
                mStatusSize = StatusSize.ExtraSmall;
            } else if (statusSize.equals(STATUS_SIZE_SMALL)) {
                mStatusSize = StatusSize.Small;
            } else if (statusSize.equals(STATUS_SIZE_MEDIUM)) {
                mStatusSize = StatusSize.Medium;
            } else if (statusSize.equals(STATUS_SIZE_LARGE)) {
                mStatusSize = StatusSize.Large;
            } else if (statusSize.equals(STATUS_SIZE_EXTRA_LARGE)) {
                mStatusSize = StatusSize.ExtraLarge;
            } else {
                mStatusSize = StatusSize.Medium;
            }

        }
    }

    /*
	 *
	 */
    public StatusSize getCurrentStatusSize() {
        return mStatusSize;
    }

    /*
	 *
	 */
    void setCurrentProfileImageSize(String profileImageSize) {
        if (profileImageSize != null) {
            if (profileImageSize.equals(PROFILE_IMAGE_SIZE_SMALL)) {
                mProfileImageSize = ProfileImageSize.Small;
            } else if (profileImageSize.equals(PROFILE_IMAGE_SIZE_MEDIUM)) {
                mProfileImageSize = ProfileImageSize.Medium;
            } else if (profileImageSize.equals(PROFILE_IMAGE_SIZE_LARGE)) {
                mProfileImageSize = ProfileImageSize.Large;
            } else {
                mProfileImageSize = ProfileImageSize.Medium;
            }
        }
    }

    /*
	 *
	 */
    public ProfileImageSize getCurrentProfileImageSize() {
        return mProfileImageSize;
    }

    /*
	 *
	 */
    void setCurrentNameDisplay(String nameDisplay) {
        if (nameDisplay.equals(NAME_DISPLAY_USERNAME)) {
            mNameDisplay = NameDisplay.Username;
        } else if (nameDisplay.equals(NAME_DISPLAY_NAME)) {
            mNameDisplay = NameDisplay.Name;
        } else if (nameDisplay.equals(NAME_DISPLAY_USERNAME_NAME)) {
            mNameDisplay = NameDisplay.Username_Name;
        } else if (nameDisplay.equals(NAME_DISPLAY_NAME_USERNAME)) {
            mNameDisplay = NameDisplay.Name_Username;
        } else {
            mNameDisplay = NameDisplay.Username;
        }
    }

    /*
	 *
	 */
    NameDisplay getCurrentNameDisplay() {
        return mNameDisplay;
    }

    /*
	 *
	 */
    void setCurrentQuoteType(String quoteType) {
        if (quoteType.equals(QUOTE_TYPE_STANDARD)) {
            mQuoteType = QuoteType.Standard;
        } else if (quoteType.equals(QUOTE_TYPE_RT)) {
            mQuoteType = QuoteType.RT;
        } else if (quoteType.equals(QUOTE_TYPE_VIA)) {
            mQuoteType = QuoteType.Via;
        } else {
            mQuoteType = QuoteType.Standard;
        }
    }

    /*
	 *
	 */
    public QuoteType getCurrentQuoteType() {
        return mQuoteType;
    }

    /*
	 *
	 */
    public static void initModule(Context mContext) {
        mInstance = new AppSettings(mContext);
    }

    /*
	 *
	 */
    public static void deinitModule() {
        mInstance = null;
    }

    /*
	 *
	 */
    public static AppSettings get() {
        return mInstance;
    }

    private static AppSettings mInstance = null;
}

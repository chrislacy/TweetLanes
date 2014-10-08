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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crittercism.app.Crittercism;
import com.tweetlanes.android.core.Constant.SystemEvent;
import com.tweetlanes.android.core.model.AccountDescriptor;
import com.tweetlanes.android.core.model.LaneDescriptor;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.widget.urlimageviewhelper.UrlImageViewHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.ConnectionStatus;
import org.tweetalib.android.TwitterConstant;
import org.tweetalib.android.TwitterContentHandleBase;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchUsers;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterLists;
import org.tweetalib.android.model.TwitterUser;

import java.util.ArrayList;
import java.util.List;

// https://docs.google.com/spreadsheet/ccc?key=0Akm3k9q4H2IPdFBibVdkWVlKQ25rX01vV1dub1hjOXc
// @ReportsCrashes(formKey = "dFBibVdkWVlKQ25rX01vV1dub1hjOXc6MQ")
public class App extends Application {

    private static String mAppVersionName;
    private static boolean mActionLauncherInstalled;

    public static String getAppVersionName() {
        return mAppVersionName;
    }

    public static boolean getActionLauncherInstalled() {
        return mActionLauncherInstalled;
    }

    private ArrayList<AccountDescriptor> mAccounts;
    private boolean mAccountDescriptorsDirty = false;
    private Integer mCurrentAccountIndex;

    private ArrayList<LaneDescriptor> mProfileLaneDefinitions = null;

    private ArrayList<LaneDescriptor> mSearchLaneDefinitions = null;

    private ArrayList<LaneDescriptor> mTweetSpotlightLaneDefinitions = null;

    private SharedPreferences mPreferences;

    public AccountDescriptor getCurrentAccount() {
        return mCurrentAccountIndex != null ? mAccounts
                .get(mCurrentAccountIndex) : null;
    }

    public AccountDescriptor getAccountByKey(String accountKey) {
        for (AccountDescriptor account : mAccounts) {
            if (account.getAccountKey().equals(accountKey)) {
                return account;
            }
        }

        return null;
    }

    public String getCurrentAccountScreenName() {
        AccountDescriptor account = getCurrentAccount();
        return account != null ? account.getScreenName() : null;
    }

    public String getCurrentAccountName() {
        AccountDescriptor account = getCurrentAccount();
        return account != null ? account.getName() : null;
    }

    public boolean getAccountDescriptorsDirty() {
        return mAccountDescriptorsDirty;
    }

    /*
     *
	 */
    public void setAccountDescriptorsDirty(boolean value) {
        mAccountDescriptorsDirty = value;
    }

    public int getAccountCount() {
        return mAccounts.size();
    }

    public ArrayList<AccountDescriptor> getAccounts() {
        return mAccounts;
    }

    private Integer getAccountIndexById(Long id) {

        if (id == null) {
            return null;
        }

        for (int i = 0; i < mAccounts.size(); i++) {
            if (mAccounts.get(i).getId() == id) {
                return i;
            }
        }

        return null;
    }

    public void removeAccount(String accountKey) {
        final Editor edit = mPreferences.edit();
        String accountIndices = mPreferences.getString(SharedPreferencesConstants.ACCOUNT_INDICES, null);
        if (accountIndices != null) {
            try {
                JSONArray jsonArray = new JSONArray(accountIndices);
                JSONArray newIndicies = new JSONArray();
                for (int i = 0; i < jsonArray.length(); i++) {
                    Long id = jsonArray.getLong(i);

                    String key = getAccountDescriptorKey(id);
                    String jsonAsString = mPreferences.getString(key, null);
                    if (jsonAsString != null) {
                        AccountDescriptor account = new AccountDescriptor(this,
                                jsonAsString);

                        if (!account.getAccountKey().equals(accountKey)) {
                            newIndicies.put(Long.toString(id));
                        } else {
                            ArrayList<LaneDescriptor> lanes = account.getAllLaneDefinitions();
                            for (LaneDescriptor lane : lanes) {
                                String lanekey = lane.getCacheKey(account.getScreenName() + account.getId());
                                edit.remove(lanekey);
                            }

                            edit.remove(key);
                            mAccounts.remove(account);
                        }
                    }
                }

                accountIndices = newIndicies.toString();
                edit.putString(SharedPreferencesConstants.ACCOUNT_INDICES,
                        accountIndices);

                edit.commit();

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        updateTwitterAccountCount();
    }

    public void setCurrentAccount(Long id) {
        SocialNetConstant.Type currentSocialNetType = getCurrentAccount() == null ? SocialNetConstant.Type.Twitter
                : getCurrentAccount().getSocialNetType();
        mCurrentAccountIndex = getAccountIndexById(id);
        if (mCurrentAccountIndex == null) {
            TwitterManager.get().setOAuthTokenWithSecret(null, null, true);
        } else {
            AccountDescriptor account = mAccounts.get(mCurrentAccountIndex);
            if (account != null) {
                if (account.getSocialNetType() == currentSocialNetType) {
                    TwitterManager.get().setOAuthTokenWithSecret(
                            account.getOAuthToken(), account.getOAuthSecret(),
                            true);
                } else {
                    TwitterManager
                            .initModule(
                                    account.getSocialNetType(),
                                    account.getSocialNetType() == SocialNetConstant.Type.Appdotnet ? ConsumerKeyConstants.APPDOTNET_CONSUMER_KEY
                                            : ConsumerKeyConstants.TWITTER_CONSUMER_KEY,
                                    account.getSocialNetType() == SocialNetConstant.Type.Appdotnet ? ConsumerKeyConstants.APPDOTNET_CONSUMER_SECRET
                                            : ConsumerKeyConstants.TWITTER_CONSUMER_SECRET,
                                    account.getOAuthToken(), account
                                    .getOAuthSecret(),
                                    account.getAccountKey(),
                                    mConnectionStatusCallbacks);

                }

                setLaneDefinitions(account.getSocialNetType());

                final Editor edit = mPreferences.edit();
                edit.putLong(SharedPreferencesConstants.CURRENT_ACCOUNT_ID,
                        account.getId());
                edit.commit();
            } else {
                // TODO: Handle me
            }
        }
    }

    public String getCurrentAccountKey() {
        return getCurrentAccount().getAccountKey();
    }

    public ArrayList<LaneDescriptor> getProfileLaneDefinitions() {
        return mProfileLaneDefinitions;
    }

    public LaneDescriptor getProfileLaneDescriptor(int index) {
        return mProfileLaneDefinitions.get(index);
    }

    public ArrayList<LaneDescriptor> getSearchLaneDefinitions() {
        return mSearchLaneDefinitions;
    }

    public LaneDescriptor getSearchLaneDescriptor(int index) {
        return mSearchLaneDefinitions.get(index);
    }

    public ArrayList<LaneDescriptor> getTweetSpotlightLaneDefinitions() {
        return mTweetSpotlightLaneDefinitions;
    }

    public LaneDescriptor getTweetSpotlightLaneDescriptor(int index) {
        return mTweetSpotlightLaneDefinitions.get(index);
    }

    void updateTwitterAccountCount() {

        mAccounts.clear();

        long currentAccountId = mPreferences.getLong(
                SharedPreferencesConstants.CURRENT_ACCOUNT_ID, -1);
        String accountIndices = mPreferences.getString(
                SharedPreferencesConstants.ACCOUNT_INDICES, null);
        if (accountIndices != null) {
            try {
                JSONArray jsonArray = new JSONArray(accountIndices);
                for (int i = 0; i < jsonArray.length(); i++) {
                    Long id = jsonArray.getLong(i);

                    String key = getAccountDescriptorKey(id);
                    String jsonAsString = mPreferences.getString(key, null);
                    if (jsonAsString != null) {
                        AccountDescriptor account = new AccountDescriptor(this,
                                jsonAsString);
                        if (Constant.ENABLE_APP_DOT_NET == false
                                && account.getSocialNetType() == SocialNetConstant.Type.Appdotnet) {
                            continue;
                        }
                        mAccounts.add(account);

                        if (currentAccountId != -1
                                && account.getId() == currentAccountId) {
                            mCurrentAccountIndex = i;
                        }
                    }
                }

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (mCurrentAccountIndex == null && mAccounts.size() > 0) {
            mCurrentAccountIndex = 0;
        }
    }

    public static String getAccountDescriptorKey(Long id) {
        return "account_descriptor_v2" + id.toString();
    }

    private String getTweetDraftKey() {
        return "draft_" + getCurrentAccountScreenName();
    }

    private String getTweetDraftTimeKey() {
        return "drafttime_" + getCurrentAccountScreenName();
    }

    /*
     *
	 */
    public void saveTweetDraft(String draftAsJsonString) {
        final Editor edit = mPreferences.edit();
        if (draftAsJsonString != null) {
            edit.putString(getTweetDraftKey(), draftAsJsonString);
            edit.putLong(getTweetDraftTimeKey(), System.currentTimeMillis());
        } else {
            edit.remove(getTweetDraftKey());
            edit.remove(getTweetDraftTimeKey());
        }
        edit.commit();
    }

    /*
     *
	 */
    public void clearTweetDraft() {
        final Editor edit = mPreferences.edit();
        edit.remove(getTweetDraftKey());
        edit.remove(getTweetDraftTimeKey());
        edit.commit();
    }

    /*
     *
	 */
    public String getTweetDraftAsString() {

        long saveTime = mPreferences.getLong(getTweetDraftTimeKey(), 0);
        if (saveTime > 0) {
            long currentTime = System.currentTimeMillis();
            long secondsDiff = (currentTime - saveTime) / 1000l;
            if (secondsDiff < Constant.RESTORE_SAVED_DRAFT_SECONDS) {
                return mPreferences.getString(
                        getTweetDraftKey(), null);
            }
        }

        return null;
    }

    /*
     *
	 */
    public void setTutorialCompleted() {
        final Editor edit = mPreferences.edit();
        edit.putBoolean(SharedPreferencesConstants.TUTORIAL_COMPLETED, true);
        edit.commit();
    }

    /*
     *
	 */
    public boolean getTutorialCompleted() {
        return mPreferences.getBoolean(
                SharedPreferencesConstants.TUTORIAL_COMPLETED, false);
    }

    /*
     *
	 */
    public void saveUpdatedAccountDescriptor(AccountDescriptor account) {
        final Editor edit = mPreferences.edit();
        edit.putString(getAccountDescriptorKey(account.getId()),
                account.toString());
        edit.commit();
    }

    /*
	 *
	 */
    public void cacheData(String key, String toCache) {
        if (Constant.UPDATE_CACHED_STATUSES) {
            final Editor edit = mPreferences.edit();
            edit.putString(key, toCache);
            edit.commit();
        }
    }

    /*
	 *
	 */
    public String getCachedData(String key) {
        return mPreferences.getString(key, null);
    }

    public void removeCachedData(String key) {
        final Editor edit = mPreferences.edit();
        edit.remove(key);
        edit.commit();
    }

    /*
	 *
	 */
    public void onPostSignIn(TwitterUser user, String oAuthToken,
                             String oAuthSecret, SocialNetConstant.Type oSocialNetType) {

        if (user != null) {

            try {

                final Editor edit = mPreferences.edit();
                String userIdAsString = Long.toString(user.getId());

                AccountDescriptor account = new AccountDescriptor(this, user,
                        oAuthToken, oAuthSecret, oSocialNetType, user.getProfileImageUrl(TwitterManager.ProfileImageSize.BIGGER));
                edit.putString(getAccountDescriptorKey(user.getId()),
                        account.toString());

                String accountIndices = mPreferences.getString(
                        SharedPreferencesConstants.ACCOUNT_INDICES, null);
                JSONArray jsonArray;

                if (accountIndices == null) {
                    jsonArray = new JSONArray();
                    jsonArray.put(0, user.getId());
                    mAccounts.add(account);
                } else {
                    jsonArray = new JSONArray(accountIndices);
                    boolean exists = false;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String c = jsonArray.getString(i);
                        if (c.compareTo(userIdAsString) == 0) {
                            exists = true;
                            mAccounts.set(i, account);
                            break;
                        }
                    }

                    if (!exists) {
                        jsonArray.put(userIdAsString);
                        mAccounts.add(account);
                    }
                }

                accountIndices = jsonArray.toString();
                edit.putString(SharedPreferencesConstants.ACCOUNT_INDICES,
                        accountIndices);

                edit.commit();

                setCurrentAccount(user.getId());

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            updateTwitterAccountCount();
            if (TwitterManager.get().getSocialNetType() == oSocialNetType) {
                TwitterManager.get().setOAuthTokenWithSecret(oAuthToken,
                        oAuthSecret, true);
            } else {
                TwitterManager
                        .initModule(
                                oSocialNetType,
                                oSocialNetType == SocialNetConstant.Type.Twitter ? ConsumerKeyConstants.TWITTER_CONSUMER_KEY
                                        : ConsumerKeyConstants.APPDOTNET_CONSUMER_KEY,
                                oSocialNetType == SocialNetConstant.Type.Twitter ? ConsumerKeyConstants.TWITTER_CONSUMER_SECRET
                                        : ConsumerKeyConstants.APPDOTNET_CONSUMER_SECRET,
                                oAuthToken, oAuthSecret, getCurrentAccountKey(),
                                mConnectionStatusCallbacks);
            }
        }
    }

    @Override
    public void onCreate() {

        Log.d("tweetlanes url fetch", "*** New run");
        Log.d("AsyncTaskEx", "*** New run");
        Log.d("StatusCache", "*** New run");

        super.onCreate();

        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(
                java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(
                java.util.logging.Level.FINEST);

        System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime",
                "true");
        System.setProperty(
                "org.apache.commons.logging.simplelog.log.httpclient.wire",
                "debug");
        System.setProperty(
                "org.apache.commons.logging.simplelog.log.org.apache.http",
                "debug");
        System.setProperty(
                "org.apache.commons.logging.simplelog.log.org.apache.http.headers",
                "debug");

        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    getPackageName(), 0);
            mAppVersionName = packageInfo.versionName;

            List<ApplicationInfo> apps = packageManager
                    .getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo app : apps) {
                if (app.packageName != null
                        && app.packageName
                        .equalsIgnoreCase("com.chrislacy.actionlauncher.pro")) {
                    mActionLauncherInstalled = true;
                    break;
                }
            }

        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPreferences.edit().putInt(SharedPreferencesConstants.VERSION,
                Constant.SHARED_PREFERENCES_VERSION);

        mAccounts = new ArrayList<AccountDescriptor>();
        updateTwitterAccountCount();

        SocialNetConstant.Type socialNetType = SocialNetConstant.Type.Twitter;
        AccountDescriptor currentAccountDescriptor = getCurrentAccount();
        if (currentAccountDescriptor != null) {
            socialNetType = currentAccountDescriptor.getSocialNetType();
            if (socialNetType == null) {
                socialNetType = SocialNetConstant.Type.Twitter;
            }
            TwitterManager
                    .initModule(
                            socialNetType,
                            socialNetType == SocialNetConstant.Type.Twitter ? ConsumerKeyConstants.TWITTER_CONSUMER_KEY
                                    : ConsumerKeyConstants.APPDOTNET_CONSUMER_KEY,
                            socialNetType == SocialNetConstant.Type.Twitter ? ConsumerKeyConstants.TWITTER_CONSUMER_SECRET
                                    : ConsumerKeyConstants.TWITTER_CONSUMER_SECRET,
                            currentAccountDescriptor.getOAuthToken(),
                            currentAccountDescriptor.getOAuthSecret(),
                            currentAccountDescriptor.getAccountKey(),
                            mConnectionStatusCallbacks);
        } else {
            TwitterManager.initModule(SocialNetConstant.Type.Twitter,
                    ConsumerKeyConstants.TWITTER_CONSUMER_KEY,
                    ConsumerKeyConstants.TWITTER_CONSUMER_SECRET, null, null, null,
                    mConnectionStatusCallbacks);
        }

        setLaneDefinitions(socialNetType);

        AppSettings.initModule(this);
    }

    private void setLaneDefinitions(SocialNetConstant.Type socialNetType) {
        mProfileLaneDefinitions = new ArrayList<LaneDescriptor>();
        mProfileLaneDefinitions
                .add(new LaneDescriptor(Constant.LaneType.PROFILE_PROFILE,
                        this.getString(R.string.lane_profile_profile),
                        new TwitterContentHandleBase(
                                TwitterConstant.ContentType.USER)));
        mProfileLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.PROFILE_PROFILE_TIMELINE, getString(socialNetType == SocialNetConstant.Type.Twitter ? R.string.lane_profile_tweets : R
                .string.lane_profile_tweets_adn),
                new TwitterContentHandleBase(
                        TwitterConstant.ContentType.STATUSES,
                        TwitterConstant.StatusesType.USER_TIMELINE)));
        mProfileLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.PROFILE_MENTIONS, getString(
                R.string.lane_profile_mentions),
                new TwitterContentHandleBase(
                        TwitterConstant.ContentType.STATUSES,
                        TwitterConstant.StatusesType.SCREEN_NAME_SEARCH)));
        mProfileLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.PROFILE_FAVORITES,
                getString(R.string.lane_profile_favorites),
                new TwitterContentHandleBase(
                        TwitterConstant.ContentType.STATUSES,
                        TwitterConstant.StatusesType.USER_FAVORITES)));
        mSearchLaneDefinitions = new ArrayList<LaneDescriptor>();

        mSearchLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.SEARCH_TERM, getString(
                socialNetType == SocialNetConstant.Type.Twitter ? R.string.lane_search_tweets : R.string
                        .lane_search_tweets_adn),
                new TwitterContentHandleBase(
                        TwitterConstant.ContentType.STATUSES,
                        TwitterConstant.StatusesType.STATUS_SEARCH)));
        if (socialNetType == SocialNetConstant.Type.Twitter) {
            mSearchLaneDefinitions.add(new LaneDescriptor(
                    Constant.LaneType.SEARCH_PERSON, getString(
                    R.string.lane_search_people),
                    new TwitterContentHandleBase(
                            TwitterConstant.ContentType.USERS,
                            TwitterConstant.UsersType.PEOPLE_SEARCH)));
        }

        mTweetSpotlightLaneDefinitions = new ArrayList<LaneDescriptor>();
        mTweetSpotlightLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.STATUS_SPOTLIGHT, getString(
                socialNetType == SocialNetConstant.Type.Twitter ? R.string.lane_tweet_status : R.string
                        .lane_tweet_status_adn),
                new TwitterContentHandleBase(
                        TwitterConstant.ContentType.STATUS,
                        TwitterConstant.StatusType.GET_STATUS)));
        mTweetSpotlightLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.STATUS_CONVERSATION, getString(
                R.string.lane_tweet_conversation),
                new TwitterContentHandleBase(
                        TwitterConstant.ContentType.STATUS,
                        TwitterConstant.StatusesType.PREVIOUS_CONVERSATION)));
        mTweetSpotlightLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.STATUS_RETWEETED_BY, getString(
                socialNetType == SocialNetConstant.Type.Twitter ? R.string.lane_tweet_retweeted_by : R.string
                        .lane_tweet_retweeted_by_adn),
                new TwitterContentHandleBase(TwitterConstant.ContentType.USERS,
                        TwitterConstant.UsersType.RETWEETED_BY)));
    }

    /*
	 *
	 */
    private final ConnectionStatus.Callbacks mConnectionStatusCallbacks = new ConnectionStatus.Callbacks() {

        @Override
        public boolean isOnline() {
            return App.this.isOnline();
        }

        @Override
        public String getErrorMessageNoConnection() {
            return App.this.getString(R.string.error_no_connection);
        }

        @Override
        public void handleError(TwitterFetchResult fetchResult) {
            Intent intent = new Intent("" + SystemEvent.DISPLAY_TOAST);
            intent.putExtra("message", fetchResult.getErrorMessage());
            LocalBroadcastManager.getInstance(App.this).sendBroadcast(intent);
        }
    };

    /*
	 *
	 */
    boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();

    }

    /*
     * (non-Javadoc)
     * @see android.app.Application#onTerminate()
     */
    @Override
    public void onTerminate() {

        TwitterManager.deinitModule();

        super.onTerminate();
    }

    /*
	 *
	 */
    public boolean onUserListsRefresh(TwitterLists lists) {

        boolean result = false;

        AccountDescriptor account = getCurrentAccount();
        if (account != null) {
            result = account.updateTwitterLists(lists);
            if (result) {
                this.saveUpdatedAccountDescriptor(account);
            }
        }

        return result;
    }

    /*
	 *
	 */
    public void triggerFollowPromoAccounts(
            TwitterFetchUsers.FinishedCallback callback) {
        ArrayList<Long> userIds = new ArrayList<Long>();
        userIds.add(Constant.USER_ID_CHRISMLACY);
        userIds.add(Constant.USER_ID_TWEETLANES);
        TwitterManager.get().updateFriendshipUserIds(
                getCurrentAccount().getId(), userIds, true, callback);
    }

    /*
	 *
	 */
    public void restartApp(Activity currentActivity) {
        Intent intent = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        currentActivity.overridePendingTransition(0, 0);
        currentActivity.startActivity(intent);
    }

    /*
	 *
	 */
    private LazyImageLoader mProfileImageLoader, mPreviewImageLoader;

    public LazyImageLoader getPreviewImageLoader() {
        if (mPreviewImageLoader == null) {
            AppSettings.MediaImageSize mediaImageSize = AppSettings.get().getCurrentMediaImageSize();

            boolean noScale = false;

            int preview_image_width = getResources()
                    .getDimensionPixelSize(R.dimen.image_preview_width);
            int preview_image_height = getResources()
                    .getDimensionPixelSize(R.dimen.image_preview_height);

            if (mediaImageSize == AppSettings.MediaImageSize.Large) {
                preview_image_height = 0;
                preview_image_width = 0;
                noScale = true;
            }

            mPreviewImageLoader = new LazyImageLoader(this,
                    Constant.DIR_NAME_CACHED_THUMBNAILS, R.drawable.white,
                    preview_image_width, preview_image_height, noScale, 30);
        }
        return mPreviewImageLoader;
    }

    /*
	 *
	 */
    public LazyImageLoader getProfileImageLoader() {
        if (mProfileImageLoader == null) {
            final int profile_image_size = getResources()
                    .getDimensionPixelSize(R.dimen.avatar_width_height_large);
            mProfileImageLoader = new LazyImageLoader(this,
                    Constant.DIR_NAME_PROFILE_IMAGES,
                    R.drawable.ic_contact_picture, profile_image_size,
                    profile_image_size, false, 60);
        }
        return mProfileImageLoader;
    }

    /*
	 *
	 */
    public void clearImageCaches() {
        UrlImageViewHelper.cleanup(this);

        if (mProfileImageLoader != null) {
            mProfileImageLoader.clearCache();
        }

        if (mPreviewImageLoader != null) {
            mPreviewImageLoader.clearCache();
        }
    }

    public void setCrittersismMetaData() {
        if (mAccounts.size() > 1) {
            AccountDescriptor account = mAccounts.get(0);
            if (account != null) {
                // Add extra Crittersism meta data
                try {
                    JSONObject metadata = new JSONObject();
                    metadata.put("FullAccountKey", account.getAccountKey());
                    metadata.put("NumberOfAccounts", mAccounts.size());
                    for (int i = 1; i < mAccounts.size(); i++) {
                        AccountDescriptor otherAccount = mAccounts.get(i);
                        metadata.put("OtherAccount_" + i, otherAccount.getAccountKey());
                    }
                    Crittercism.setMetadata(metadata);
                    Crittercism.setUsername(account.getAccountKey30Chars());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

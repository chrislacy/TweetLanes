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

package com.tweetlanes.android.core.view;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.Notifier;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.SharedPreferencesConstants;
import com.tweetlanes.android.core.model.AccountDescriptor;
import com.tweetlanes.android.core.model.LaneDescriptor;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.widget.viewpagerindicator.TitleProvider;

import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterFetchLists.FinishedCallback;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchUsers;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterLists;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatusUpdate;
import org.tweetalib.android.model.TwitterUser;
import org.tweetalib.android.model.TwitterUsers;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends BaseLaneActivity {

    HomeLaneAdapter mHomeLaneAdapter;
    SpinnerAdapter mSpinnerAdapter;
    ViewSwitcher mViewSwitcher;
    FinishedCallback mFetchListsCallback;
    private OnNavigationListener mOnNavigationListener;
    private Integer mDefaultLaneOverride = null;

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // StrictMode.setThreadPolicy(new
        // StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDialog().build());
        // StrictMode.setThreadPolicy(new
        // StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
        // StrictMode.setVmPolicy(new
        // StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());

        AccountDescriptor account = getApp().getCurrentAccount();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String accountKey = extras.getString("account_key");
            String notificationType = extras.getString("notification_type");
            long postId = extras.getLong("post_id");
            String laneName = extras.getString("lane");

            if (accountKey != null) {
                getIntent().removeExtra("account_key");
                getIntent().removeExtra("notification_type");
                AccountDescriptor notificationAccount = getApp().getAccountByKey(accountKey);

                Notifier.saveLastNotificationActioned(this, accountKey, notificationType, postId);

                Constant.LaneType notificationLaneType = notificationType.equals(SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION) ? Constant.LaneType.USER_MENTIONS : Constant.LaneType.DIRECT_MESSAGES;

                if (notificationAccount != null) {
                    long  notificationAccountId = notificationAccount.getId();
                    long currentAccountId = account.getId();
                    if (notificationAccountId == currentAccountId) {
                        int index = account.getCurrentLaneIndex(notificationLaneType);
                        if (index > -1) {
                            mDefaultLaneOverride = index;
                        }
                    } else {
                        showAccount(notificationAccount, notificationLaneType);
                    }
                }
            } else if (laneName != null) {
                getIntent().removeExtra("lane");
                int index = account.getCurrentLaneIndex(Constant.LaneType.valueOf(laneName.trim().toUpperCase()));
                if (index > -1) {
                    mDefaultLaneOverride = index;
                }
            }
        }

        super.onCreate(savedInstanceState);

        // Attempt at fixing a crash found in HomeActivity
        if (account == null) {
            Toast.makeText(getApplicationContext(),
                    "No cached account found, restarting",
                    Constant.DEFAULT_TOAST_DISPLAY_TIME).show();
            restartApp();
            return;
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setTitle(null);
        actionBar.setDisplayShowTitleEnabled(false);

        mSpinnerAdapter = new AccountAdapter(this, getApp().getAccounts());

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(mSpinnerAdapter,
                mOnNavigationListener);
        actionBar.setSelectedNavigationItem(0);

        onCreateNavigationListener();
        configureListNavigation();

        mViewSwitcher = (ViewSwitcher) findViewById(R.id.rootViewSwitcher);
        updateViewVisibility();

        onCreateHandleIntents();

        account.setDisplayedLaneDefinitionsDirty(false);

        Notifier.setNotificationAlarm(this);
    }

    /*
     *
	 */
    void onCreateHandleIntents() {

        boolean turnSoftKeyboardOff = true;

        Intent intent = getIntent();
        if (intent.getAction() == Intent.ACTION_SEND) {

            Bundle extras = intent.getExtras();
            String type = intent.getType();
            if (type.equals("text/plain") == true) {

                String shareString = extras.getString(Intent.EXTRA_TEXT);
                if (extras.containsKey(Intent.EXTRA_TEXT)) {
                    shareString = extras.getString(Intent.EXTRA_SUBJECT) + " "
                            + shareString;
                }
                beginShareStatus(shareString);

                turnSoftKeyboardOff = false;
            } else if (type.contains("image/")) {
                // From http://stackoverflow.com/a/2641363/328679
                if (extras.containsKey(Intent.EXTRA_STREAM)) {
                    Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                    String scheme = uri.getScheme();
                    if (scheme.equals("content")) {
                        ContentResolver contentResolver = getContentResolver();
                        Cursor cursor = contentResolver.query(uri, null, null,
                                null, null);
                        cursor.moveToFirst();
                        try {
                            String imagePath = cursor.getString(cursor
                                    .getColumnIndexOrThrow(Images.Media.DATA));
                            beginShareImage(imagePath);
                        } catch (java.lang.IllegalArgumentException e) {
                            Toast.makeText(this, R.string.picture_attach_error,
                                    Toast.LENGTH_SHORT).show();
                        }

                        turnSoftKeyboardOff = false;
                    }
                }
            }
        }

        if (turnSoftKeyboardOff == true) {
            // Turn the soft-keyboard off. For some reason it wants to appear on
            // screen by default when coming back from multitasking...
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        AccountDescriptor account = getApp().getCurrentAccount();

        if (account.getDisplayedLaneDefinitionsDirty()) {
            onLaneDataSetChanged();
            account.setDisplayedLaneDefinitionsDirty(false);
        }

        if (account.shouldRefreshLists() == true) {
            mRefreshListsHandler.removeCallbacks(mRefreshListsTask);
            mRefreshListsHandler.postDelayed(mRefreshListsTask,
                    Constant.REFRESH_LISTS_WAIT_TIME);
        }

        if (isComposing() == false) {
            // Check we are using List navigation on the ActionBar. This will
            // not be the case when ComposeTweet is present.
            if (getActionBar().getNavigationMode() == android.app.ActionBar.NAVIGATION_MODE_LIST) {
                /*
                 * Integer accountIndex =
                 * mSpinnerAdapter.getIndexOfAccount(getApp
                 * ().getCurrentAccount()); if (accountIndex != null) {
                 * getActionBar().setSelectedNavigationItem(accountIndex); }
                 */
            }
        }

        getApp().clearImageCaches();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneActivity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        App app = getApp();
        app.saveUpdatedAccountDescriptor(app.getCurrentAccount());

        saveData(getCurrentLaneIndex());

        mRefreshListsHandler.removeCallbacks(mRefreshListsTask);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {

        mHomeLaneAdapter = null;

        super.onDestroy();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneActivity#getInitialLaneIndex()
     */
    @Override
    protected int getInitialLaneIndex() {
        AccountDescriptor account = getApp().getCurrentAccount();

        if (mDefaultLaneOverride != null) {
            int lane = mDefaultLaneOverride.intValue();
            mDefaultLaneOverride = null;
            return lane;
        }

        if (account == null) {
            return 0;
        }

        return account.getInitialLaneIndex();
    }

    /*
	 *
	 */
    @Override
    String getCachedData(int laneIndex) {

        if (Constant.ENABLE_STATUS_CACHING == true) {
            AccountDescriptor account = getApp().getCurrentAccount();


            LaneDescriptor laneDescriptor = account
                    .getDisplayedLaneDefinition(laneIndex);
            if (laneDescriptor != null) {
                // Never cache app.net interactions
                if (account.getSocialNetType() == SocialNetConstant.Type.Appdotnet && laneDescriptor.getLaneType() ==
                        Constant.LaneType.RETWEETS_OF_ME) {
                    return null;
                }

                String cacheKey = laneDescriptor.getCacheKey(account.getScreenName() + account.getId());
                String cachedData = getApp().getCachedData(cacheKey);
                if (cachedData != null) {
                    return cachedData;
                }
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneActivity#getAdapterForViewPager()
     */
    @Override
    protected PagerAdapter getAdapterForViewPager() {
        if (mHomeLaneAdapter == null) {
            mHomeLaneAdapter = new HomeLaneAdapter(getSupportFragmentManager());
        }
        return mHomeLaneAdapter;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneActivity#getFragmentStatePagerAdapter
     * ()
     */
    @Override
    protected FragmentStatePagerAdapter getFragmentStatePagerAdapter() {
        return mHomeLaneAdapter;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneActivity#onLaneChange(int)
     */
    @Override
    protected void onLaneChange(final int position, final int oldPosition) {
        super.onLaneChange(position, oldPosition);

        getApp().getCurrentAccount().setCurrentLaneIndex(position);

        saveData(oldPosition);
    }

    /*
	 *
	 */
    private void saveData(final int position) {
        final App app = getApp();

        new Thread(new Runnable() {

            public void run() {
                if (app != null) {
                    AccountDescriptor account = app.getCurrentAccount();

                    BaseLaneFragment fragment = getFragmentAtIndex(position);
                    final String toCache = fragment != null ? fragment
                            .getDataToCache() : null;

                    app.saveUpdatedAccountDescriptor(account);

                    if (toCache != null) {
                        LaneDescriptor laneDescriptor = account
                                .getDisplayedLaneDefinition(position);
                        final String cacheKey = laneDescriptor != null ? laneDescriptor
                                .getCacheKey(account.getScreenName() + account.getId()) : null;
                        if (cacheKey != null) {
                            app.cacheData(cacheKey, toCache);
                        }
                    }
                }
            }
        }).start();
    }

    /*
	 *
	 */
    private void updateViewVisibility() {
        mViewSwitcher.reset();
        mViewSwitcher.setDisplayedChild(1);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneActivity#configureOptionsMenu(android
     * .view.Menu)
     */
    @Override
    public boolean configureOptionsMenu(Menu menu) {
        super.configureOptionsMenu(menu);

        return configureListNavigation();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneActivity#getDefaultOptionsMenu()
     */
    @Override
    public Integer getDefaultOptionsMenu() {
        return R.menu.home_default_action_bar;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneActivity#onOptionsItemSelected(android
     * .view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (super.onOptionsItemSelected(item) == true) {
            return true;
        }

        int i = item.getItemId();
        if (i == R.id.action_settings) {
            showUserPreferences();

        } else if (i == R.id.action_buy_alp) {
            Intent browserIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.chrislacy.actionlauncher.pro"));
            startActivity(browserIntent);


        /*
         * case R.id.action_promote: showPromote(); break;
         */

        /*
         * case CHOOSE_LANES: Toast.makeText(getApplicationContext(),
         * getString(R.string.functionality_not_implemented),
         * Constant.DEFAULT_TOAST_DISPLAY_TIME).show(); break;
         */

        /*
         * case R.id.action_send_feedback: Intent intent = new
         * Intent(Intent.ACTION_SEND); intent.setType("plain/text"); String
         * recipient[] = {"lacy@tweetlanes.com"};
         * intent.putExtra(Intent.EXTRA_EMAIL, recipient);
         * intent.putExtra(Intent.EXTRA_SUBJECT, "Tweet Lanes Feedback");
         * //String userScreenName = getApp().getCurrentAccountScreenName();
         * //intent.putExtra(Intent.EXTRA_TEXT, userScreenName != null ?
         * "(Feedback sent by @" + userScreenName : ""); startActivity(intent);
         * break;
         */
        } else {
            return false;
        }

        return false;
    }

    /*
	 *
	 */
    void onCreateNavigationListener() {
        mOnNavigationListener = new OnNavigationListener() {

            @Override
            public boolean onNavigationItemSelected(int position, long itemId) {

                if (position == mSpinnerAdapter.getCount() - 1) {
                    showAddAccount();
                } else {
                    ArrayList<AccountDescriptor> accounts = getApp()
                            .getAccounts();
                    if (position < accounts.size()) {
                        AccountDescriptor account = accounts.get(position);
                        showAccount(account, null);
                    }
                }

                return true;
            }
        };
    }

    /*
	 *
	 */
    private boolean configureListNavigation() {

        if (mSpinnerAdapter == null) {
            return false;
        }

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(android.app.ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(mSpinnerAdapter,
                mOnNavigationListener);

        int accountIndex = 0;
        AccountDescriptor currentAccount = getApp().getCurrentAccount();
        if (currentAccount != null) {
            for (int i = 0; i < getApp().getAccounts().size(); i++) {
                if (currentAccount.getAccountKey().equals(getApp().getAccounts().get(i)
                        .getAccountKey())) {
                    accountIndex = i;
                    break;
                }
            }
        }
        actionBar.setSelectedNavigationItem(accountIndex);
        actionBar.setDisplayHomeAsUpEnabled(false);
        return true;
    }

    /*
     *
     */
    private void showAccount(AccountDescriptor selectedAccount, Constant.LaneType lane) {

        App app = getApp();
        AccountDescriptor currentAccount = app.getCurrentAccount();
        TwitterUser cachedUser = TwitterManager.get().getFetchUserInstance().getCachedUser(currentAccount.getId());
        if (cachedUser != null) {
            currentAccount.setProfileImageUrl(cachedUser.getProfileImageUrl(TwitterManager.ProfileImageSize.BIGGER));
        }
        app.saveUpdatedAccountDescriptor(currentAccount);

        saveData(getCurrentLaneIndex());

        if (currentAccount == null
                || currentAccount.getId() != selectedAccount.getId()) {

            saveData(getCurrentLaneIndex());

            clearFragmentsCache();

            app.setCurrentAccount(selectedAccount.getId());

            // From http://stackoverflow.com/a/3419987/328679
            Intent intent = getIntent();
            overridePendingTransition(0, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            if (lane != null) {
                intent.putExtra("lane", lane.toString());
            }
            finish();

            overridePendingTransition(0, 0);
            startActivity(intent);
        }
    }

    /*
	 *
	 */
    private void showAddAccount() {
        startActivity(new Intent(this, NewAccountActivity.class));
    }

    /*
	 *
	 */
    public void showUserPreferences() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    /*
     *
     */
    private void showFreeForLifeSuccess() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

        builder.setMessage(getString(R.string.free_for_life_success));
        builder.setTitle(getString(R.string.success));
        builder.setIcon(android.R.drawable.btn_star_big_on);

        builder.setPositiveButton("OK", new Dialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

            }

        });

        builder.create().show();
    }

    /*
     *
     */
    private void showFreeForLifeError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

        builder.setMessage(getString(R.string.free_for_life_error));
        builder.setIcon(0);

        builder.setPositiveButton("OK", new Dialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

            }

        });
    }

    /*
     *
     */
    public void showPromote() {
        final View layout = View.inflate(this, R.layout.promote, null);

        // final EditText promoStatus = ((EditText)
        // layout.findViewById(R.id.promoStatusEditText));
        // promoStatus.setText(R.string.free_for_life_promo_status);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(0);

        builder.setPositiveButton("Agree!", new Dialog.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                TwitterFetchUsers.FinishedCallback callback = TwitterManager
                        .get().getFetchUsersInstance().new FinishedCallback() {

                    @Override
                    public void finished(TwitterFetchResult result,
                                         TwitterUsers users) {

                        if (result.isSuccessful()) {

                            String statusText = getString(R.string.promote_status_text);
                            TwitterStatusUpdate statusUpdate = new TwitterStatusUpdate(
                                    statusText);

                            TwitterManager
                                    .get()
                                    .setStatus(
                                            statusUpdate,
                                            TwitterManager.get()
                                                    .getFetchStatusInstance().new FinishedCallback() {

                                                @Override
                                                public void finished(
                                                        TwitterFetchResult result,
                                                        TwitterStatus status) {

                                                    if (result.isSuccessful()) {
                                                        showFreeForLifeSuccess();
                                                    } else {
                                                        showFreeForLifeError();
                                                    }
                                                }
                                            });
                        }

                    }
                };

                getApp().triggerFollowPromoAccounts(callback);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.setView(layout);
        builder.setTitle("Promote Tweet Lanes!");

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * A call-back for when the user presses the back button.
     */
    OnClickListener mBackListener = new OnClickListener() {

        public void onClick(View v) {
            finish();
        }
    };

    /*
     * Hanlder for refreshing a user's lists
     */
    private Handler mRefreshListsHandler = new Handler();
    private final Runnable mRefreshListsTask = new Runnable() {

        public void run() {

            mFetchListsCallback = TwitterManager.get().getFetchListsInstance().new FinishedCallback() {

                @Override
                public void finished(boolean successful, TwitterLists lists) {
                    mFetchListsCallback = null;

                    if (successful == true) {
                        boolean modified = getApp().onUserListsRefresh(lists);
                        if (modified == true) {
                            onLaneDataSetChanged();
                        }
                    }
                }
            };

            AccountDescriptor account = getApp().getCurrentAccount();
            if (account != null) {
                TwitterManager.get().getLists(account.getScreenName(), mFetchListsCallback);
            }
        }
    };

    /*
	 *
	 */
    void onLaneDataSetChanged() {
        if (mHomeLaneAdapter != null) {
            mHomeLaneAdapter.notifyDataSetChanged();
        }
        if (mPageIndicator != null) {
            mPageIndicator.notifyDataSetChanged();
        }
        getApp().getCurrentAccount().setDisplayedLaneDefinitionsDirty(false);
    }

    /*
     *
     */
    class HomeLaneAdapter extends FragmentStatePagerAdapter implements
            TitleProvider {

        public HomeLaneAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment result = null;

            AccountDescriptor account = getApp().getCurrentAccount();
            if (account != null) {
                String screenName = account.getScreenName();
                LaneDescriptor laneDescriptor = account
                        .getDisplayedLaneDefinition(position);
                switch (laneDescriptor.getLaneType()) {
                    case USER_HOME_TIMELINE:
                    case USER_PROFILE_TIMELINE:
                    case USER_MENTIONS:
                    case USER_FAVORITES:
                    case GLOBAL_FEED:
                    case RETWEETS_OF_ME:
                        result = TweetFeedFragment.newInstance(position,
                                laneDescriptor.getContentHandleBase(), screenName,
                                Long.toString(account.getId()),
                                getApp().getCurrentAccountKey());
                        break;

                    case USER_PROFILE:
                        result = ProfileFragment.newInstance(position,
                                account.getId());
                        break;

                    case USER_LIST_TIMELINE:
                        result = TweetFeedFragment.newInstance(position,
                                laneDescriptor.getContentHandleBase(), screenName,
                                laneDescriptor.getIdentifier(),
                                getApp().getCurrentAccountKey());
                        break;

                    case FRIENDS:
                    case FOLLOWERS:
                        result = UserFeedFragment.newInstance(position,
                                laneDescriptor.getContentHandleBase(), screenName,
                                null, getApp().getCurrentAccountKey());
                        break;

                    case DIRECT_MESSAGES:
                        result = DirectMessageFeedFragment.newInstance(position,
                                laneDescriptor.getContentHandleBase(), screenName,
                                Long.toString(account.getId()), null, getApp().getCurrentAccountKey());
                        break;

                    default:
                        result = PlaceholderPagerFragment.newInstance(position,
                                laneDescriptor.getLaneTitle(), position);
                        break;
                }
            }
            return result;
        }

        @Override
        public int getCount() {

            AccountDescriptor account = getApp().getCurrentAccount();
            if (account != null) {
                return account.getDisplayedLaneDefinitionsSize();
            }

            return 0;
        }

        @Override
        public String getTitle(int position) {

            String result = null;

            AccountDescriptor account = getApp().getCurrentAccount();
            if (account != null) {
                result = account.getDisplayedLaneDefinition(position)
                        .getLaneTitle().toUpperCase();
            }

            return result;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    class AccountAdapter extends android.widget.BaseAdapter {

        Context mContext;
        List<AccountData> mData;

        public AccountAdapter(Context context, List<AccountDescriptor> data)
        {
            mContext = context;
            mData = new ArrayList<AccountData>();
            if (data != null) {
                for (AccountDescriptor account : data) {
                    mData.add(new AccountData(account.getId(), "@" + account.getScreenName(),
                            account.getSocialNetType(), account.getProfileImageUrl()));
                }
            }

            mData.add(new AccountData(0, getString(R.string.add_account), null, null));
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public AccountData getItem(int i) {
            return mData.get(i);
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).Id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            AccountHolder holder;

            if (row == null)
            {
                LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
                row = inflater.inflate(R.layout.account_row, parent, false);

                holder = new AccountHolder();
                holder.AvatarImage = (ImageView)row.findViewById(R.id.accountAvatar);
                holder.ServiceImage = (ImageView)row.findViewById(R.id.serviceImage);
                holder.ScreenName = (TextView)row.findViewById(R.id.accountScreenName);

                row.setTag(holder);
            }
            else
            {
                holder = (AccountHolder)row.getTag();
            }

            AccountData account = mData.get(position);

            if (account == null)
            {
                return row;
            }

            holder.ScreenName.setText(account.ScreenName, TextView.BufferType.NORMAL);
            setProfileImage(account.AvatarImageUrl, account.ServiceType, holder.AvatarImage, holder.ServiceImage);

            return row;
        }

        class AccountData {
            public AccountData(long id, String screenName, SocialNetConstant.Type serviceType, String avatarImageUrl) {
                Id = id;
                ScreenName = screenName;
                AvatarImageUrl = avatarImageUrl;
                ServiceType = serviceType;
            }

            public String AvatarImageUrl;
            public SocialNetConstant.Type ServiceType;
            public String ScreenName;
            public long Id;
        }

        class AccountHolder {
            public ImageView AvatarImage;
            public ImageView ServiceImage;
            public TextView ScreenName;
        }

        private void setProfileImage(String profileImageUrl, SocialNetConstant.Type serviceType, ImageView avatar, ImageView service) {
            if (profileImageUrl != null) {
                service.setVisibility(View.VISIBLE);
                service.setImageResource(serviceType == SocialNetConstant.Type.Twitter ? R.drawable.twitter_logo_small : R.drawable.adn_logo_small);
                LazyImageLoader profileImageLoader = getApp().getProfileImageLoader();
                if (profileImageLoader != null) {

                    profileImageLoader.displayImage(profileImageUrl, avatar);
                }
            }
            else {
                avatar.setImageResource(R.drawable.ic_action_user_add);
                service.setVisibility(View.GONE);
            }
        }
    }
}

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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.model.ComposeTweetDefault;
import com.tweetlanes.android.core.model.LaneDescriptor;
import com.tweetlanes.android.core.widget.viewpagerindicator.TitleProvider;

import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchStatus;
import org.tweetalib.android.TwitterFetchStatus.FinishedCallback;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterModifyStatuses;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatuses;

public class TweetSpotlightActivity extends BaseLaneActivity {

    private TweetSpotlightAdapter mTweetSpotlightAdapter;
    private ViewSwitcher mViewSwitcher;
    TwitterStatus mStatus;
    private FinishedCallback mGetStatusCallback;
    private MenuItem mFavoriteMenuItem;
    private MenuItem mRetweetMenuItem;

    private final static String STATUS_ID_KEY = "statusId";
    private final static String STATUS_KEY = "status";

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String statusIdAsString = getIntent().getStringExtra(STATUS_ID_KEY);
        String statusAsString = getIntent().getStringExtra(STATUS_KEY);
        String clearCompose = getIntent().getStringExtra("clearCompose");

        long statusId = 0;
        if (statusIdAsString != null) {
            statusId = Long.parseLong(statusIdAsString);
        }

        TwitterStatus status = null;
        if (statusAsString != null) {
            status = new TwitterStatus(statusAsString);
        }

        BaseLaneFragment fragment = super.getFragmentAtIndex(0);
        super.setCurrentComposeFragment((fragment instanceof DirectMessageFeedFragment) ? super.COMPOSE_DIRECT_MESSAGE
                : super.COMPOSE_TWEET);


        if (clearCompose != null && clearCompose.equals("true")) {
            clearCompose();
            getIntent().removeExtra("clearCompose");
        }

        mViewSwitcher = (ViewSwitcher) findViewById(R.id.rootViewSwitcher);
        updateViewVisibility();

        if (status != null && status.mId == statusId) {
            onGetStatus(status);
        } else if (statusId > 0) {
            mGetStatusCallback = TwitterManager.get().getFetchStatusInstance().new FinishedCallback() {

                @Override
                public void finished(TwitterFetchResult result,
                                     TwitterStatus status) {
                    // TODO: handle error properly
                    if (result.isSuccessful() && status != null) {
                        if (mTweetSpotlightAdapter != null) {
                            onGetStatus(status);
                        }
                    } else {
                        Intent returnIntent = new Intent();
                        if (mStatus != null) {
                            returnIntent.putExtra("status", mStatus.toString());
                        } else {
                            returnIntent.putExtra("status", "");
                        }
                        setResult(RESULT_OK, returnIntent);
                        finish();
                    }
                    mGetStatusCallback = null;
                }
            };

            // TODO: Look at using a cached value
            TwitterManager.get().getStatus(statusId, mGetStatusCallback);

        } else {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("status", mStatus.toString());
            setResult(RESULT_OK, returnIntent);
            finish();
        }


    }

    void TweetDeleted(String result) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("status", mStatus.toString());
        returnIntent.putExtra("result", result);
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Intent returnIntent = new Intent();
            if (mStatus != null) {
                returnIntent.putExtra("status", mStatus.toString());
            }
            setResult(RESULT_OK, returnIntent);
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /*
     *
	 */
    @Override
    protected void onDestroy() {

        mTweetSpotlightAdapter = null;

        super.onDestroy();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneActivity#getAdapterForViewPager()
     */
    @Override
    protected PagerAdapter getAdapterForViewPager() {
        if (mTweetSpotlightAdapter == null) {
            mTweetSpotlightAdapter = new TweetSpotlightAdapter(
                    getSupportFragmentManager());
        }
        return mTweetSpotlightAdapter;
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
        return mTweetSpotlightAdapter;
    }

    /*
     *
	 */
    @Override
    protected ComposeTweetDefault getComposeTweetDefault() {
        if (mStatus != null) {
            TwitterStatuses defaultStatuses = new TwitterStatuses(mStatus);
            return new ComposeTweetDefault(getApp()
                    .getCurrentAccountScreenName(), defaultStatuses);
        }
        return super.getComposeTweetDefault();
    }

    /*
     *
	 */
    void onGetStatus(TwitterStatus status) {
        mStatus = new TwitterStatus(status);
        updateViewVisibility();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        invalidateOptionsMenu();
        setComposeDefault();
    }

    /*
     *
	 */
    private void updateViewVisibility() {

        mViewSwitcher.reset();

        if (mStatus == null) {
            mViewSwitcher.setDisplayedChild(0);
        } else {
            mViewSwitcher.setDisplayedChild(1);
            if (mTweetSpotlightAdapter != null) {
                mTweetSpotlightAdapter.notifyDataSetChanged();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        int i = item.getItemId();
        if (i == android.R.id.home) {// app icon in action bar clicked; go home
            Intent returnIntent = new Intent();
            returnIntent.putExtra("status", mStatus.toString());
            setResult(RESULT_OK, returnIntent);
            finish();
            return true;

            /*
             * case R.id.action_reply: beginCompose(); return true;
             */
        } else if (i == R.id.action_retweet) {

            TwitterFetchStatus.FinishedCallback callback = TwitterManager.get()
                    .getFetchStatusInstance().new FinishedCallback() {

                @Override
                public void finished(TwitterFetchResult result, TwitterStatus status) {

                    boolean success = true;

                    if (result != null && result.isSuccessful()) {
                        if (status == null || status.mOriginalRetweetId == 0) {
                            if (result.getErrorMessage() == null) {
                                success = false;
                            } else if (!result.getErrorMessage().equals("CancelPressed") && !result.getErrorMessage().equals("QutotePressed")) {
                                success = false;
                            }
                        }
                    } else {
                        success = false;
                    }

                    if (!success) {
                        showToast(getString(R.string.retweeted_un_successful));
                        mStatus.mIsRetweetedByMe = false;
                        onGetStatus(mStatus);
                        setIsRetweeted();
                    }
                }

            };

            TwitterFetchStatus.FinishedCallback showRTcallback = TwitterManager.get()
                    .getFetchStatusInstance().new FinishedCallback() {

                @Override
                public void finished(TwitterFetchResult result, TwitterStatus status) {
                    mStatus.mIsRetweetedByMe = true;
                    onGetStatus(mStatus);
                    setIsRetweeted();
                }
            };

            if (mStatus.mIsRetweetedByMe) {
                showToast(getString(R.string.cannot_unretweet));
                setIsRetweeted();
            } else {
                boolean isDarkTheme = AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark || AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Light_DarkAction;
                mRetweetMenuItem.setIcon(isDarkTheme ? R.drawable.ic_action_rt_pressed_dark : R.drawable.ic_action_rt_pressed_light);

                retweetSelected(mStatus, callback, showRTcallback);
            }

            return true;
        } else if (i == R.id.action_favorite) {

            //test to see if dark theme and show visual cue when favorite button is pressed
            boolean isDarkTheme = AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark || AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Light_DarkAction;
            mFavoriteMenuItem
                    .setIcon(isDarkTheme ? R.drawable.ic_action_star_pressed_dark
                            : R.drawable.ic_action_star_pressed_light);

            TwitterModifyStatuses.FinishedCallback callback =
                    TwitterManager.get().getSetStatusesInstance().new FinishedCallback() {

                        @Override
                        public void finished(boolean successful, TwitterStatuses statuses, Integer value) {
                            if (!successful) {

                                showToast(getString(mStatus.mIsFavorited ? R.string.favorited_un_successfully : R.string
                                        .unfavorited_un_successfully));

                                mStatus.setFavorite(!mStatus.mIsFavorited);
                                onGetStatus(mStatus);
                                setIsFavorited();
                            }
                        }

                    };

            TwitterManager.get().setFavorite(mStatus, !mStatus.mIsFavorited, callback);

            mStatus.setFavorite(!mStatus.mIsFavorited);
            onGetStatus(mStatus);
            setIsFavorited();

            return true;
        } else {
            return false;
        }
    }

    void showToast(String message) {
        Toast.makeText(getApplicationContext(), message,
                Constant.DEFAULT_TOAST_DISPLAY_TIME).show();
    }

    /**
     * Called when your activity's options menu needs to be created.
     */
    @Override
    public boolean configureOptionsMenu(Menu menu) {

        if (mStatus != null) {

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.tweet_spotlight_action_bar, menu);
            storeMenuItems(menu);
            return true;
        }

        return false;
    }

    void setIsFavorited() {
        if (mFavoriteMenuItem != null) {
            boolean isDarkTheme = AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark || AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Light_DarkAction;
            if (mStatus.mIsFavorited) {
                mFavoriteMenuItem
                        .setIcon(isDarkTheme ? R.drawable.ic_action_star_on_dark
                                : R.drawable.ic_action_star_on_light);
                mFavoriteMenuItem.setTitle(R.string.action_unfavorite);
            } else {
                mFavoriteMenuItem
                        .setIcon(isDarkTheme ? R.drawable.ic_action_star_off_dark
                                : R.drawable.ic_action_star_off_light);
                mFavoriteMenuItem.setTitle(R.string.action_favorite);
            }
        }
    }

    void setIsRetweeted() {
        if (mRetweetMenuItem != null) {
            boolean isDarkTheme = AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark || AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Light_DarkAction;
            if (mStatus.mIsRetweetedByMe) {
                mRetweetMenuItem.setIcon(
                        isDarkTheme ? R.drawable.ic_action_rt_on_dark : R.drawable.ic_action_rt_on_light);
                mRetweetMenuItem.setTitle(R.string.action_retweet_unset);
            } else {
                mRetweetMenuItem.setIcon(
                        isDarkTheme ? R.drawable.ic_action_rt_off_dark : R.drawable.ic_action_rt_off_light);
                mRetweetMenuItem.setTitle(R.string.action_retweet);
            }
        }
    }

    /*
     *
     */
    void storeMenuItems(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            if (menuItem.getItemId() == R.id.action_favorite) {
                mFavoriteMenuItem = menuItem;
                setIsFavorited();
            }
            if (menuItem.getItemId() == R.id.action_retweet) {
                mRetweetMenuItem = menuItem;
                setIsRetweeted();
            }
        }
    }

    /*
     *
     */
    class TweetSpotlightAdapter extends FragmentStatePagerAdapter implements
            TitleProvider {

        public TweetSpotlightAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {

            Fragment result;
            if (mStatus != null) {
                LaneDescriptor laneDescriptor = getApp()
                        .getTweetSpotlightLaneDescriptor(position);

                long statusId = mStatus.mIsRetweet ? mStatus.mOriginalRetweetId : mStatus.mId;

                switch (laneDescriptor.getLaneType()) {
                    case STATUS_SPOTLIGHT:
                        result = TweetSpotlightFragment.newInstance(position,
                                mStatus);
                        break;

                    case STATUS_CONVERSATION:
                        result = TweetFeedFragment.newInstance(position,
                                laneDescriptor.getContentHandleBase(),
                                mStatus.getAuthorScreenName(),
                                String.valueOf(statusId),
                                getApp().getCurrentAccountKey());
                        break;

                    case STATUS_RETWEETED_BY:
                        result = UserFeedFragment.newInstance(position,
                                laneDescriptor.getContentHandleBase(),
                                mStatus.getAuthorScreenName(),
                                String.valueOf(statusId),
                                getApp().getCurrentAccountKey());
                        break;

                    default:
                        result = PlaceholderPagerFragment.newInstance(position,
                                laneDescriptor.getLaneTitle(), position);
                        break;
                }
            } else {
                result = LoadingFragment.newInstance(position);
            }
            return result;
        }

        @Override
        public int getCount() {
            return getApp().getTweetSpotlightLaneDefinitions().size();
        }

        @Override
        public String getTitle(int position) {
            return getApp().getTweetSpotlightLaneDescriptor(position)
                    .getLaneTitle().toUpperCase();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }
}

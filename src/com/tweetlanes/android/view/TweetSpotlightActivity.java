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

package com.tweetlanes.android.view;

import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchStatus.FinishedCallback;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatuses;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ViewSwitcher;

import com.tweetlanes.android.AppSettings;
import com.tweetlanes.android.R;
import com.tweetlanes.android.model.ComposeTweetDefault;
import com.tweetlanes.android.model.LaneDescriptor;
import com.tweetlanes.android.widget.viewpagerindicator.TitleProvider;

public class TweetSpotlightActivity extends BaseLaneActivity {

    public final static String STATUS_ID_KEY = "statusId";
    TweetSpotlightAdapter mTweetSpotlightAdapter;
    ViewSwitcher mViewSwitcher;
    TwitterStatus mStatus;
    FinishedCallback mGetStatusCallback;
    MenuItem mFavoriteMenuItem;

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String statusIdAsString = getIntent().getStringExtra(STATUS_ID_KEY);
        if (statusIdAsString != null) {
            long statusId = Long.parseLong(statusIdAsString);

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
                        finish();
                    }
                    mGetStatusCallback = null;
                }
            };

            // TODO: Look at using a cached value
            TwitterManager.get().getStatus(statusId, mGetStatusCallback);

        } else {
            finish();
            return;
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mViewSwitcher = (ViewSwitcher) findViewById(R.id.rootViewSwitcher);
        updateViewVisibility();
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
     * @see
     * com.tweetlanes.android.view.BaseLaneActivity#getAdapterForViewPager()
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
     * @see
     * com.tweetlanes.android.view.BaseLaneActivity#getFragmentStatePagerAdapter
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
    private void onGetStatus(TwitterStatus status) {
        mStatus = new TwitterStatus(status);
        updateViewVisibility();
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
            mTweetSpotlightAdapter.notifyDataSetChanged();
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (super.onOptionsItemSelected(item) == true) {
            return true;
        }

        switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in action bar clicked; go home
            // TODO: Should this be finish()?
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

            /*
             * case R.id.action_reply: beginCompose(); return true;
             */

        case R.id.action_retweet:
            retweetSelected(mStatus);
            return true;

        case R.id.action_favorite: {

            org.tweetalib.android.TwitterModifyStatuses.FinishedCallback callback = TwitterManager
                    .get().getSetStatusesInstance().new FinishedCallback() {

                @Override
                public void finished(boolean successful,
                        TwitterStatuses statuses, Integer value) {
                    if (successful && mTweetSpotlightAdapter != null) {
                        if (statuses != null && statuses.getStatusCount() > 0) {
                            onGetStatus(statuses.getStatus(0));
                        }
                    }
                }

            };
            TwitterManager.get().setFavorite(mStatus, !mStatus.mIsFavorited,
                    callback);
            return true;
        }

        default:
            return false;
        }
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

    /*
     * 
     */
    void storeMenuItems(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            if (menuItem.getItemId() == R.id.action_favorite) {
                mFavoriteMenuItem = menuItem;
                boolean isDarkTheme = AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark;
                if (mStatus.mIsFavorited == true) {
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
                break;
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

            Fragment result = null;
            if (mStatus != null) {
                LaneDescriptor laneDescriptor = getApp()
                        .getTweetSpotlightLaneDescriptor(position);

                switch (laneDescriptor.getLaneType()) {
                case STATUS_SPOTLIGHT:
                    result = TweetSpotlightFragment.newInstance(position,
                            mStatus);
                    break;

                case STATUS_CONVERSATION:
                    result = TweetFeedFragment.newInstance(position,
                            laneDescriptor.getContentHandleBase(),
                            mStatus.getAuthorScreenName(),
                            String.valueOf(mStatus.mId));
                    break;

                case STATUS_RETWEETED_BY:
                    result = UserFeedFragment.newInstance(position,
                            laneDescriptor.getContentHandleBase(),
                            mStatus.getAuthorScreenName(),
                            String.valueOf(mStatus.mId));
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

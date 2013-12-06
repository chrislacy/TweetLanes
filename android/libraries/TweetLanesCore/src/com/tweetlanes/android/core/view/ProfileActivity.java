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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.model.ComposeTweetDefault;
import com.tweetlanes.android.core.model.LaneDescriptor;
import com.tweetlanes.android.core.widget.viewpagerindicator.TitleProvider;

import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchUser;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterUser;

public class ProfileActivity extends BaseLaneActivity {

    private ProfileAdapter mProfileAdapter;
    private ViewSwitcher mViewSwitcher;
    private TwitterUser mUser;
    private String mScreenName;

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);

        String clearCompose = getIntent().getStringExtra("clearCompose");
        boolean savedStateRecreate = false;
        if (savedInstanceState != null && savedInstanceState.containsKey("Recreate")) {
            savedStateRecreate = savedInstanceState.getBoolean("Recreate");
        }

        mScreenName = getIntent().getStringExtra("userScreenName");
        if (mScreenName == null) {
            Uri data = getIntent().getData();
            mScreenName = data.toString()
                    .replace("com.tweetlanes.android.core.profile://", "")
                    .replace("@", "");
        }


        if (mScreenName == null) {
            restartApp();
            return;
        }

        BaseLaneFragment fragment = super.getFragmentAtIndex(0);
        super.setCurrentComposeFragment((fragment instanceof DirectMessageFeedFragment) ? super.COMPOSE_DIRECT_MESSAGE
                : super.COMPOSE_TWEET);

        if ((clearCompose != null && clearCompose.equals("true")) && !savedStateRecreate) {
            clearCompose();
            getIntent().removeExtra("clearCompose");
        }

        TwitterFetchUser.FinishedCallback callback = TwitterManager.get()
                .getFetchUserInstance().new FinishedCallback() {

            public void finished(TwitterFetchResult result, TwitterUser user) {
                if (result.isSuccessful()) {
                    mUser = user;
                    updateViewVisibility();
                } else {
                    // TODO: Handle this properly
                    finish();
                }
            }

        };

        boolean requestedUser = false;
        String profileIdAsString = getIntent().getStringExtra("userId");

        Long mappedProfileId = TwitterManager
                .getUserIdFromScreenName(mScreenName);
        if (mappedProfileId != null) {
            mUser = TwitterManager.get().getUser(mappedProfileId, callback);
            requestedUser = true;
        } else if (profileIdAsString != null) {
            long profileId = Long.parseLong(profileIdAsString);
            if (profileId > 0) {
                mUser = TwitterManager.get().getUser(profileId, callback);
                requestedUser = true;
            }
        }
        if (!requestedUser) {
            mUser = TwitterManager.get().getUser(mScreenName, callback);
        }

        mViewSwitcher = (ViewSwitcher) findViewById(R.id.rootViewSwitcher);
        updateViewVisibility();

        setComposeDefault();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("Recreate", true);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        // By clearing this variable and checking whether it's valid in
        // updateViewVisibility(), an exception is averted whe nthe Activity is
        // destroyed, yet a callback is still initiated from a Twitter fetch
        // operation. A better solution is needed here, but this works for now.
        mProfileAdapter = null;

        super.onDestroy();
    }

    /*
     *
	 */
    @Override
    protected ComposeTweetDefault getComposeTweetDefault() {
        return new ComposeTweetDefault(getApp().getCurrentAccountScreenName(),
                "@" + mScreenName + " ", null, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneActivity#getAdapterForViewPager()
     */
    @Override
    protected PagerAdapter getAdapterForViewPager() {
        if (mProfileAdapter == null) {
            mProfileAdapter = new ProfileAdapter(getSupportFragmentManager());
        }
        return mProfileAdapter;
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
        return mProfileAdapter;
    }

    /*
     *
	 */
    private void updateViewVisibility() {

        configureActionBarView();

        mViewSwitcher.reset();

        if (mUser == null) {
            mViewSwitcher.setDisplayedChild(0);
        } else {
            mViewSwitcher.setDisplayedChild(1);
            // Will be NULL if the callback was called after the Activity was
            // released
            if (mProfileAdapter != null) {
                mProfileAdapter.notifyDataSetChanged();
            }
        }
    }

    /*
     *
	 */
    @Override
    public boolean configureOptionsMenu(Menu menu) {
        boolean result = super.configureOptionsMenu(menu);

        configureActionBarView();

        return result;
    }

    /*
	 *
	 */
    void configureActionBarView() {

        if (mScreenName != null) {

            final ActionBar actionBar = getActionBar();
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle("@" + mScreenName);

            final LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final int layout = R.layout.profile_title_thin;
            /*
             * // TODO: This is messy, and likely won't work for large screen
             * devices. Need to come up with a better solution int layout; if
             * (getResources().getConfiguration().orientation ==
             * Configuration.ORIENTATION_LANDSCAPE) { layout=
             * R.layout.profile_title_thin; } else { layout =
             * R.layout.profile_title; }
             */

            final View abView = inflator.inflate(layout, null);
            final ImageView verified = (ImageView) abView.findViewById(R.id.verifiedImage);
            verified.setVisibility(mUser != null && mUser.getVerified() ? View.VISIBLE : View.GONE);

            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(abView);
        }
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

        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        clearCompose();
    }

    /*
	 *
	 */
    class ProfileAdapter extends FragmentStatePagerAdapter implements
            TitleProvider {

        public ProfileAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {

            Fragment result;
            if (mUser != null) {
                LaneDescriptor laneDescriptor = getApp()
                        .getProfileLaneDescriptor(position);

                switch (laneDescriptor.getLaneType()) {
                    case PROFILE_PROFILE:
                        result = ProfileFragment.newInstance(position,
                                mUser.getId());
                        break;

                    case PROFILE_PROFILE_TIMELINE:
                    case PROFILE_MENTIONS:
                    case PROFILE_FAVORITES:
                        result = TweetFeedFragment
                                .newInstance(position,
                                        laneDescriptor.getContentHandleBase(),
                                        mUser.getScreenName(),
                                        Long.toString(mUser.getId()),
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
            return getApp().getProfileLaneDefinitions().size();
        }

        @Override
        public String getTitle(int position) {
            return getApp().getProfileLaneDescriptor(position).getLaneTitle()
                    .toUpperCase();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

}

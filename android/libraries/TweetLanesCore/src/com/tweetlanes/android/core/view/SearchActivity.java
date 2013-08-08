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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.MenuItem;
import android.widget.ViewSwitcher;

import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.model.LaneDescriptor;
import com.tweetlanes.android.core.widget.viewpagerindicator.TitleProvider;

public class SearchActivity extends BaseLaneActivity {

    private String mSearchTerm;
    private SearchAdapter mSearchAdapter;
    private ViewSwitcher mViewSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mSearchTerm = getIntent().getStringExtra("query");
        if (mSearchTerm == null) {
            Uri data = getIntent().getData();
            if (data != null) {
                mSearchTerm = data.toString().replace(
                        "com.tweetlanes.android.core.search://", "");
            }
        }

        super.onCreate(savedInstanceState);

        // This could be true if loading back into the app from Multitasking
        if (mSearchTerm == null) {
            restartApp();
            return;
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(getString(R.string.action_bar_search_title) + " \""
                + mSearchTerm + "\"");
        actionBar.setDisplayHomeAsUpEnabled(true);

        mViewSwitcher = (ViewSwitcher) findViewById(R.id.rootViewSwitcher);
        updateViewVisibility();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneActivity#getAdapterForViewPager()
     */
    @Override
    protected PagerAdapter getAdapterForViewPager() {

        if (mSearchTerm == null) {
            return null;
        }

        if (mSearchAdapter == null) {
            mSearchAdapter = new SearchAdapter(getSupportFragmentManager());
        }
        return mSearchAdapter;
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
        return mSearchAdapter;
    }

    /*
     *
	 */
    private void updateViewVisibility() {

        mViewSwitcher.reset();
        mViewSwitcher.setDisplayedChild(1);
    }

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

    /*
     *
     */
    class SearchAdapter extends FragmentStatePagerAdapter implements
            TitleProvider {

        public SearchAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {

            Fragment result;
            LaneDescriptor laneDescriptor = getApp().getSearchLaneDescriptor(
                    position);
            switch (laneDescriptor.getLaneType()) {
                case SEARCH_TERM:
                    result = TweetFeedFragment.newInstance(position,
                            laneDescriptor.getContentHandleBase(), mSearchTerm,
                            null, getApp().getCurrentAccountKey());
                    break;

                case SEARCH_PERSON:
                    result = UserFeedFragment.newInstance(position,
                            laneDescriptor.getContentHandleBase(), mSearchTerm,
                            null, getApp().getCurrentAccountKey());
                    break;

                default:
                    result = PlaceholderPagerFragment.newInstance(position,
                            laneDescriptor.getLaneTitle(), position);
                    break;
            }
            return result;
        }

        @Override
        public int getCount() {
            return getApp().getSearchLaneDefinitions().size();
        }

        @Override
        public String getTitle(int position) {
            return getApp().getSearchLaneDescriptor(position).getLaneTitle()
                    .toUpperCase();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.widget.SearchView.OnQueryTextListener#onQueryTextSubmit(java.
     * lang.String)
     */
    @Override
    public boolean onQueryTextSubmit(String query) {

        if (query != null) {
            mSearchTerm = query;
            getIntent().putExtra("query", query);
            getActionBar().setTitle(mSearchTerm);
            mSearchAdapter.notifyDataSetChanged();

            return true;
        }
        return false;
    }

}

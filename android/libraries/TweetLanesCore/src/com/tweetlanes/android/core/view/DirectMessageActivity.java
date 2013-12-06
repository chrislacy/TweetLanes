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
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.widget.viewpagerindicator.TitleProvider;

import org.json.JSONArray;
import org.tweetalib.android.TwitterContentHandle;
import org.tweetalib.android.model.TwitterDirectMessage;

import java.util.ArrayList;

public class DirectMessageActivity extends BaseLaneActivity {

    private DirectMessageLaneAdapter mDirectMessageLaneAdapter;
    private boolean mDeleting = false;
    private boolean mHasDoneDelete = false;

    private static final String KEY_HANDLE_BASE = "handleBase";
    private static final String KEY_OTHER_USER_ID = "otherUserId";
    private static final String KEY_OTHER_USER_SCREEN_NAME = "otherUserScreenName";
    private static final String KEY_CACHE_MESSAGES = "cacheMessages";

    /*
     *
	 */
    public static void createAndStartActivity(Activity currentActivity,
                                              TwitterContentHandle contentHandle, long otherUserId,
                                              String otherUserScreenName, ArrayList<TwitterDirectMessage> requiredMessages) {

        Intent intent = new Intent(currentActivity,
                DirectMessageActivity.class);
        intent.putExtra(KEY_HANDLE_BASE, contentHandle);
        intent.putExtra(KEY_OTHER_USER_ID, otherUserId);
        intent.putExtra(KEY_OTHER_USER_SCREEN_NAME, otherUserScreenName);
        JSONArray statusArray = new JSONArray();
        int statusCount = requiredMessages.size();
        for (int i = 0; i < statusCount; ++i) {
            TwitterDirectMessage status = requiredMessages.get(i);
            statusArray.put(status.toString());
        }
        intent.putExtra(KEY_CACHE_MESSAGES, statusArray.toString());
        currentActivity.startActivityForResult(intent, Constant.REQUEST_CODE_DM);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configureActionBarView();

        ViewSwitcher viewSwitcher = (ViewSwitcher) findViewById(R.id.rootViewSwitcher);
        viewSwitcher.reset();
        viewSwitcher.setDisplayedChild(1);

        setDirectMessageOtherUserScreenName(getOtherUserScreenName());
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

        switch (item.getItemId()) {
            case android.R.id.home:

                if (mDeleting) {
                    showNoBackToast();
                    return false;
                }

                Intent returnIntent = new Intent();
                returnIntent.putExtra("statusDelete", mHasDoneDelete);
                setResult(RESULT_OK, returnIntent);
                finish();
                return true;

            default:
                return false;
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

    void configureActionBarView() {

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(getString(R.string.dm_title)
                + getOtherUserScreenName());
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    /*
     *
	 */
    TwitterContentHandle getContentHandle() {
        return (TwitterContentHandle) getIntent().getSerializableExtra(
                KEY_HANDLE_BASE);
    }

    long getOtherUserId() {
        return getIntent().getLongExtra(KEY_OTHER_USER_ID, -1);
    }

    String getOtherUserScreenName() {
        return getIntent().getStringExtra(KEY_OTHER_USER_SCREEN_NAME);
    }

    String getCachedMessages() {
        return getIntent().getStringExtra(KEY_CACHE_MESSAGES);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneActivity#getDefaultOptionsMenu()
     */
    @Override
    public Integer getDefaultOptionsMenu() {
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
        if (mDirectMessageLaneAdapter == null) {
            mDirectMessageLaneAdapter = new DirectMessageLaneAdapter(
                    getSupportFragmentManager());
        }
        return mDirectMessageLaneAdapter;
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
        return mDirectMessageLaneAdapter;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (mDeleting) {
                showNoBackToast();
                return false;
            }

            if (event.getRepeatCount() == 0) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("statusDelete", mHasDoneDelete);
                setResult(RESULT_OK, returnIntent);
                finish();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showNoBackToast() {
        Toast.makeText(getApplicationContext(), R.string.delete_dm_noback,
                Constant.DEFAULT_TOAST_DISPLAY_TIME).show();
    }

    public void setDeleting(boolean newDeletingValue) {
        mDeleting = newDeletingValue;
        if (mDeleting == true && mHasDoneDelete == false) {
            mHasDoneDelete = mDeleting;
        }
    }

    /*
     *
	 */
    class DirectMessageLaneAdapter extends FragmentStatePagerAdapter implements
            TitleProvider {

        public DirectMessageLaneAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        public ArrayList<DirectMessageFeedFragment> directMessageFeedFragments = new ArrayList<DirectMessageFeedFragment>();

        @Override
        public Fragment getItem(int position) {
            TwitterContentHandle contentHandle = getContentHandle();
            DirectMessageFeedFragment fragment = DirectMessageFeedFragment.newInstance(position,
                    contentHandle, getApp().getCurrentAccountScreenName(), getApp().getCurrentAccountName(),
                    contentHandle.getIdentifier(), getOtherUserId(), getApp().getCurrentAccountKey(), getCachedMessages());

            directMessageFeedFragments.add(fragment);
            return fragment;
        }

        @Override
        public int getCount() {

            return 1;
        }

        @Override
        public String getTitle(int position) {

            return "Title";
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }
}

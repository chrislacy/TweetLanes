/*
 * Copyright (C) 2013 Chris Lacy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tweetlanes.android.view;

import org.tweetalib.android.TwitterContentHandle;

import com.tweetlanes.android.App;
import com.tweetlanes.android.R;
import com.tweetlanes.android.widget.viewpagerindicator.TitleProvider;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ViewSwitcher;

public class DirectMessageActivity extends BaseLaneActivity {

	DirectMessageLaneAdapter mDirectMessageLaneAdapter;
	ViewSwitcher mViewSwitcher;
	
	static final String KEY_HANDLE_BASE = "handleBase";
	static final String KEY_OTHER_USER_ID = "otherUserId";
	static final String KEY_OTHER_USER_SCREEN_NAME = "otherUserScreenName";
	
	/*
	 * 
	 */
	public static void createAndStartActivity(Activity currentActivity, TwitterContentHandle contentHandle, 
			long otherUserId, String otherUserScreenName) {
		Intent intent = new Intent(App.getContext(), DirectMessageActivity.class);
		intent.putExtra(KEY_HANDLE_BASE, contentHandle);
		intent.putExtra(KEY_OTHER_USER_ID, otherUserId);
		intent.putExtra(KEY_OTHER_USER_SCREEN_NAME, otherUserScreenName);
		currentActivity.startActivity(intent);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		configureActionBarView();

		mViewSwitcher = (ViewSwitcher) findViewById(R.id.rootViewSwitcher);
		mViewSwitcher.reset();
		mViewSwitcher.setDisplayedChild(1);
		
		setDirectMessageOtherUserScreenName(getOtherUserScreenName());
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
        super.configureOptionsMenu(menu);

        return configureActionBarView();
    }
	
	/*
	 * 
	 */
	boolean configureActionBarView() {
	
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(getString(R.string.dm_title) + getOtherUserScreenName());
		actionBar.setDisplayUseLogoEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		return true;
	}

	/*
	 * 
	 */
	TwitterContentHandle getContentHandle() { return (TwitterContentHandle) getIntent().getSerializableExtra(KEY_HANDLE_BASE); }
	long 	getOtherUserId() 			{ return getIntent().getLongExtra(KEY_OTHER_USER_ID, -1); }
	String 	getOtherUserScreenName() 		{ return getIntent().getStringExtra(KEY_OTHER_USER_SCREEN_NAME); }
	
	/*
	 * (non-Javadoc)
	 * @see com.tweetlanes.android.view.BaseLaneActivity#getDefaultOptionsMenu()
	 */
	@Override
	public Integer getDefaultOptionsMenu() {
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.tweetlanes.android.view.BaseLaneActivity#getAdapterForViewPager()
	 */
	@Override
	protected PagerAdapter getAdapterForViewPager() {
		if (mDirectMessageLaneAdapter == null) {
			mDirectMessageLaneAdapter = new DirectMessageLaneAdapter(getSupportFragmentManager());
		}
		return mDirectMessageLaneAdapter; 
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.tweetlanes.android.view.BaseLaneActivity#getFragmentStatePagerAdapter()
	 */
	@Override
	protected FragmentStatePagerAdapter getFragmentStatePagerAdapter() {
		return mDirectMessageLaneAdapter;
	}
	
	/*
	 * 
	 */
	class DirectMessageLaneAdapter extends FragmentStatePagerAdapter implements TitleProvider {
		public DirectMessageLaneAdapter(FragmentManager supportFragmentManager) {
			super(supportFragmentManager);
		}

		@Override
		public Fragment getItem(int position) {
			TwitterContentHandle contentHandle = getContentHandle();
			return DirectMessageFeedFragment.newInstance(position, contentHandle, 
					contentHandle.getScreenName(), contentHandle.getIdentifier(), getOtherUserId());
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

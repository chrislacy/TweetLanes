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

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;

import com.crittercism.app.Crittercism;
import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.ConsumerKeyConstants;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.widget.viewpagerindicator.UnderlinePageIndicator;

public class TutorialActivity extends FragmentActivity {

    private ViewPager mViewPager;
    private TutorialLaneAdapter mLaneAdapter;
    private boolean mDoFollow = true;

    private static final int PAGE_WELCOME = 0;
    private static final int PAGE_COMPOSE_TWEET = 1;
    private static final int PAGE_MULTIPLE_SELECTION = 2;
    private static final int PAGE_VOLSCROLL = 3;
    private static final int PAGE_SPAM_CONTROL = 4;
    private static final int PAGE_THANKS = 5;
    private static final int PAGE_MAX = 6;

    /*
     *
	 */
    App getApp() {
        return (App) getApplication();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Constant.ENABLE_CRASH_TRACKING) {
            Crittercism.initialize(getApplicationContext(),
                    ConsumerKeyConstants.CRITTERCISM_APP_ID);
        }

        // Key the screen from dimming -
        // http://stackoverflow.com/a/4197370/328679
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (mLaneAdapter == null) {
            mLaneAdapter = new TutorialLaneAdapter(getSupportFragmentManager());
        }

        if (mLaneAdapter != null) {
            setContentView(R.layout.tutorial);

            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setAdapter(mLaneAdapter);

            UnderlinePageIndicator indicator = (UnderlinePageIndicator) findViewById(R.id.indicator);
            indicator.setViewPager(mViewPager);
            indicator.setFades(false);
            indicator.setOnPageChangeListener(mOnPageChangeListener);
        }
    }

    /*
     *
	 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        int currentItem = mViewPager.getCurrentItem();
        if (currentItem != PAGE_WELCOME) {
            menu.add(getString(R.string.action_previous))
                    .setOnMenuItemClickListener(
                            new MenuItem.OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    mViewPager.setCurrentItem(mViewPager
                                            .getCurrentItem() - 1);
                                    return true;
                                }

                            })
                    .setShowAsAction(
                            MenuItem.SHOW_AS_ACTION_ALWAYS
                                    | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }

        if (currentItem != PAGE_THANKS) {
            menu.add(getString(R.string.action_next))
                    .setOnMenuItemClickListener(
                            new MenuItem.OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    mViewPager.setCurrentItem(mViewPager
                                            .getCurrentItem() + 1);
                                    return true;
                                }

                            })
                    .setShowAsAction(
                            MenuItem.SHOW_AS_ACTION_ALWAYS
                                    | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }

        return true;
    }

    /*
     *
	 */
    private final OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageSelected(int laneIndex) {

            Resources res = getResources();
            String[] titles = res.getStringArray(R.array.tutorial_titles);
            String title = titles[laneIndex];
            if (laneIndex == PAGE_THANKS) {
                String screenName = getApp().getCurrentAccountScreenName();
                if (screenName != null) {
                    title = "@" + screenName + " "
                            + getString(R.string.tutorial_thanks_title);
                }
            }

            getActionBar().setTitle(title);

            invalidateOptionsMenu();
        }
    };

    /*
	 * 
	 */
    public void onFinishTutorialClicked(View view) {
        // We don't want to come back here, so remove from the activity stack
        finish();
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(intent);

        if (mDoFollow) {
            getApp().triggerFollowPromoAccounts(null);
        }

        getApp().setTutorialCompleted();
    }

    /*
	 * 
	 */
    public void onFollowCheckboxClicked(View view) {
        mDoFollow = ((CheckBox) (view)).isChecked();
    }

    /*
     * 
     */
    class TutorialLaneAdapter extends FragmentPagerAdapter {

        public TutorialLaneAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {

                case PAGE_WELCOME:
                    return InflatedLayoutFragment
                            .newInstance(R.layout.tutorial_welcome);

                // case PAGE_IMAGE_PREVIEW:
                // return
                // InflatedLayoutFragment.newInstance(R.layout.tutorial_images);

                case PAGE_VOLSCROLL:
                    return InflatedLayoutFragment
                            .newInstance(R.layout.tutorial_volscroll);

                case PAGE_COMPOSE_TWEET:
                    return InflatedLayoutFragment
                            .newInstance(R.layout.tutorial_compose_tweet);

                case PAGE_MULTIPLE_SELECTION:
                    return InflatedLayoutFragment
                            .newInstance(R.layout.tutorial_multiple_selection);

                case PAGE_SPAM_CONTROL:
                    return InflatedLayoutFragment
                            .newInstance(R.layout.tutorial_spam_control);

                case PAGE_THANKS:
                    return InflatedLayoutFragment.newInstance(
                            R.layout.tutorial_thanks,
                            new InflatedLayoutFragment.Callback() {

                                @Override
                                public void onCreateView(View view) {
                                    TextView textView = (TextView) view
                                            .findViewById(R.id.acceptTermsTextView);
                                    if (textView != null) {
                                        textView.setMovementMethod(LinkMovementMethod
                                                .getInstance());
                                    }
                                }
                            });

                default:
                    return InflatedLayoutFragment
                            .newInstance(R.layout.tutorial_welcome);
            }
        }

        @Override
        public int getCount() {
            return PAGE_MAX;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

}
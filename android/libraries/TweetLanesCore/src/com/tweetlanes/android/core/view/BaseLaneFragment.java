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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.R;

import org.tweetalib.android.model.TwitterStatus;

public abstract class BaseLaneFragment extends Fragment {

    public enum InitialDownloadState {
        NOT_SET, WAITING, DOWNLOADING, DOWNLOADED,
    }

    private InitialDownloadState mInitialDownloadState = InitialDownloadState.NOT_SET;

    private BaseLaneActivity mBaseLaneActivity;

    /*
     *
	 */
    BaseLaneActivity getBaseLaneActivity() {
        if (getActivity() == null) {
            return mBaseLaneActivity;
        }

        mBaseLaneActivity = (BaseLaneActivity) getActivity();
        return mBaseLaneActivity;
    }

    /*
     *
	 */
    App getApp() {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            return (App) fragmentActivity.getApplication();
        } else {
            BaseLaneActivity baseLaneActivity = getBaseLaneActivity();
            if (baseLaneActivity != null) {
                return baseLaneActivity.getApp();
            }
        }

        return null;
    }

    private static final String KEY_LANE_INDEX = "blf_laneIndex";
    private static final String KEY_IDENTIFIER = "blf_identifier";

    /*
     *
	 */
    public boolean configureOptionsMenu(MenuInflater inflater, Menu menu) {
        return false;
    }

    /*
	 * 
	 */
    public int getLaneIndex() {
        return getArguments().getInt(KEY_LANE_INDEX);
    }

    public void fetchNewestTweets() {
    }

    /*
	 * 
	 */
    boolean isCurrentLaneIndex() {
        BaseLaneActivity baseLaneActivity = getBaseLaneActivity();
        if (baseLaneActivity != null) {
            if (baseLaneActivity.getCurrentLaneIndex() == getLaneIndex()) {
                return true;
            }
        }

        return false;
    }

    /*
     * If we are the current lane, add this offset to async requests so they get
     * dealt with first
     */
    int getAsyncTaskPriorityOffset() {

        if (isCurrentLaneIndex()) {
            return 1;
        }

        return 0;
    }

    /*
	 * 
	 */
    public interface ConfigureBundleListener {

        public void addValues(Bundle args);
    }

    /*
	 * 
	 */
    void configureBaseLaneFragment(int laneIndex, String identifier,
                                   ConfigureBundleListener configureBundleListener) {

        Bundle arguments = new Bundle();

        arguments.putString(KEY_IDENTIFIER, identifier);
        arguments.putInt(KEY_LANE_INDEX, laneIndex);

        if (configureBundleListener != null) {
            configureBundleListener.addValues(arguments);
        }

        setArguments(arguments);
    }

    /*
	 * 
	 */
    public InitialDownloadState getInitialDownloadState() {
        return mInitialDownloadState;
    }

    /*
	 * 
	 */
    void setInitialDownloadState(InitialDownloadState initialLoadState) {
        mInitialDownloadState = initialLoadState;
        getBaseLaneActivity().onLaneFragmentInitialDownloadStateChange(this);
    }

    /*
	 * 
	 */
    void configureLaneWidth(View resultView) {

        RelativeLayout laneContent = (RelativeLayout) resultView
                .findViewById(R.id.lane_content);
        if (laneContent != null) {
            RelativeLayout tweetFeedLoaded = (RelativeLayout) resultView
                    .findViewById(R.id.tweet_feed_loaded);
            if (tweetFeedLoaded != null) {
                if (!AppSettings.get().showTabletMargin()) {
                    android.view.ViewGroup.LayoutParams parentParams = tweetFeedLoaded
                            .getLayoutParams();
                    laneContent
                            .setLayoutParams(new RelativeLayout.LayoutParams(
                                    parentParams.width, parentParams.height));
                } else {
                    /*
                     * final Resources res = getResources(); float width =
                     * res.getDimension(R.dimen.lane_content_width);
                     * RelativeLayout.LayoutParams params = new
                     * RelativeLayout.LayoutParams((int) width,
                     * RelativeLayout.LayoutParams.FILL_PARENT);
                     * params.addRule(RelativeLayout.CENTER_IN_PARENT, -1);
                     * laneContent.setLayoutParams(params);
                     */
                }
            }
        }
    }

    /*
	 * 
	 */
    public abstract void triggerInitialDownload();

    /*
	 *
	 */
    public abstract void UpdateTweetCache(TwitterStatus status, boolean deleteStatus);

    /*
	 * 
	 */
    public abstract void onJumpToTop();

    /*
     * Clear in memory lists & data belonging to the fragment
     */
    public abstract void clearLocalCache();

    /*
	 * 
	 */
    public abstract String getDataToCache();

    /*
	 * 
	 */
    String getCachedData() {
        return getBaseLaneActivity().getCachedData(getLaneIndex());
    }

    /*
	 * 
	 */
    void showToast(String message) {
        FragmentActivity activity = getActivity();
        if (activity != null && activity.getApplicationContext() != null) {
            Toast.makeText(activity.getApplicationContext(), message,
                    Constant.DEFAULT_TOAST_DISPLAY_TIME).show();
        }
    }
}

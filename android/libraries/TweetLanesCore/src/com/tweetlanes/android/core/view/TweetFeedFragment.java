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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.Constant.SystemEvent;
import com.tweetlanes.android.core.Notifier;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.SharedPreferencesConstants;
import com.tweetlanes.android.core.model.AccountDescriptor;
import com.tweetlanes.android.core.model.ComposeTweetDefault;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshBase.OnLastItemVisibleListener;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterConstant;
import org.tweetalib.android.TwitterConstant.StatusesType;
import org.tweetalib.android.TwitterContentHandle;
import org.tweetalib.android.TwitterContentHandleBase;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchStatus;
import org.tweetalib.android.TwitterFetchUsers;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterModifyStatuses;
import org.tweetalib.android.TwitterPaging;
import org.tweetalib.android.callback.TwitterFetchStatusesFinishedCallback;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatuses;
import org.tweetalib.android.model.TwitterUsers;

import java.util.ArrayList;

public final class TweetFeedFragment extends BaseLaneFragment {

    public static TweetFeedFragment newInstance(int laneIndex, final TwitterContentHandleBase handleBase,
                                                final String screenName, final String laneIdentifier, final String currentAccountKey) {

        TweetFeedFragment fragment = new TweetFeedFragment();

        fragment.mContentHandle =
                TwitterManager.get().getContentHandle(handleBase, screenName, laneIdentifier, currentAccountKey);

        fragment.configureBaseLaneFragment(laneIndex, fragment.mContentHandle.getTypeAsString(),
                new ConfigureBundleListener() {

                    @Override
                    public void addValues(Bundle args) {
                        // TODO: serializing is a slow way of doing this...
                        args.putSerializable(KEY_HANDLE_BASE, handleBase);
                        args.putString(KEY_SCREEN_NAME, screenName);
                        args.putString(KEY_LANE_IDENTIFIER, laneIdentifier);
                    }

                });

        return fragment;
    }

    private PullToRefreshListView mTweetFeedListView;
    private TweetFeedListAdapter mTweetFeedListAdapter;
    private TextView mListHeadingTextView;
    private ImageView mListHeadingHideImage;
    boolean mHidingListHeading = false;
    private TwitterContentHandle mContentHandle;
    private TwitterStatuses _mStatusFeed; // Don't touch me directly. Use the
    // accessors
    private TwitterStatuses _mCachedStatusFeed;
    private TwitterFetchStatusesFinishedCallback mTweetDataRefreshCallback;
    private TwitterFetchStatusesFinishedCallback mTweetDataLoadMoreCallback;
    private ViewSwitcher mViewSwitcher;
    private ArrayList<TweetFeedItemView> mSelectedItems = new ArrayList<TweetFeedItemView>();
    private ArrayList<Long> mConverstaionViewIds = new ArrayList<Long>();
    private MultipleTweetSelectionCallback mMultipleTweetSelectionCallback;

    private Long mNewestTweetId;
    private Long mOldestTweetId;
    private Long mRefreshingOldestTweetId;
    private boolean mMoreStatusesAvailable = true;
    private Long mResumeStatusId;

    private LazyImageLoader mProfileImageLoader;
    private LazyImageLoader mPreviewImageLoader;

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mProfileImageLoader = getApp().getProfileImageLoader();
        mPreviewImageLoader = getApp().getPreviewImageLoader();

        mContentHandle = TwitterManager.get()
                .getContentHandle(getContentHandleBase(), getScreenName(), getLaneIdentifier(),
                        getApp().getCurrentAccountKey());

        View resultView = inflater.inflate(R.layout.lane, null);
        configureLaneWidth(resultView);

        mViewSwitcher = (ViewSwitcher) resultView.findViewById(R.id.profileSwitcher);
        mListHeadingTextView = (TextView) resultView.findViewById(R.id.list_heading);
        mListHeadingHideImage = (ImageView) resultView.findViewById(R.id.list_hide);
        mListHeadingHideImage.setOnClickListener(mListHeadingHideImageOnClickListener);
        mMultipleTweetSelectionCallback = new MultipleTweetSelectionCallback();
        mTweetFeedListAdapter = new TweetFeedListAdapter(inflater);

        mTweetFeedListView = (PullToRefreshListView) resultView.findViewById(R.id.pull_to_refresh_listview);
        mTweetFeedListView.getRefreshableView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mTweetFeedListView.getRefreshableView().setMultiChoiceModeListener(mMultipleTweetSelectionCallback);
        mTweetFeedListView.getRefreshableView().setOnScrollListener(mTweetFeedOnScrollListener);
        mTweetFeedListView.getRefreshableView().setAdapter(mTweetFeedListAdapter);
        mTweetFeedListView.setOnRefreshListener(mTweetFeedOnRefreshListener);
        mTweetFeedListView.setOnLastItemVisibleListener(mTweetFeedOnLastItemVisibleListener);

        configureInitialStatuses();

        mRefreshTimestampsHandler.removeCallbacks(mRefreshTimestampsTask);
        mRefreshTimestampsHandler.postDelayed(mRefreshTimestampsTask, REFRESH_TIMESTAMPS_INTERVAL);

        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mVolumeUpKeyDownReceiver, new IntentFilter("" + SystemEvent.VOLUME_UP_KEY_DOWN));
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mVolumeDownKeyDownReceiver, new IntentFilter("" + SystemEvent.VOLUME_DOWN_KEY_DOWN));

        return resultView;
    }

    /*
     *
	 */
    private void configureInitialStatuses() {
        boolean autoUpdateStatuses = false;

        boolean configuredCachedStatuses = configureCachedStatuses();

        TwitterStatuses cachedFeed = TwitterManager.get().getContentFeed(mContentHandle);
        if (cachedFeed != null && cachedFeed.getStatusCount(getBaseLaneActivity().mStatusesFilter) > 0) {
            setStatusFeed(cachedFeed, true);
        } else {
            if (configuredCachedStatuses) {
                autoUpdateStatuses = true;
            } else {
                setStatusFeed(null, true);
            }
        }

        if (getStatusFeed() == null || getFilteredStatusCount() == 0) {
            updateViewVisibility(false);
            setInitialDownloadState(InitialDownloadState.WAITING);
        } else {
            if (autoUpdateStatuses == true && mTweetDataRefreshCallback == null) {
                fetchNewestTweets();
            }

            setInitialDownloadState(InitialDownloadState.DOWNLOADED);
            updateViewVisibility(true);
        }
    }

    /*
     *
	 */
    void fetchNewestTweets() {
        if (mNewestTweetId != null) {
            fetchNewestTweets(mNewestTweetId.longValue(), null);
        }
    }

    /*
	 *
	 */
    void fetchNewestTweets(final long sinceStatusId, Long maxStatusId) {
        if (mTweetDataRefreshCallback == null) {
            mTweetDataRefreshCallback = new TwitterFetchStatusesFinishedCallback() {

                @Override
                public void finished(TwitterFetchResult fetchResult, TwitterStatuses feed,
                                     TwitterContentHandle contentHandle) {
                    if (!contentHandle.getCurrentAccountKey().equals(getApp().getCurrentAccountKey())) {
                        Log.e("Statuses", "account changed, don't display statuses");
                        return;
                    }

                    beginListHeadingCount();

                    onRefreshFinished(fetchResult, feed);
                    mTweetDataRefreshCallback = null;

                    if (fetchResult.isSuccessful()) {
                        // If there are more statuses to get, go get 'em
                        if (feed != null && feed.getNewStatusesMaxId() != null) {
                            fetchNewestTweets(sinceStatusId, feed.getNewStatusesMaxId());
                            // Log.d("Statuses", "Fetching more");
                        } else {
                            // Log.d("Statuses", "DONE!!!");
                        }
                    }
                }
            };

            Log.d("api-call", "--fetchNewestTweets(" + mContentHandle.getStatusesType().toString() + ")");
            TwitterPaging paging = new TwitterPaging(null, null, sinceStatusId, maxStatusId);
            TwitterManager.get().triggerFetchStatuses(mContentHandle, paging, mTweetDataRefreshCallback,
                    getAsyncTaskPriorityOffset());
            if (getBaseLaneActivity().isComposing() == false &&
                    !(mSelectedItems == null || mSelectedItems.size() == 0)) {
                getBaseLaneActivity().finishCurrentActionMode();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#UpdateTweetCache()
     */
    @Override
    public void UpdateTweetCache(TwitterStatus status, boolean deleteStatus) {
        TwitterStatuses statusFeed = getStatusFeed();
        if(statusFeed != null)
        {
            TwitterStatus cachedStatus = statusFeed.findByStatusId(status.mId);
            if (cachedStatus != null)
            {
                if(deleteStatus)
                {
                    TwitterStatuses selectedStatuses = new TwitterStatuses(cachedStatus);
                    statusFeed.remove(selectedStatuses);
                    _mCachedStatusFeed.remove(selectedStatuses);
                }
                else
                {
                    cachedStatus.setFavorite(status.mIsFavorited);
                    cachedStatus.setRetweet(status.mIsRetweetedByMe);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#onJumpToTop()
     */
    @Override
    public void onJumpToTop() {
        if (mTweetFeedListView != null) {
            ListView listView = mTweetFeedListView.getRefreshableView();
            if (listView != null && listView.getAdapter() != null && listView.getAdapter().isEmpty() == false) {
                listView.setSelection(0);
            }
        }
    }

    private static final String KEY_VISIBLE_STATUS_ID = "visibleStatusId";
    private static final String KEY_STATUSES = "statuses";

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#clearLocalCache()
     */
    @Override
    public void clearLocalCache() {
        _mStatusFeed = null;
        _mCachedStatusFeed = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#getContentToCache()
     */
    @Override
    public String getDataToCache() {

        TwitterStatuses feed = getStatusFeed();
        if (feed == null || feed.getStatusCount() == 0) {
            return null;
        }

        if (mTweetFeedListAdapter != null && mTweetFeedListView != null) {
            ListView listView = mTweetFeedListView.getRefreshableView();
            int visibleIndex = Math.max(listView.getFirstVisiblePosition() - 1, 0);

            // View view = (View)listView.getItemAtPosition(visible);

            int startIndex = Math.max(0, visibleIndex - 5);
            // int endIndex = Math.min(visibleIndex + 5,
            // Math.min(getStatusFeed().getStatusCount(),
            // mTweetFeedListAdapter.getCount()));
            int endIndex = Math.min(visibleIndex + 5, feed.getStatusCount());

            Long visibleStatusId = null;

            TwitterStatuses statuses = new TwitterStatuses();
            for (int i = startIndex; i < endIndex; i++) {
                TwitterStatus status = feed.getStatus(i);
                if (status == null) {
                    break;
                }

                if (i == visibleIndex) {
                    visibleStatusId = status.mId;
                    // Log.d("StatusCache", "Set visible: " +
                    // status.getStatus());
                }

                statuses.add(status);
            }

            if (statuses.getStatusCount() > 0 && visibleStatusId != null) {
                JSONObject object = new JSONObject();
                try {
                    object.put(KEY_VISIBLE_STATUS_ID, visibleStatusId);
                    JSONArray statusArray = new JSONArray();
                    int statusCount = statuses.getStatusCount();
                    for (int i = 0; i < statusCount; ++i) {
                        TwitterStatus status = statuses.getStatus(i);
                        statusArray.put(status.toString());
                    }
                    object.put(KEY_STATUSES, statusArray);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return object.toString();
            }
        }

        return null;
    }

    /*
	 *
	 */
    boolean configureCachedStatuses() {

        String cachedData = getCachedData();
        if (cachedData != null) {
            JSONObject object;
            try {
                object = new JSONObject(cachedData);
                if (object.has(KEY_VISIBLE_STATUS_ID)) {
                    mResumeStatusId = object.getLong(KEY_VISIBLE_STATUS_ID);
                    if (object.has(KEY_STATUSES)) {
                        String statusesAsString = object.getString(KEY_STATUSES);
                        if (statusesAsString != null) {
                            _mCachedStatusFeed = new TwitterStatuses();
                            JSONArray jsonArray = new JSONArray(statusesAsString);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                String statusString = jsonArray.getString(i);
                                TwitterStatus status = new TwitterStatus(statusString);
                                _mCachedStatusFeed.add(status);
                            }

                            setStatusFeed(_mCachedStatusFeed, false);
                            return true;
                        }
                    }
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneFragment#triggerInitialDownload()
     */
    public void triggerInitialDownload() {
        mTweetDataRefreshCallback = new TwitterFetchStatusesFinishedCallback() {

            @Override
            public void finished(TwitterFetchResult fetchResult, TwitterStatuses feed, TwitterContentHandle handle) {
                if (!handle.getCurrentAccountKey().equals(getApp().getCurrentAccountKey())) {
                    Log.e("Statuses", "account changed, don't display statuses");
                    return;
                }


                if (feed != null) {
                    setStatusFeed(feed, true);
                }
                updateViewVisibility(true);
                setInitialDownloadState(InitialDownloadState.DOWNLOADED);
                mTweetDataRefreshCallback = null;
            }
        };

        Log.d("api-call", "--triggerInitialDownload((" + mContentHandle.getStatusesType().toString() + ")");
        TwitterManager.get()
                .triggerFetchStatuses(mContentHandle, null, mTweetDataRefreshCallback, getAsyncTaskPriorityOffset());
        setInitialDownloadState(InitialDownloadState.DOWNLOADING);
    }

    /*
	 *
	 */
    private BroadcastReceiver mVolumeUpKeyDownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (AppSettings.get().isVolScrollEnabled() == true) {
                ListView listView = mTweetFeedListView.getRefreshableView();
                int firstPos = listView.getFirstVisiblePosition();
                int nextPos = firstPos - 1;
                // Greater than 0 so we ignore the pullToRefresh
                if (nextPos > 0) {
                    listView.setSelection(nextPos);
                    // listView.smoothScrollToPosition(nextPos);
                    listView.setScrollbarFadingEnabled(false);
                }
            }
        }
    };

    /*
	 *
	 */
    private BroadcastReceiver mVolumeDownKeyDownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (AppSettings.get().isVolScrollEnabled() == true) {
                ListView listView = mTweetFeedListView.getRefreshableView();

                int nextPos = Math.max(1, listView.getFirstVisiblePosition()) + 1;
                int last = listView.getLastVisiblePosition();
                if (nextPos < last) {
                    listView.setSelection(nextPos);
                    // listView.smoothScrollToPosition(nextPos);
                    listView.setScrollbarFadingEnabled(false);
                }
            }
        }
    };

    /*
	 *
	 */
    enum ScrollDirection {
        UNKNOWN,
        TO_NEWER,
        TO_OLDER,
    }

    /*
	 *
	 */
    private class ScrollTracker {

        private ScrollDirection mLastScrollDirection = ScrollDirection.UNKNOWN;
        private int mFirstVisibleYOffset = 0;
        private int mOldFirstVisibleYOffset = 0;
        private int mOldFirstVisibleItem = 0;
        private int mOldTotalItemCount = 0;

        ScrollTracker() {
            mLastScrollDirection = ScrollDirection.UNKNOWN;
            mFirstVisibleYOffset = 0;
            mOldFirstVisibleYOffset = 0;
            mOldFirstVisibleItem = 0;
            mOldTotalItemCount = 0;
        }

        void update(int firstVisibleItem, int totalItemCount, Integer yOffset) {

            if (yOffset != null) {
                mOldFirstVisibleYOffset = mFirstVisibleYOffset;
                mFirstVisibleYOffset = yOffset;
            }

            if (mOldTotalItemCount != totalItemCount) {
                // mLastScrollDirection = ScrollDirection.UNKNOWN;
            } else {
                if (mOldFirstVisibleItem == firstVisibleItem) {
                    if (mOldFirstVisibleYOffset > mFirstVisibleYOffset) {
                        mLastScrollDirection = ScrollDirection.TO_OLDER;
                    } else if (mOldFirstVisibleYOffset < mFirstVisibleYOffset) {
                        mLastScrollDirection = ScrollDirection.TO_NEWER;
                    }
                } else {
                    if (mOldFirstVisibleItem > firstVisibleItem) {
                        mLastScrollDirection = ScrollDirection.TO_NEWER;
                    } else if (mOldFirstVisibleItem < firstVisibleItem) {
                        mLastScrollDirection = ScrollDirection.TO_OLDER;
                    }
                }
            }

            mOldTotalItemCount = totalItemCount;
            mOldFirstVisibleItem = firstVisibleItem;

            // Log.d("Statuses", "Direction: " +
            // mLastScrollDirection.toString());
        }

        ScrollDirection getLastScrollDirection() {
            return mLastScrollDirection;
        }

        int getFirstVisibleYOffset() {
            return mFirstVisibleYOffset;
        }
    }

    private ScrollTracker mScrollTracker = new ScrollTracker();

    /*
	 *
	 */
    private OnScrollListener mTweetFeedOnScrollListener = new OnScrollListener() {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            Integer yOffset = null;
            if (view != null && view.getChildAt(0) != null) {
                yOffset = view.getChildAt(0).getTop();
            }

            if (firstVisibleItem == 1 && view != null && view.getChildAt(firstVisibleItem - 1) != null) {
                int previousTop = view.getChildAt(firstVisibleItem - 1).getTop();
                int previousBottom = view.getChildAt(firstVisibleItem - 1).getBottom();
                if (previousBottom > 0 && previousTop >= -10)
                {
                    firstVisibleItem--;
                }
            }

            mTweetFeedListView.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
            mScrollTracker.update(firstVisibleItem, totalItemCount, yOffset);

            updateListHeading(firstVisibleItem);

            getVisibleStatus();
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

            if (scrollState == 0) {
                // Restore scrollbar fading, which may have been set to false
                // when scrolling through items via the volume keys
                view.setScrollbarFadingEnabled(true);
            }

            mTweetFeedListView.onScrollStateChanged(view, scrollState);
        }

    };

    /*
	 *
	 */
    private OnLastItemVisibleListener mTweetFeedOnLastItemVisibleListener = new OnLastItemVisibleListener() {

        @Override
        public void onLastItemVisible() {
            if (mOldestTweetId != null) {
                if (mRefreshingOldestTweetId == null) {
                    if (mTweetDataLoadMoreCallback == null) {
                        mTweetDataLoadMoreCallback = new TwitterFetchStatusesFinishedCallback() {

                            @Override
                            public void finished(TwitterFetchResult result, TwitterStatuses feed,
                                                 TwitterContentHandle handle) {
                                if (!handle.getCurrentAccountKey().equals(getApp().getCurrentAccountKey())) {
                                    Log.e("Statuses", "account changed, don't display statuses");
                                    return;
                                }

                                if (feed != null) {
                                    setStatusFeed(feed, true);
                                }

                                if (mRefreshingOldestTweetId != null && mOldestTweetId != null &&
                                        mRefreshingOldestTweetId.longValue() == mOldestTweetId.longValue()) {
                                    mMoreStatusesAvailable = false;
                                }
                                mRefreshingOldestTweetId = null;
                                mTweetDataLoadMoreCallback = null;

                                if (mTweetFeedListAdapter != null) {
                                    mTweetFeedListAdapter.notifyDataSetChanged();
                                }
                            }
                        };

                        Log.d("api-call",
                                "--OnLastItemVisibleListener((" + mContentHandle.getStatusesType().toString() + ")");
                        TwitterManager.get()
                                .triggerFetchStatuses(mContentHandle, TwitterPaging.createGetOlder(mOldestTweetId),
                                        mTweetDataLoadMoreCallback, getAsyncTaskPriorityOffset());
                        mRefreshingOldestTweetId = mOldestTweetId;
                    }
                }
            }
        }
    };

    Long mTwitterStatusIdWhenRefreshed;
    Long mLastTwitterStatusIdSeen;
    int mCurrentFirstVisibleItem = 0;
    int mNewStatuses = 0;

    /*
	 *
	 */
    void beginListHeadingCount() {
        if (mTwitterStatusIdWhenRefreshed == null) {
            if (getStatusFeed() != null && mCurrentFirstVisibleItem < getStatusFeed().getStatusCount()) {
                TwitterStatus status = getStatusFeed().getStatus(mCurrentFirstVisibleItem);
                TwitterStatus visibleStatus = getVisibleStatus();
                mTwitterStatusIdWhenRefreshed = status.mId;
                mLastTwitterStatusIdSeen = visibleStatus.mId;
                mHidingListHeading = false;
            }
        }
    }

    /*
	 *
	 */ OnClickListener mListHeadingHideImageOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            mHidingListHeading = true;
            setListHeadingVisiblilty(View.GONE);
        }
    };

    /*
	 *
	 */
    void setListHeadingVisiblilty(int value) {
        mListHeadingTextView.setVisibility(value);
        mListHeadingHideImage.setVisibility(value);
    }

    /*
	 *
	 */
    void updateListHeading(int firstVisibleItem) {

        SocialNetConstant.Type socialNetType = getApp().getCurrentAccount().getSocialNetType();

        if (mTwitterStatusIdWhenRefreshed != null && firstVisibleItem > 0) {
            if (mHidingListHeading == false) {
                TwitterStatus status = getStatusFeed().getStatus(firstVisibleItem);
                if((mNewStatuses == 0 || status.mId >= mTwitterStatusIdWhenRefreshed) && status.mId >= mLastTwitterStatusIdSeen)
                {
                    mNewStatuses = firstVisibleItem;
                    mLastTwitterStatusIdSeen = status.mId;
                }
                else{
                    setListHeadingVisiblilty(View.VISIBLE);
                    mListHeadingTextView.setText(mNewStatuses + " " + getString(mNewStatuses == 1 ?
                            socialNetType == SocialNetConstant.Type.Twitter ? R.string.new_tweet : R.string.new_post :
                            socialNetType == SocialNetConstant.Type.Twitter ? R.string.new_tweets :
                                    R.string.new_posts));
                }
            }
        } else {
            setListHeadingVisiblilty(View.GONE);
            mTwitterStatusIdWhenRefreshed = null;
        }

        mCurrentFirstVisibleItem = firstVisibleItem;
    }

    /*
	 *
	 */
    private TwitterStatus getVisibleStatus() {

        TwitterStatus visibleStatus = null;

        if (getStatusFeed() != null && mTweetFeedListView != null && mTweetFeedListView.getRefreshableView() != null) {
            int visiblePosition = mTweetFeedListView.getRefreshableView().getFirstVisiblePosition();

            if (visiblePosition < getStatusFeed().getStatusCount()) {
                visibleStatus = getStatusFeed().getStatus(visiblePosition);
                if (visibleStatus != null) {
                    String notifcationType = null;
                    String pref = null;
                    if (getLaneIndex() == getApp().getCurrentAccount().getCurrentLaneIndex(Constant.LaneType.USER_MENTIONS)) {

                        notifcationType = SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION;
                        pref = SharedPreferencesConstants.NOTIFICATION_LAST_DISPLAYED_MENTION_ID;
                    }else if (getLaneIndex() == getApp().getCurrentAccount().getCurrentLaneIndex(Constant.LaneType.DIRECT_MESSAGES)) {

                        notifcationType = SharedPreferencesConstants.NOTIFICATION_TYPE_DIRECT_MESSAGE;
                        pref = SharedPreferencesConstants.NOTIFICATION_LAST_DISPLAYED_DIRECT_MESSAGE_ID;
                    }

                    if (notifcationType != null && pref != null )
                    {

                            Notifier.saveLastNotificationActioned(getBaseLaneActivity(),
                                    getApp().getCurrentAccountKey(), notifcationType, visibleStatus.mId);
                            Notifier.cancel(getBaseLaneActivity(), getApp().getCurrentAccountKey(), notifcationType);

                    }
                }
            }
        }

        return visibleStatus;
    }

    /*
	 *
	 */
    private void onRefreshFinished(TwitterFetchResult result, TwitterStatuses feed) {

        if (mTweetFeedListView == null || mTweetFeedListView.getRefreshableView() == null) {
            return;
        }

        TwitterStatus visibleStatus = getVisibleStatus();

        int oldFeedCount = getFilteredStatusCount();

        if (feed != null) {
            setStatusFeed(feed, true);
        }

        int newFeedCount = getFilteredStatusCount();
        mTweetFeedListView.onRefreshComplete();
        mTweetFeedListAdapter.notifyDataSetChanged();

        int newCount = newFeedCount - oldFeedCount;
        if (newCount > 0) {
            if (visibleStatus != null) {
                Integer statusIndex = getStatusFeed().getStatusIndex(visibleStatus.mId);
                if (statusIndex != null) {
                    mTweetFeedListView.getRefreshableView()
                            .setSelectionFromTop(statusIndex.intValue(), mScrollTracker.getFirstVisibleYOffset());

                    int total = getStatusFeed().getStatusCount();
                    int newStatuses = 0;

                    for (int i = 0; i< total; i++)
                    {
                        TwitterStatus status =  getStatusFeed().getStatus(i);
                        if(status!= null && status.mId > visibleStatus.mId)
                        {
                            newStatuses++;
                        }
                    }

                    mNewStatuses = newStatuses;
                    mLastTwitterStatusIdSeen = visibleStatus.mId;
                }
            }
        }
        mTweetDataRefreshCallback = null;
    }

    /*
	 *
	 */
    private OnRefreshListener mTweetFeedOnRefreshListener = new OnRefreshListener() {

        @Override
        public void onRefresh() {
            fetchNewestTweets();
        }
    };

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem
     * )
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.action_replies_visibility) {
            getBaseLaneActivity().mStatusesFilter
                    .setShowReplies(!getBaseLaneActivity().mStatusesFilter.getShowReplies());
            item.setTitle(getString(
                    getBaseLaneActivity().mStatusesFilter.getShowReplies() ? R.string.action_hide_replies :
                            R.string.action_show_replies));
            mTweetFeedListAdapter.notifyDataSetChanged();
            return false;
        } else if (i == R.id.action_retweets_visibility) {
            getBaseLaneActivity().mStatusesFilter
                    .setShowRetweets(!getBaseLaneActivity().mStatusesFilter.getShowRetweets());
            item.setTitle(getString(
                    getBaseLaneActivity().mStatusesFilter.getShowRetweets() ? R.string.action_hide_retweets :
                            R.string.action_show_retweets));
            mTweetFeedListAdapter.notifyDataSetChanged();
            return false;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneFragment#configureOptionsMenu(android
     * .view.Menu)
     */
    @Override
    public boolean configureOptionsMenu(MenuInflater inflater, Menu menu) {

        if (inflater != null && mContentHandle != null &&
                (mContentHandle.getStatusesType() == StatusesType.USER_TIMELINE ||
                        mContentHandle.getStatusesType() == StatusesType.USER_HOME_TIMELINE ||
                        mContentHandle.getStatusesType() == StatusesType.USER_LIST_TIMELINE)) {

            if (getBaseLaneActivity() instanceof HomeActivity) {
                inflater.inflate(R.menu.home_tweet_feed_action_bar, menu);
            } else {
                inflater.inflate(R.menu.tweet_feed_action_bar, menu);
            }

            for (int i = 0; i < menu.size(); i++) {
                MenuItem menuItem = menu.getItem(i);
                if (menuItem.getItemId() == R.id.action_feed_filter) {
                    SubMenu subMenu = menuItem.getSubMenu();
                    if (subMenu != null) {
                        SocialNetConstant.Type socialNetType = getApp().getCurrentAccount().getSocialNetType();
                        int subMenuSize = subMenu.size();
                        for (int j = 0; j < subMenuSize; j++) {
                            MenuItem subMenuItem = subMenu.getItem(j);
                            int i1 = subMenuItem.getItemId();
                            if (i1 == R.id.action_replies_visibility) {
                                subMenuItem.setTitle(getString(getBaseLaneActivity().mStatusesFilter.getShowReplies() ?
                                        R.string.action_hide_replies : R.string.action_show_replies));

                            } else if (i1 == R.id.action_retweets_visibility) {
                                subMenuItem.setTitle(getString(getBaseLaneActivity().mStatusesFilter.getShowRetweets() ?
                                        socialNetType == SocialNetConstant.Type.Twitter ?
                                                R.string.action_hide_retweets : R.string.action_hide_retweets_adn :
                                        socialNetType == SocialNetConstant.Type.Twitter ?
                                                R.string.action_show_retweets : R.string.action_show_retweets_adn));

                            } else {
                            }

                        }
                    }
                }
            }

            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mVolumeUpKeyDownReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mVolumeDownKeyDownReceiver);

        super.onDestroy();
    }

    /*
	 *
	 */
    private void updateViewVisibility(boolean loadHasFinished) {

        mViewSwitcher.reset();

        if (loadHasFinished == false && (getStatusFeed() == null || getFilteredStatusCount() == 0)) {
            mViewSwitcher.setDisplayedChild(0);
        } else {
            mViewSwitcher.setDisplayedChild(1);
            mTweetFeedListAdapter.notifyDataSetChanged();
            if (mResumeStatusId != null && getStatusFeed() != null) {
                Integer statusIndex = getStatusFeed().getStatusIndex(mResumeStatusId);
                if (statusIndex != null) {
                    mTweetFeedListView.getRefreshableView().setSelectionFromTop(statusIndex.intValue() + 1, 0);
                }
            }
        }
    }

    private static final String KEY_HANDLE_BASE = "handleBase";
    private static final String KEY_SCREEN_NAME = "screenName";
    private static final String KEY_LANE_IDENTIFIER = "laneIdentifier";

    private TwitterContentHandleBase getContentHandleBase() {
        return (TwitterContentHandleBase) getArguments().getSerializable(KEY_HANDLE_BASE);
    }

    private String getScreenName() {
        return getArguments().getString(KEY_SCREEN_NAME);
    }

    private String getLaneIdentifier() {
        return getArguments().getString(KEY_LANE_IDENTIFIER);
    }

    private void setStatusFeed(TwitterStatuses statuses, boolean addCachedStatuses) {

        mNewestTweetId = null;
        mOldestTweetId = null;

        if (statuses == null) {
            _mStatusFeed = null;
        } else {
            _mStatusFeed = new TwitterStatuses(statuses);
        }

        if (addCachedStatuses && _mCachedStatusFeed != null && _mCachedStatusFeed.getStatusCount() > 0) {
            _mStatusFeed.insert(_mCachedStatusFeed);
        }

        if (_mStatusFeed != null && _mStatusFeed.getStatusCount() > 0) {
            mNewestTweetId = _mStatusFeed.getStatus(0).mId;
            mOldestTweetId = _mStatusFeed.getStatus(_mStatusFeed.getStatusCount() - 1).mId;
        }
    }

    private TwitterStatuses getStatusFeed() {
        return _mStatusFeed;
    }

    private int getFilteredStatusCount() {
        return getStatusFeed() != null ? getStatusFeed().getStatusCount(getBaseLaneActivity().mStatusesFilter) : 0;
    }

    private final int REFRESH_TIMESTAMPS_INTERVAL = 1000 * 30;
    private Handler mRefreshTimestampsHandler = new Handler();
    private final Runnable mRefreshTimestampsTask = new Runnable() {

        public void run() {

            if (getFilteredStatusCount() > 0) {
                mTweetFeedListAdapter.notifyDataSetChanged();
            }

            mRefreshTimestampsHandler.postDelayed(this, REFRESH_TIMESTAMPS_INTERVAL);
        }
    };

    /*
	 *
	 */
    private boolean onTweetFeedItemSingleTap(View view, int position) {

        if (mSelectedItems.size() == 0) {
            TweetFeedItemView tweetFeedItemView = (TweetFeedItemView) (view);
            TwitterStatus status = tweetFeedItemView.getTwitterStatus();
            Intent tweetSpotlightIntent = new Intent(getActivity(), TweetSpotlightActivity.class);
            tweetSpotlightIntent.putExtra("statusId", Long.toString(status.mId));
            tweetSpotlightIntent.putExtra("status", status.toString());
            getActivity().startActivityForResult(tweetSpotlightIntent, Constant.REQUEST_CODE_SPOTLIGHT );
            return true;
        } else {
            onTweetFeedItemLongPress(view, position);
            return true;
        }

    }

    /*
	 *
	 */
    private void onTweetFeedItemLongPress(View view, int position) {

        boolean isChecked =
                mTweetFeedListView.getRefreshableView().getCheckedItemPositions().get(position) == true ? true : false;
        mTweetFeedListView.getRefreshableView().setItemChecked(position, !isChecked);

        TweetFeedItemView tweetFeedItemView = (TweetFeedItemView) (view);

        for (int index = 0; index < mSelectedItems.size(); index++) {
            TweetFeedItemView item = mSelectedItems.get(index);
            if (item.getTwitterStatus() != null && tweetFeedItemView.getTwitterStatus() != null) {
                if (item.getTwitterStatus().mId == tweetFeedItemView.getTwitterStatus().mId) {
                    mSelectedItems.remove(index);
                    break;
                }
            }
        }

        if (!isChecked) {
            mSelectedItems.add(tweetFeedItemView);
        }

        if (mSelectedItems.size() > 0 && getApp() != null) {
            mMultipleTweetSelectionCallback
                    .setIsFavorited(getSelectedFavoriteState() == ItemSelectedState.ALL ? true : false);
            TwitterStatus firstItem = getFirstSelectedStatus();
            if (firstItem != null)
            {
                mMultipleTweetSelectionCallback.setIsRetweet(firstItem.mIsRetweetedByMe);
            }
            getBaseLaneActivity().setComposeTweetDefault(
                    new ComposeTweetDefault(getApp().getCurrentAccountScreenName(), getSelectedStatuses()));
        } else {
            mMultipleTweetSelectionCallback.setIsFavorited(false);
            mMultipleTweetSelectionCallback.setIsRetweet(false);
            getBaseLaneActivity().setComposeTweetDefault();
        }
    }

    /*
	 *
	 */
    private TwitterStatuses getSelectedStatuses() {

        TwitterStatuses selectedList = new TwitterStatuses();

        for (int i = 0; i < mSelectedItems.size(); i++) {
            TweetFeedItemView tweetFeedItemView = mSelectedItems.get(i);
            TwitterStatus status = tweetFeedItemView.getTwitterStatus();
            if (status != null) {
                selectedList.add(status);
            }
        }

        return selectedList.getStatusCount() > 0 ? selectedList : null;
    }

    /*
	 *
	 */
    private TwitterStatus getFirstSelectedStatus() {

        for (int i = 0; i < mSelectedItems.size(); i++) {
            TweetFeedItemView tweetFeedItemView = mSelectedItems.get(i);
            TwitterStatus status = tweetFeedItemView.getTwitterStatus();
            if (status != null) {
                return status;
            }
        }

        return null;
    }

    /*
	 *
	 */
    enum ItemSelectedState {
        NONE,
        SOME,
        ALL
    }

    /*
	 *
	 */
    private ItemSelectedState getSelectedFavoriteState() {

        int favoriteCount = 0;
        int selectedCount = mSelectedItems.size();
        for (int i = 0; i < selectedCount; i++) {
            TwitterStatus status = mSelectedItems.get(i).getTwitterStatus();
            if (status != null) {
                if (status.mIsFavorited) {
                    favoriteCount++;
                }
            }
        }

        if (favoriteCount == selectedCount) {
            return ItemSelectedState.ALL;
        } else if (favoriteCount != 0) {
            return ItemSelectedState.SOME;
        }
        return ItemSelectedState.NONE;
    }


    /*
	 *
	 */
    private class MultipleTweetSelectionCallback implements ListView.MultiChoiceModeListener {

        private MenuItem mFavoriteMenuItem;
        private MenuItem mRetweetMenuItem;

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            configure(mode);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            if (item == null || mode == null) {
                return true;
            }

            final int itemId = item.getItemId();
            if (itemId == R.id.action_share) {
                getBaseLaneActivity().shareSelected(getFirstSelectedStatus());
                mode.finish();

            } else if (itemId == R.id.action_retweet) {

                TwitterStatus statusSelected = getFirstSelectedStatus();

                if(statusSelected.mIsRetweetedByMe)
                {
                    showToast(getString(R.string.cannot_unretweet));
                    mode.finish();
                }
                else
                {

                    TwitterFetchStatus.FinishedCallback callback = TwitterManager.get()
                            .getFetchStatusInstance().new FinishedCallback() {

                        @Override
                        public void finished(TwitterFetchResult result, TwitterStatus status) {

                            if (result != null && result.isSuccessful())
                            {
                                if (status != null && status.mOriginalRetweetId > 0)
                                {
                                    TwitterStatuses cachedStatuses = getStatusFeed();
                                    TwitterStatus cachedStatus = cachedStatuses.findByStatusId(status.mOriginalRetweetId);
                                    if (cachedStatus != null)
                                    {
                                        cachedStatus.setRetweet(true);
                                        showToast(getString(R.string.retweeted_successfully));
                                        setIsRetweet(true);
                                    }
                                    else
                                    {
                                        showToast(getString(R.string.retweeted_un_successful));
                                    }
                                }
                                else
                                {
                                    showToast(getString(R.string.retweeted_un_successful));
                                }
                            }
                            else
                            {
                                showToast(getString(R.string.retweeted_un_successful));
                            }
                        }

                    };

                    getBaseLaneActivity().retweetSelected(statusSelected,callback);
                    mode.finish();
                }

            } else if (itemId == R.id.action_favorite) {
                TwitterModifyStatuses.FinishedCallback callback =
                        TwitterManager.get().getSetStatusesInstance().new FinishedCallback() {

                            @Override
                            public void finished(boolean successful, TwitterStatuses statuses, Integer value) {
                                if (successful == true) {

                                    TwitterStatuses cachedStatuses = getStatusFeed();

                                    boolean settingFavorited = true;

                                    if (statuses != null && statuses.getStatusCount() > 0) {
                                        for (int i = 0; i < statuses.getStatusCount(); i++) {
                                            TwitterStatus updatedStatus = statuses.getStatus(i);
                                            TwitterStatus cachedStatus =
                                                    cachedStatuses.findByStatusId(updatedStatus.mId);
                                            cachedStatus.setFavorite(updatedStatus.mIsFavorited);
                                            if (!updatedStatus.mIsFavorited){
                                                settingFavorited = false;
                                            }
                                        }
                                    }

                                    showToast(getString(settingFavorited ? R.string.favorited_successfully : R.string
                                            .unfavorited_successfully));

                                    setIsFavorited(settingFavorited);
                                }
                                else
                                {
                                    boolean newState = getSelectedFavoriteState() == ItemSelectedState.ALL ? false : true;
                                    showToast(getString(newState ? R.string.favorited_un_successfully : R.string
                                            .unfavorited_un_successfully));
                                }
                            }

                        };
                boolean newState = getSelectedFavoriteState() == ItemSelectedState.ALL ? false : true;
                TwitterManager.get().setFavorite(getSelectedStatuses(), newState, callback);
                mode.finish();
            } else if (itemId == R.id.action_manage_friendship) {
                showToast(getString(R.string.functionality_not_implemented));
                mode.finish();

            } else if (itemId == R.id.action_delete_status) {
                TwitterModifyStatuses.FinishedCallback callback =
                        TwitterManager.get().getSetStatusesInstance().new FinishedCallback() {

                            final TwitterStatuses selected = getSelectedStatuses();

                            @Override
                            public void finished(boolean successful, TwitterStatuses statuses, Integer value) {
                                if (successful == true) {

                                    showToast(getString(R.string.deleted_successfully));
                                }
                                else
                                {
                                    showToast(getString(R.string.deleted_un_successfully));

                                    if (statuses != null && statuses.getStatusCount() > 0) {
                                        TwitterStatuses cachedStatuses = getStatusFeed();
                                        cachedStatuses.add(statuses);
                                    }
                                }
                            }
                        };

                TwitterStatuses cachedStatuses = getStatusFeed();
                TwitterStatuses selectedStatuses =  getSelectedStatuses();
                TwitterManager.get().deleteTweet(selectedStatuses, callback);
                if (selectedStatuses != null && selectedStatuses.getStatusCount() > 0) {
                    cachedStatuses.remove(selectedStatuses);
                    _mCachedStatusFeed.remove(selectedStatuses);
                }
                mode.finish();
            } else if (itemId == R.id.action_report_for_spam || itemId == R.id.action_block) {
                AccountDescriptor account = getApp().getCurrentAccount();
                if (account != null) {

                    final TwitterStatuses selected = getSelectedStatuses();
                    if (selected != null && selected.getStatusCount() > 0) {

                        ArrayList<Long> userIds = new ArrayList<Long>();
                        for (int i = 0; i < selected.getStatusCount(); i++) {
                            userIds.add(selected.getStatus(i).mUserId);
                        }

                        TwitterFetchUsers.FinishedCallback callback =
                                TwitterManager.get().getFetchUsersInstance().new FinishedCallback() {

                                    @Override
                                    public void finished(TwitterFetchResult result, TwitterUsers users) {

                                        getBaseLaneActivity().finishCurrentActionMode();

                                        if (getStatusFeed() != null) {
                                            getStatusFeed().remove(selected);
                                        }

                                        setStatusFeed(getStatusFeed(), true);
                                        mTweetFeedListAdapter.notifyDataSetChanged();
                                        mTweetFeedListView.onRefreshComplete();
                                        updateViewVisibility(true);

                                        if (result.isSuccessful() && users != null && users.getUserCount() > 0) {
                                            int userCount = users.getUserCount();
                                            String notice = null;
                                            if (itemId == R.id.action_report_for_spam) {
                                                if (userCount == 1) {
                                                    notice = "Reported @" + users.getUser(0).getScreenName() +
                                                            " for Spam.";
                                                } else {
                                                    notice = "Reported " + userCount + " users for Spam.";
                                                }
                                            } else {
                                                if (userCount == 1) {
                                                    notice = "Blocked @" + users.getUser(0).getScreenName() + ".";
                                                } else {
                                                    notice = "Blocked " + userCount + " users.";
                                                }
                                            }
                                            if (notice != null) {
                                                showToast(notice);
                                            }
                                        }
                                    }
                                };

                        if (item.getItemId() == R.id.action_report_for_spam) {
                            TwitterManager.get().reportSpam(account.getId(), userIds, callback);
                        } else {
                            TwitterManager.get().createBlock(account.getId(), userIds, callback);
                        }
                    }
                }
            } else {
            }
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {

            mSelectedItems.clear();

            // Don't update the default status when TweetCompose has focus
            if (getBaseLaneActivity().composeHasFocus() == false) {
                getBaseLaneActivity().setComposeDefault();
            }
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            configure(mode);
        }

        void configure(ActionMode mode) {

            BaseLaneActivity baseLaneActivity = (BaseLaneActivity) getActivity();
            if (baseLaneActivity == null || baseLaneActivity.isComposing() || mTweetFeedListView == null ||
                    mTweetFeedListView.getRefreshableView() == null || mode == null) {
                return;
            }

            final int checkedCount = mTweetFeedListView.getRefreshableView().getCheckedItemCount();
            switch (checkedCount) {
                case 0:
                    mode.setSubtitle(null);
                    break;
                case 1: {
                    mode.getMenu().clear();
                    MenuInflater inflater = getActivity().getMenuInflater();
                    inflater.inflate(R.menu.single_tweet_selected, mode.getMenu());
                    storeMenuItems(mode.getMenu());
                    mode.setTitle("");
                    mode.setSubtitle("");
                    break;
                }
                case 2: {
                    mode.getMenu().clear();
                    MenuInflater inflater = getActivity().getMenuInflater();
                    inflater.inflate(R.menu.multiple_tweets_selected, mode.getMenu());
                    storeMenuItems(mode.getMenu());
                    mode.setTitle("Select Tweets");
                    mode.setSubtitle("" + checkedCount + " items selected");
                    break;
                }
                default: {
                    mode.setTitle("Select Tweets");
                    mode.setSubtitle("" + checkedCount + " items selected");
                    break;
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
                }else if (menuItem.getItemId() == R.id.action_retweet) {
                    mRetweetMenuItem = menuItem;
                }
            }
        }

        /*
         *
         */
        void setIsFavorited(boolean favorited) {
            if (mFavoriteMenuItem != null) {
                boolean isDarkTheme = AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark;
                if (favorited) {
                    mFavoriteMenuItem.setIcon(
                            isDarkTheme ? R.drawable.ic_action_star_on_dark : R.drawable.ic_action_star_on_light);
                    mFavoriteMenuItem.setTitle(R.string.action_unfavorite);
                } else {
                    mFavoriteMenuItem.setIcon(
                            isDarkTheme ? R.drawable.ic_action_star_off_dark : R.drawable.ic_action_star_off_light);
                    mFavoriteMenuItem.setTitle(R.string.action_favorite);
                }
            }
        }

        void setIsRetweet(boolean retweet) {
            if (mRetweetMenuItem != null) {
                boolean isDarkTheme = AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark;
                if (retweet) {
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
    }

    /*
     *
     */
    public boolean showConversationView(TwitterStatus status) {

        for (Long id : mConverstaionViewIds) {
            if (id == status.mId) {
                return true;
            }
        }

        return false;
    }

    /*
     *
     */
    private class TweetFeedListAdapter extends BaseAdapter {

        public TweetFeedListAdapter(LayoutInflater inflater) {
            mInflater = inflater;
        }

        /**
         * The number of items in the list is determined by the number of
         * speeches in our array.
         *
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() {
            int count;
            if (getStatusFeed() != null) {
                count = getFilteredStatusCount() + 1; // +1 for the trailing
                // LoadMore view
            } else {
                count = 1;
            }

            return count;
        }

        /**
         * Since the data comes from an array, just returning the index is
         * sufficent to get at the data. If we were using a more complex data
         * structure, we would return whatever object represents one row in the
         * list.
         *
         * @see android.widget.ListAdapter#getItem(int)
         */
        public Object getItem(int position) {
            return position;
        }

        /**
         * Use the array index as a unique id.
         *
         * @see android.widget.ListAdapter#getItemId(int)
         */
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a TweetFeedItemView to hold each row.
         */
        public View getView(int position, View convertView, ViewGroup parent) {

            int statusCount = getFilteredStatusCount();

            View resultView = null;
            if (statusCount == 0 && position == getCount() - 1) {
                resultView = getLoadMoreView(convertView);
            } else if (position == statusCount) {
                resultView = getLoadMoreView(convertView);
            } else {
                resultView = getTweetFeedView(position, convertView);
            }

            return resultView;
        }

        /*
         *
         */
        View getTweetFeedView(int position, View convertView) {

            convertView = mInflater.inflate(R.layout.tweet_feed_item_received, null);

            TwitterStatus item = getStatusFeed().getStatus(position, getBaseLaneActivity().mStatusesFilter);

            TweetFeedItemView tweetFeedItemView = (TweetFeedItemView) convertView.findViewById(R.id.tweetFeedItem);

            TweetFeedItemView.Callbacks callbacks = new TweetFeedItemView.Callbacks() {

                @Override
                public boolean onSingleTapConfirmed(View view, int position) {
                    return onTweetFeedItemSingleTap(view, position);
                }

                @Override
                public void onLongPress(View view, int position) {
                    onTweetFeedItemLongPress(view, position);
                }

                @Override
                public Activity getActivity() {
                    return TweetFeedFragment.this.getActivity();
                }

                @Override
                public void onUrlClicked(TwitterStatus status) {
                    TwitterStatuses selected = getSelectedStatuses();
                    if (selected != null && selected.getStatusCount() == 1) {
                        if (selected.getStatus(0).mId == status.mId) {

                            // HACK ALERT: Finishing the actionMode in this
                            // callback will cause the link to be unable to be
                            // clicked.
                            // Adding a slight delay makes things work just fine
                            Runnable finishActionModeTask = new Runnable() {

                                public void run() {
                                    getBaseLaneActivity().finishCurrentActionMode();
                                }
                            };
                            mHandler.postDelayed(finishActionModeTask, 90);
                        }
                    }

                }

                @Override
                public void onConversationViewToggle(long statusId, boolean show) {
                    if (show) {
                        boolean add = true;
                        for (int i = 0; i < mConverstaionViewIds.size(); ++i) {
                            if (mConverstaionViewIds.get(i) == statusId) {
                                add = false;
                                break;
                            }
                        }
                        if (add) {
                            mConverstaionViewIds.add(statusId);
                        }
                    } else {
                        for (int i = 0; i < mConverstaionViewIds.size(); ++i) {
                            if (mConverstaionViewIds.get(i) == statusId) {
                                mConverstaionViewIds.remove(i);
                                break;
                            }
                        }
                    }
                }

                @Override
                public LayoutInflater getLayoutInflater() {
                    return mInflater;
                }

                @Override
                public LazyImageLoader getProfileImageLoader() {
                    return mProfileImageLoader;
                }

                @Override
                public LazyImageLoader getPreviewImageLoader() {
                    return mPreviewImageLoader;
                }

            };

            boolean showRetweetCount = mContentHandle.getStatusesType() == TwitterConstant.StatusesType.RETWEETS_OF_ME;

            tweetFeedItemView
                    .configure(item, position + 1, callbacks, true, showRetweetCount, showConversationView(item), false,
                            true, getApp().getCurrentAccount().getSocialNetType(), getApp().getCurrentAccountKey());
            return tweetFeedItemView;
        }

        /*
         *
         */
        View getLoadMoreView(View convertView) {

            convertView = mInflater.inflate(R.layout.load_more, null);
            LoadMoreView loadMoreView = (LoadMoreView) convertView.findViewById(R.id.loadMoreView);

            LoadMoreView.Mode mode;
            if (getStatusFeed() == null || getFilteredStatusCount() == 0) {
                mode = LoadMoreView.Mode.NONE_FOUND;
            } else {
                mode = mMoreStatusesAvailable == true ? LoadMoreView.Mode.LOADING : LoadMoreView.Mode.NO_MORE;
            }

            loadMoreView.configure(mode);
            return loadMoreView;
        }

        private LayoutInflater mInflater;
    }

    private Handler mHandler = new Handler();

}

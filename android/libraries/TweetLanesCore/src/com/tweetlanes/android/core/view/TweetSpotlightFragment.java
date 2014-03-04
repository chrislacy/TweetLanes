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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ViewSwitcher;

import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshListView;

import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchStatus.FinishedCallback;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterStatus;

public final class TweetSpotlightFragment extends BaseLaneFragment {

    public static TweetSpotlightFragment newInstance(int laneIndex,
                                                     final TwitterStatus status) {

        TweetSpotlightFragment fragment = new TweetSpotlightFragment();
        fragment.configureBaseLaneFragment(laneIndex, "TweetSpotlight",
                new ConfigureBundleListener() {

                    @Override
                    public void addValues(Bundle args) {
                        args.putLong(KEY_STATUS_ID, status.mId);
                    }
                });
        fragment.mStatus = status;

        return fragment;
    }

    private PullToRefreshListView mTweetFeedListView;
    private TweetFeedListAdapter mTweetFeedListAdapter;
    private FinishedCallback mGetStatusCallback;
    private ViewSwitcher mViewSwitcher;

    private TwitterStatus mStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        //
        //
        //
        /*
         * mContentHandle =
         * TwitterManager.get().getContentHandle(getContentHandleBase(),
         * getScreenName(), null);
         */
        mGetStatusCallback = TwitterManager.get().getFetchStatusInstance().new FinishedCallback() {

            @Override
            public void finished(TwitterFetchResult result, TwitterStatus status) {
                if (mTweetFeedListAdapter != null) {
                    onStatusRefresh(status);
                    updateViewVisibility();
                } else {
                    String errorMessage = "";
                    if (result != null) {
                        errorMessage = result.getErrorMessage();
                        if (errorMessage == null) {
                            errorMessage = "";
                        }
                    }
                    if (errorMessage.contains("does not exist")) {
                        TweetSpotlightActivity spotlightActivity = (TweetSpotlightActivity) getActivity();
                        if (spotlightActivity != null) {
                            spotlightActivity.TweetDeleted(errorMessage);
                        }
                    }
                }
                mGetStatusCallback = null;
            }
        };

        TweetSpotlightActivity spotlightActivity = (TweetSpotlightActivity) getActivity();
        if (spotlightActivity != null) {
            mStatus = spotlightActivity.mStatus;
        }

        if (mStatus == null) {
            mStatus = TwitterManager.get().getStatus(getStatusId(),
                    mGetStatusCallback);
        }

        setInitialDownloadState(InitialDownloadState.DOWNLOADED);

        //
        //
        //
        View resultView = inflater.inflate(R.layout.lane, null);
        configureLaneWidth(resultView);

        mViewSwitcher = (ViewSwitcher) resultView
                .findViewById(R.id.profileSwitcher);
        mTweetFeedListView = (PullToRefreshListView) resultView
                .findViewById(R.id.pull_to_refresh_listview);

        mTweetFeedListView.setOnRefreshListener(new OnRefreshListener() {

            @Override
            public void onRefresh() {

                mGetStatusCallback = TwitterManager.get()
                        .getFetchStatusInstance().new FinishedCallback() {

                    @Override
                    public void finished(TwitterFetchResult result,
                                         TwitterStatus status) {
                        // TODO: Handle error properly
                        if (result.isSuccessful()) {

                            if (mStatus.mIsRetweet && mStatus.mOriginalRetweetId > 0) {
                                mGetStatusCallback = TwitterManager.get()
                                        .getFetchStatusInstance().new FinishedCallback() {

                                    @Override
                                    public void finished(TwitterFetchResult result,
                                                         TwitterStatus status) {
                                        // TODO: Handle error properly
                                        if (result.isSuccessful()) {
                                            mStatus.mIsRetweetedByMe = status.mIsRetweetedByMe;
                                            onStatusRefresh(mStatus);
                                        } else if (result.getErrorMessage().contains("does not exist")) {
                                            TweetSpotlightActivity spotlightActivity = (TweetSpotlightActivity) getActivity();
                                            if (spotlightActivity != null) {
                                                spotlightActivity.TweetDeleted(result.getErrorMessage());
                                            }
                                        }
                                        mGetStatusCallback = null;
                                    }
                                };

                                mStatus = status;
                                if (mStatus != null) {
                                    TwitterManager.get().getStatus(mStatus.mOriginalRetweetId, mGetStatusCallback);
                                }
                            } else {
                                onStatusRefresh(status);
                            }
                        } else if (result.getErrorMessage().contains("does not exist")) {
                            TweetSpotlightActivity spotlightActivity = (TweetSpotlightActivity) getActivity();
                            if (spotlightActivity != null) {
                                spotlightActivity.TweetDeleted(result.getErrorMessage());
                            }
                        }
                        mGetStatusCallback = null;
                    }
                };

                TwitterManager.get().getStatus(mStatus.mId, mGetStatusCallback);
            }
        });

        mTweetFeedListAdapter = new TweetFeedListAdapter(inflater);
        mTweetFeedListView.getRefreshableView().setAdapter(
                mTweetFeedListAdapter);

        updateViewVisibility();

        return resultView;
    }

    public void onDestroyView() {
        mTweetFeedListAdapter = null;

        super.onDestroyView();
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
            mTweetFeedListAdapter.notifyDataSetChanged();
        }
    }

    private static final String KEY_STATUS_ID = "statusId";

    private long getStatusId() {
        return getArguments().getLong(KEY_STATUS_ID);
    }

    /*
     *
     */
    private void onStatusRefresh(TwitterStatus status) {
        if (mTweetFeedListAdapter != null) {
            mStatus = status;
            mTweetFeedListAdapter.notifyDataSetChanged();
            mTweetFeedListView.onRefreshComplete();
            TweetSpotlightActivity spotlightActivity = (TweetSpotlightActivity) getActivity();
            if (spotlightActivity != null) {
                spotlightActivity.onGetStatus(mStatus);
            }
        }
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
            return 1;
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

            View resultView = null;

            if (position == 0) {
                resultView = getTweetFeedView(position);
            } else if (position == 1) {

            }

            return resultView;
        }

        /*
         *
         */
        View getTweetFeedView(int position) {

            View convertView = mInflater.inflate(R.layout.tweet_feed_item_spotlight,
                    null);

            TwitterStatus item = mStatus;

            TweetFeedItemView tweetFeedItemView = (TweetFeedItemView) convertView
                    .findViewById(R.id.tweetFeedItem);

            TweetFeedItemView.Callbacks callbacks = new TweetFeedItemView.Callbacks() {

                @Override
                public boolean onSingleTapConfirmed(View view, int position) {
                    return false;
                }

                @Override
                public void onLongPress(View view, int position) {
                }

                @Override
                public void onUrlClicked(TwitterStatus status) {
                }

                @Override
                public Activity getActivity() {
                    return TweetSpotlightFragment.this.getActivity();
                }

                @Override
                public LayoutInflater getLayoutInflater() {
                    return mInflater;
                }

                @Override
                public void onConversationViewToggle(long statusId, boolean show) {
                }

                @Override
                public LazyImageLoader getProfileImageLoader() {
                    return getApp().getProfileImageLoader();
                }

                @Override
                public LazyImageLoader getPreviewImageLoader() {
                    return getApp().getPreviewImageLoader();
                }

            };

            tweetFeedItemView.configure(item, position + 1, callbacks, false,
                    true, false, false, false, getApp().getCurrentAccount().getSocialNetType(), getApp().getCurrentAccountKey());
            return tweetFeedItemView;
        }

        /**
         * Remember our context so we can use it when constructing views.
         */
        // private Context mContext;
        private final LayoutInflater mInflater;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneFragment#triggerInitialDownload()
     */
    @Override
    public void triggerInitialDownload() {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#UpdateTweetCache()
     */
    @Override
    public void UpdateTweetCache(TwitterStatus status, boolean deleteStatus) {

    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#onJumpToTop()
     */
    @Override
    public void onJumpToTop() {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#clearLocalCache()
     */
    @Override
    public void clearLocalCache() {
        mStatus = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#getContentToCache()
     */
    @Override
    public String getDataToCache() {
        return null;
    }
}
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
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.util.LazyImageLoader;

import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterConstant;
import org.tweetalib.android.TwitterContentHandle;
import org.tweetalib.android.TwitterContentHandleBase;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.callback.TwitterFetchStatusesFinishedCallback;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatuses;

public class ConversationView extends LinearLayout {

    private Callbacks mCallbacks;
    private TwitterStatus mTwitterStatus;
    private RelativeLayout mLoadingView;
    private TwitterContentHandle mContentHandle;
    private TwitterStatuses mConversationStatuses;
    private LinearLayout mConversationView;
    private LayoutInflater mInflater;
    private SocialNetConstant.Type mSocialNetType;
    private String mCurrentAccountKey;

    /*
     *
     */
    public ConversationView(Context context) {
        super(context);
    }

    public ConversationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConversationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface Callbacks {

        Activity getActivity();

        LazyImageLoader getProfileImageLoader();

        LazyImageLoader getPreviewImageLoader();
    }

    /*
     *
	 */
    public void configure(TwitterStatus twitterStatus, LayoutInflater inflater,
                          Callbacks callbacks, SocialNetConstant.Type socialNetType, String currentAccountKey) {

        mTwitterStatus = twitterStatus;
        mInflater = inflater;
        mCallbacks = callbacks;
        mSocialNetType = socialNetType;
        mCurrentAccountKey = currentAccountKey;

        mLoadingView = (RelativeLayout) findViewById(R.id.conversation_feed_loading);
        mConversationView = (LinearLayout) findViewById(R.id.conversation_feed_loaded);

        TwitterContentHandleBase base = new TwitterContentHandleBase(
                TwitterConstant.ContentType.STATUSES,
                TwitterConstant.StatusesType.PREVIOUS_CONVERSATION);
        mContentHandle = new TwitterContentHandle(base,
                twitterStatus.mUserScreenName, Long.valueOf(mTwitterStatus.mId)
                .toString(), currentAccountKey);

        TwitterStatuses cachedStatuses = TwitterManager.get().getContentFeed(
                mContentHandle);
        if (cachedStatuses != null && cachedStatuses.getStatusCount() > 0) {
            setStatuses(cachedStatuses);
            updateViewVisibility(true);
        } else {
            TwitterManager.get().triggerFetchStatuses(mContentHandle, null,
                    new TwitterFetchStatusesFinishedCallback() {

                        @Override
                        public void finished(TwitterFetchResult result,
                                             TwitterStatuses feed, TwitterContentHandle handle) {

                            if (!mContentHandle.getKey().equals(handle.getKey())) {
                                Log.w("Statuses", "content handle changed");
                                return;
                            }

                            if (result.isSuccessful()) {
                                setStatuses(feed);
                            }
                            updateViewVisibility(true);
                        }

                    }, 1);
        }
    }

    /*
     *
	 */
    private void setStatuses(TwitterStatuses statuses) {

        if (statuses != null && statuses.getStatusCount() > 0) {
            mConversationStatuses = new TwitterStatuses();

            for (int i = 0; i < statuses.getStatusCount(); i++) {
                TwitterStatus status = statuses.getStatus(i);
                if (status.mId != mTwitterStatus.mId) {
                    mConversationStatuses.add(new TwitterStatus(status));
                }
            }

            if (mConversationStatuses.getStatusCount() == 0) {
                mConversationStatuses = null;
            }
        }
    }

    /*
     *
	 */
    private void updateViewVisibility(boolean loadHasFinished) {

        if (!loadHasFinished
                && (mConversationStatuses == null || mConversationStatuses
                .getStatusCount() == 0)) {
            mLoadingView.setVisibility(View.VISIBLE);
        } else {
            mLoadingView.setVisibility(View.GONE);

            mConversationView.removeAllViews();

            if (mConversationStatuses == null
                    || mConversationStatuses.getStatusCount() == 0) {
                View view = getLoadMoreView();
                mConversationView.addView(view);
            } else {
                for (int i = 0; i < mConversationStatuses.getStatusCount(); ++i) {
                    View view = getTweetFeedView(i,
                            mConversationStatuses.getStatus(i));
                    mConversationView.addView(view);
                }
            }
        }
    }

    /*
     *
     */
    View getTweetFeedView(int position, TwitterStatus item) {

        View convertView = mInflater.inflate(
                R.layout.tweet_feed_conversation_item, null);

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
            public Activity getActivity() {
                return mCallbacks.getActivity();
            }

            @Override
            public void onUrlClicked(TwitterStatus status) {
            }

            @Override
            public void onConversationViewToggle(long statusId, boolean show) {
            }

            @Override
            public LayoutInflater getLayoutInflater() {
                return null;
            }

            @Override
            public LazyImageLoader getProfileImageLoader() {
                return mCallbacks.getProfileImageLoader();
            }

            @Override
            public LazyImageLoader getPreviewImageLoader() {
                return mCallbacks.getPreviewImageLoader();
            }

        };

        tweetFeedItemView.configure(item, position, callbacks, true, false, false, true, false, mSocialNetType, mCurrentAccountKey);
        return tweetFeedItemView;
    }

    /*
     *
     */
    View getLoadMoreView() {

        View convertView = mInflater.inflate(R.layout.load_more, null);
        LoadMoreView loadMoreView = (LoadMoreView) convertView
                .findViewById(R.id.loadMoreView);

        loadMoreView.configure(LoadMoreView.Mode.NONE_FOUND);
        return loadMoreView;
    }

}

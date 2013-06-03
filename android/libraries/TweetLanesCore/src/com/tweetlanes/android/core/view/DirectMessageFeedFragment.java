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

import java.util.ArrayList;

import org.tweetalib.android.TwitterContentHandle;
import org.tweetalib.android.TwitterContentHandleBase;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterPaging;
import org.tweetalib.android.callback.TwitterFetchDirectMessagesFinishedCallback;
import org.tweetalib.android.model.TwitterDirectMessage;
import org.tweetalib.android.model.TwitterDirectMessage.MessageType;
import org.tweetalib.android.model.TwitterDirectMessages;
import org.tweetalib.android.model.TwitterDirectMessagesHandle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ViewSwitcher;

import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.Constant.SystemEvent;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.view.DirectMessageItemView.DirectMessageItemViewCallbacks;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshBase.OnLastItemVisibleListener;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshListView;

public class DirectMessageFeedFragment extends BaseLaneFragment {

    /*
     *
	 */
    public static DirectMessageFeedFragment newInstance(int laneIndex,
                                                        final TwitterContentHandleBase handleBase, final String screenName,
                                                        final String laneIdentifier, final Long otherUserId, final String currentAccountKey) {

        DirectMessageFeedFragment fragment = new DirectMessageFeedFragment();

        fragment.mContentHandle = TwitterManager.get().getContentHandle(
                handleBase, screenName, laneIdentifier, currentAccountKey);

        fragment.configureBaseLaneFragment(laneIndex,
                fragment.mContentHandle.getTypeAsString(),
                new ConfigureBundleListener() {

                    @Override
                    public void addValues(Bundle args) {
                        // TODO: serializing is a slow way of doing this...
                        args.putSerializable("handleBase", handleBase);
                        args.putString("screenName", screenName);
                        args.putString("laneIdentifier", laneIdentifier);
                        if (otherUserId != null) {
                            args.putLong("otherUserId", otherUserId);
                        }
                    }

                });

        return fragment;
    }

    private PullToRefreshListView mConversationListView;
    private DirectMessageConversationListAdapter mConversationListAdapter;
    private TwitterContentHandle mContentHandle;
    private TwitterDirectMessagesHandle mDirectMessagesHandle;
    private ArrayList<TwitterDirectMessage> mDirectMessageConversation;
    private TwitterFetchDirectMessagesFinishedCallback mRefreshCallback;
    private ViewSwitcher mViewSwitcher;

    private Long mNewestDirectMessageId;
    private Long mRefreshingNewestDirectMessageId;
    private Long mOldestDirectMessageId;
    private Long mRefreshingDirectMessageId;
    private boolean mMoreDirectMessagesAvailable = true;

    /*
	 *
	 */
    public App getApp() {
        return (App) getActivity().getApplication();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        mContentHandle = TwitterManager.get().getContentHandle(
                getContentHandleBase(), getScreenName(), getLaneIdentifier(), getApp().getCurrentAccountKey());
        mDirectMessagesHandle = new TwitterDirectMessagesHandle(
                getBaseLaneActivity().getApp().getCurrentAccount().getId(),
                getOtherUserId());

        if (mContentHandle == null) {
            // Occurs when coming back after the app was sleeping. Force refresh
            // of the adapter in this instance to ensure Fragments are created
            // correctly.
            showToast("No Content Handle found, forcing refresh");
            Intent intent = new Intent(""
                    + SystemEvent.FORCE_FRAGMENT_PAGER_ADAPTER_REFRESH);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                    intent);
            return null;
        }

        //
        //
        //
        View resultView = inflater.inflate(R.layout.lane, null);
        configureLaneWidth(resultView);

        mViewSwitcher = (ViewSwitcher) resultView
                .findViewById(R.id.profileSwitcher);
        mConversationListAdapter = new DirectMessageConversationListAdapter(
                inflater);
        // mMultipleConversationSelectionCallback = new
        // MultipleConversationSelectionCallback();
        mConversationListView = (PullToRefreshListView) resultView
                .findViewById(R.id.pull_to_refresh_listview);
        mConversationListView.getRefreshableView().setOnItemClickListener(
                mTweetFeedOnItemClickListener);
        mConversationListView.getRefreshableView().setChoiceMode(
                ListView.CHOICE_MODE_NONE);
        // mConversationListView.getRefreshableView().setMultiChoiceModeListener(mMultipleConversationSelectionCallback);
        // mConversationListView.getRefreshableView()
        mConversationListView.getRefreshableView().setOnScrollListener(
                mOnScrollListener);
        mConversationListView.getRefreshableView().setAdapter(
                mConversationListAdapter);
        mConversationListView.setOnRefreshListener(mOnRefreshListener);
        mConversationListView
                .setOnLastItemVisibleListener(mOnLastItemVisibleListener);

        //
        //
        //
        TwitterDirectMessages cachedDirectMessages = TwitterManager.get()
                .getDirectMessages(mContentHandle);
        if (cachedDirectMessages != null) {
            setDirectMessages(cachedDirectMessages);
        } else {
            setDirectMessages(null);
        }

        if (mDirectMessageConversation == null
                || mDirectMessageConversation.size() == 0) {
            updateViewVisibility(false);
            setInitialDownloadState(InitialDownloadState.WAITING);
        } else {
            setInitialDownloadState(InitialDownloadState.DOWNLOADED);
            updateViewVisibility(true);
        }

        return resultView;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private TwitterContentHandleBase getContentHandleBase() {
        return (TwitterContentHandleBase) getArguments().getSerializable(
                "handleBase");
    }

    private String getScreenName() {
        return getArguments().getString("screenName");
    }

    private String getLaneIdentifier() {
        return getArguments().getString("laneIdentifier");
    }

    private Long getOtherUserId() {
        return getArguments().containsKey("otherUserId") ? getArguments()
                .getLong("otherUserId") : null;
    }

    /*
	 *
	 */
    private void setDirectMessages(TwitterDirectMessages directMessages) {

        mNewestDirectMessageId = null;
        mOldestDirectMessageId = null;

        if (directMessages == null
                || directMessages.getConversationCount() == 0) {
            mDirectMessageConversation = null;
        } else {
            mDirectMessageConversation = directMessages
                    .getList(mDirectMessagesHandle);
            // _mDirectMessages = new TwitterDirectMessages(directMessages,
            // getOtherUserId());

            Long newestDirectMessageId = directMessages
                    .getNewestDirectMessageId();
            if (newestDirectMessageId != null) {
                mNewestDirectMessageId = newestDirectMessageId;
            }

            Long oldestDirectMessageId = directMessages
                    .getOldestDirectMessageId();
            if (oldestDirectMessageId != null) {
                mOldestDirectMessageId = oldestDirectMessageId;
            }
        }
    }

    /*
	 *
	 */
    // private TwitterDirectMessages getDirectMessages() {
    // return _mDirectMessages;
    // }

    /*
	 *
	 */
    private void onRefreshComplete(TwitterDirectMessages feed) {

        if (feed != null) {
            setDirectMessages(feed);
        }
        mConversationListAdapter.notifyDataSetChanged();
        mRefreshCallback = null;
    }

    /*
	 *
	 */
    private void updateViewVisibility(boolean loadHasFinished) {

        mViewSwitcher.reset();

        if (loadHasFinished == false
                && (mDirectMessageConversation == null || mDirectMessageConversation
                .size() == 0)) {
            mViewSwitcher.setDisplayedChild(0);
        } else {
            mViewSwitcher.setDisplayedChild(1);
            mConversationListAdapter.notifyDataSetChanged();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#onJumpToTop()
     */
    @Override
    public void onJumpToTop() {
        if (mConversationListView != null) {
            ListView listView = mConversationListView.getRefreshableView();
            if (listView != null && listView.getAdapter() != null
                    && listView.getAdapter().isEmpty() == false) {
                listView.setSelection(0);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#clearLocalCache()
     */
    @Override
    public void clearLocalCache() {
        mDirectMessageConversation = null;
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

    /*
	 *
	 */
    private OnScrollListener mOnScrollListener = new OnScrollListener() {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            mConversationListView.onScroll(view, firstVisibleItem,
                    visibleItemCount, totalItemCount);
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

            if (scrollState == 0) {
                // Restore scrollbar fading, which may have been set to false
                // when scrolling through items via the volume keys
                view.setScrollbarFadingEnabled(true);
            }

            mConversationListView.onScrollStateChanged(view, scrollState);
        }

    };

    /*
	 *
	 */
    private OnLastItemVisibleListener mOnLastItemVisibleListener = new OnLastItemVisibleListener() {

        @Override
        public void onLastItemVisible() {

            mRefreshCallback = new TwitterFetchDirectMessagesFinishedCallback() {

                @Override
                public void finished(TwitterFetchResult result,
                                     TwitterDirectMessages feed) {

                    onRefreshComplete(feed);

                    if (mRefreshingDirectMessageId.longValue() == mOldestDirectMessageId
                            .longValue()) {
                        mMoreDirectMessagesAvailable = false;
                    }
                    mRefreshingDirectMessageId = null;
                }
            };

            if (mOldestDirectMessageId != null) {
                if (mRefreshingDirectMessageId == null) {
                    // TODO: Bit of a hack, but does remove an unnecessary fetch
                    // that is triggered here when initializing
                    int count = mConversationListAdapter.getCount();
                    if (count > 2) {
                        mRefreshingDirectMessageId = mOldestDirectMessageId;
                        TwitterManager
                                .get()
                                .getDirectMessages(
                                        mContentHandle,
                                        TwitterPaging
                                                .createGetOlder(mOldestDirectMessageId),
                                        mRefreshCallback);
                    }
                }
            }
        }
    };

    /*
	 *
	 */
    private OnRefreshListener mOnRefreshListener = new OnRefreshListener() {

        @Override
        public void onRefresh() {

            mRefreshCallback = new TwitterFetchDirectMessagesFinishedCallback() {

                @Override
                public void finished(TwitterFetchResult result,
                                     TwitterDirectMessages feed) {

                    onRefreshComplete(feed);
                    mConversationListView.onRefreshComplete();
                    mRefreshingNewestDirectMessageId = null;
                }
            };

            if (mNewestDirectMessageId != null) {
                if (mRefreshingNewestDirectMessageId == null) {
                    mRefreshingNewestDirectMessageId = mNewestDirectMessageId;
                    TwitterDirectMessages directMessages = TwitterManager
                            .get()
                            .getDirectMessages(
                                    mContentHandle,
                                    TwitterPaging
                                            .createGetNewer(mNewestDirectMessageId),
                                    mRefreshCallback);
                    if (directMessages == null) {
                        getBaseLaneActivity().finishCurrentActionMode();
                    }
                }
            } else {
                TwitterManager.get().getDirectMessages(mContentHandle, null,
                        mRefreshCallback);
                getBaseLaneActivity().finishCurrentActionMode();
            }
        }
    };

    /*
	 *
	 */
    private final OnItemClickListener mTweetFeedOnItemClickListener = new OnItemClickListener() {

        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            // tweetFeedListView.setItemChecked(position, true);
        }
    };

    /*
	 *
	 */
    private void onDirectMessageItemViewClick(View view, int position) {

        if (getOtherUserId() == null) {

            DirectMessageItemView directMessageItemView = (DirectMessageItemView) (view);

            TwitterDirectMessage directMessage = directMessageItemView
                    .getDirectMessage();

            DirectMessageActivity.createAndStartActivity(getActivity(),
                    mContentHandle, directMessage.getOtherUserId(),
                    directMessage.getOtherUserScreenName());
        }

        /*
         * for (int index = 0; index < mSelectedItems.size(); index++) {
         * DirectMessageItemView item = mSelectedItems.get(index); if
         * (item.getDirectMessage() != null &&
         * directMessageItemView.getDirectMessage() != null) { if
         * (item.getDirectMessage().getId() ==
         * directMessageItemView.getDirectMessage().getId()) {
         * mSelectedItems.remove(index); break; } } } if (!isChecked) {
         * mSelectedItems.add(directMessageItemView); }
         */

        // if (mSelectedItems.size() > 0) {
        // getBaseLaneActivity().setComposeDefault(new
        // ComposeTweetDefault(getApp().getCurrentAccountScreenName(),
        // getSelectedUsers()));
        // } else {
        // getBaseLaneActivity().setComposeDefault();
        // }
    }

    /*
     *
     */
    public interface DirectMessageViewCallbacks {

        public void onClicked(View view, int position);

        public Activity getActivity();
    }

    /*
     *
     */
    private class DirectMessageConversationListAdapter extends BaseAdapter {

        public DirectMessageConversationListAdapter(LayoutInflater inflater) {
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
            if (mDirectMessageConversation != null) {
                if (getOtherUserId() == null) {
                    count = mDirectMessageConversation.size() + 1; // +1 for the
                    // trailing
                    // LoadMore
                    // view
                } else {
                    count = mDirectMessageConversation.size();
                }
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
         *
         */
        public View getView(int position, View convertView, ViewGroup parent) {

            int directMessageCount = mDirectMessageConversation != null ? mDirectMessageConversation
                    .size() : 0;

            View resultView = null;
            if (directMessageCount == 0 && position == getCount() - 1) {
                resultView = getLoadMoreView(convertView);
            } else if (position == directMessageCount) {
                resultView = getLoadMoreView(convertView);
            } else {
                resultView = getDirectMessageFeedItemView(position, convertView);
            }

            return resultView;
        }

        /*
         *
         */
        View getDirectMessageFeedItemView(int position, View convertView) {

            TwitterDirectMessage directMessage = mDirectMessageConversation
                    .get(position);

            MessageType messageType = MessageType.RECEIVED;

            Long otherUserId = getOtherUserId();
            if (otherUserId != null
                    && directMessage.getMessageType() == MessageType.SENT) {
                messageType = MessageType.SENT;
                // convertView =
                // mInflater.inflate(R.layout.direct_message_feed_item_sent,
                // null);
            }
            convertView = mInflater.inflate(
                    R.layout.direct_message_feed_item_received, null);

            DirectMessageItemView directMessageItemView = (DirectMessageItemView) convertView
                    .findViewById(R.id.directMessageItem);

            DirectMessageItemViewCallbacks callbacks = new DirectMessageItemViewCallbacks() {

                @Override
                public void onClicked(View view, int position) {
                    onDirectMessageItemViewClick(view, position);
                }

                @Override
                public Activity getActivity() {
                    return DirectMessageFeedFragment.this.getActivity();
                }

                @Override
                public LazyImageLoader getProfileImageLoader() {
                    return getApp().getProfileImageLoader();
                }

            };

            directMessageItemView.configure(getScreenName(), directMessage,
                    position + 1, messageType, otherUserId == null ? false
                    : true, callbacks);
            return directMessageItemView;
        }

        /*
         *
         */
        View getLoadMoreView(View convertView) {

            convertView = mInflater.inflate(R.layout.load_more, null);
            LoadMoreView loadMoreView = (LoadMoreView) convertView
                    .findViewById(R.id.loadMoreView);

            LoadMoreView.Mode mode;
            if (mDirectMessageConversation == null
                    || mDirectMessageConversation.size() == 0) {
                mode = LoadMoreView.Mode.NONE_FOUND;
            } else {
                mode = mMoreDirectMessagesAvailable == true ? LoadMoreView.Mode.LOADING
                        : LoadMoreView.Mode.NO_MORE;
            }

            loadMoreView.configure(mode);
            return loadMoreView;
        }

        /**
         * Remember our context so we can use it when constructing views.
         */
        // private Context mContext;
        private LayoutInflater mInflater;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.BaseLaneFragment#triggerInitialDownload()
     */
    @Override
    public void triggerInitialDownload() {

        mRefreshCallback = new TwitterFetchDirectMessagesFinishedCallback() {

            @Override
            public void finished(TwitterFetchResult fetchResult,
                                 TwitterDirectMessages directMessages) {
                onRefreshComplete(directMessages);
                mConversationListView.onRefreshComplete();

                updateViewVisibility(true);
                mRefreshCallback = null;

                setInitialDownloadState(InitialDownloadState.DOWNLOADED);
            }

        };

        TwitterDirectMessages cachedDirectMessages = TwitterManager.get()
                .getDirectMessages(mContentHandle, null, mRefreshCallback);
        if (cachedDirectMessages != null) {
            if (mRefreshCallback != null) {
                mRefreshCallback.finished(new TwitterFetchResult(true, null),
                        cachedDirectMessages);
            }
        } else {
            setInitialDownloadState(InitialDownloadState.DOWNLOADING);
        }
    }

}

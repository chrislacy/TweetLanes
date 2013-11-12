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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.Constant.SystemEvent;
import com.tweetlanes.android.core.Notifier;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.SharedPreferencesConstants;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.view.DirectMessageItemView.DirectMessageItemViewCallbacks;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshBase.OnLastItemVisibleListener;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tweetalib.android.TwitterContentHandle;
import org.tweetalib.android.TwitterContentHandleBase;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterModifyDirectMessages;
import org.tweetalib.android.TwitterPaging;
import org.tweetalib.android.callback.TwitterFetchDirectMessagesFinishedCallback;
import org.tweetalib.android.model.TwitterDirectMessage;
import org.tweetalib.android.model.TwitterDirectMessage.MessageType;
import org.tweetalib.android.model.TwitterDirectMessages;
import org.tweetalib.android.model.TwitterDirectMessagesHandle;
import org.tweetalib.android.model.TwitterStatus;

import java.util.ArrayList;
import java.util.Calendar;

public class DirectMessageFeedFragment extends BaseLaneFragment {

    /*
     *
	 */
    public static DirectMessageFeedFragment newInstance(int laneIndex,
                                                        final TwitterContentHandleBase handleBase, final String screenName,
                                                        final String laneIdentifier, final Long otherUserId, final String currentAccountKey,
                                                        final String cachedMessages) {

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
                        if (cachedMessages != null) {
                            args.putString("cachedMessages", cachedMessages);
                        }
                    }

                });

        return fragment;
    }

    private PullToRefreshListView mConversationListView;
    private DirectMessageConversationListAdapter mConversationListAdapter;
    private TwitterContentHandle mContentHandle;
    private TwitterDirectMessagesHandle mDirectMessagesHandle;
    private TwitterDirectMessages mDirectMessages;
    private TwitterDirectMessages mDirectMessagesCache;
    private ArrayList<TwitterDirectMessage> mCurrentViewDirectMessageConversion;
    private TwitterFetchDirectMessagesFinishedCallback mRefreshCallback;
    private final ArrayList<DirectMessageItemView> mSelectedItems = new ArrayList<DirectMessageItemView>();
    private MultipleDirectMessageSelectionCallback mMultipleDirectMessageSelectionCallback;
    private ViewSwitcher mViewSwitcher;

    private Long mNewestDirectMessageId;
    private Long mRefreshingNewestDirectMessageId;
    private Long mOldestDirectMessageId;
    private Long mRefreshingDirectMessageId;
    private boolean mMoreDirectMessagesAvailable = true;
    private Calendar mLastRefreshTime = null;

    private static final String KEY_STATUSES = "statuses";

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

        mViewSwitcher = (ViewSwitcher) resultView.findViewById(R.id.profileSwitcher);
        mConversationListAdapter = new DirectMessageConversationListAdapter(inflater);

        mMultipleDirectMessageSelectionCallback = new MultipleDirectMessageSelectionCallback();

        mConversationListView = (PullToRefreshListView) resultView.findViewById(R.id.pull_to_refresh_listview);
        mConversationListView.getRefreshableView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mConversationListView.getRefreshableView().setMultiChoiceModeListener(mMultipleDirectMessageSelectionCallback);
        mConversationListView.getRefreshableView().setOnScrollListener(mOnScrollListener);
        mConversationListView.getRefreshableView().setAdapter(mConversationListAdapter);
        mConversationListView.setOnRefreshListener(mOnRefreshListener);

        configureInitialStatuses();

        return resultView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getLaneIndex() == getApp().getCurrentAccount().getCurrentLaneIndex(Constant.LaneType.DIRECT_MESSAGES)) {

            String cacheKey = "dm_" + getApp().getCurrentAccountKey();
            String dmCachedData = getApp().getCachedData(cacheKey);
            try {
                if (dmCachedData != null) {
                    TwitterDirectMessages directMessages = new TwitterDirectMessages(getBaseLaneActivity().getApp().getCurrentAccount().getId());
                    JSONArray jsonArray = new JSONArray(dmCachedData);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String statusString = jsonArray.getString(i);
                        TwitterDirectMessage status = new TwitterDirectMessage(statusString);
                        directMessages.add(status);
                    }

                    getApp().removeCachedData(cacheKey);

                    TwitterDirectMessages cachedFeed = TwitterManager.get().setDirectMessages(mContentHandle, directMessages);

                    onRefreshComplete(cachedFeed, true);
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (AppSettings.get().isAutoRefreshEnabled()) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, -2);
            if (mLastRefreshTime == null) {
                fetchNewestTweets(false);
            } else if (mLastRefreshTime.before(cal)) {
                fetchNewestTweets(false);
            }
        }
    }

    private void configureInitialStatuses() {

        boolean configuredCachedStatuses = configureCachedStatuses();

        TwitterDirectMessages cachedDirectMessages = TwitterManager.get().getDirectMessages(mContentHandle);
        if (cachedDirectMessages != null) {
            setDirectMessages(cachedDirectMessages, true);
        } else if (!configuredCachedStatuses) {
            setDirectMessages(null, true);
        }

        if (mCurrentViewDirectMessageConversion == null || mCurrentViewDirectMessageConversion.size() == 0) {
            updateViewVisibility(false);
            setInitialDownloadState(InitialDownloadState.WAITING);
        } else {
            setInitialDownloadState(InitialDownloadState.DOWNLOADED);
            updateViewVisibility(true);
        }
    }

    boolean configureCachedStatuses() {

        String statusesAsString = null;
        try {
            String cachedData = getCachedData();
            if (cachedData == null) {
                statusesAsString = getArguments().getString("cachedMessages");
            } else {
                JSONObject object;
                object = new JSONObject(cachedData);
                if (object.has(KEY_STATUSES)) {
                    statusesAsString = object.getString(KEY_STATUSES);
                }
            }

            return addCachedStatusesFromString(statusesAsString);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    boolean addCachedStatusesFromString(String statusesAsString) throws JSONException {

        if (statusesAsString != null) {
            mDirectMessagesCache = new TwitterDirectMessages(getBaseLaneActivity().getApp().getCurrentAccount().getId());
            JSONArray jsonArray = new JSONArray(statusesAsString);
            for (int i = 0; i < jsonArray.length(); i++) {
                String statusString = jsonArray.getString(i);
                TwitterDirectMessage status = new TwitterDirectMessage(statusString);
                mDirectMessagesCache.add(status);
            }

            setDirectMessages(mDirectMessagesCache, false);

            return true;
        }

        return false;
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
    private void setDirectMessages(TwitterDirectMessages directMessages, boolean addCachedMessages) {

        mNewestDirectMessageId = null;
        mOldestDirectMessageId = null;

        if (directMessages == null || directMessages.getConversationCount() == 0) {
            mDirectMessages = null;
            mCurrentViewDirectMessageConversion = null;
        } else {
            mDirectMessages = directMessages;
        }

        if (addCachedMessages && mDirectMessagesCache != null && mDirectMessagesCache.getAllMessages().size() > 0) {
            if (mDirectMessages == null) {
                mDirectMessages = new TwitterDirectMessages(getBaseLaneActivity().getApp().getCurrentAccount().getId());
            }
            mDirectMessages.insert(mDirectMessagesCache);
        }

        if (mDirectMessages != null) {
            mCurrentViewDirectMessageConversion = mDirectMessages
                    .getList(mDirectMessagesHandle);

            Long newestDirectMessageId = mDirectMessages
                    .getNewestDirectMessageId();
            if (newestDirectMessageId != null) {
                mNewestDirectMessageId = newestDirectMessageId;
            }

            Long oldestDirectMessageId = mDirectMessages
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
    private void onRefreshComplete(TwitterDirectMessages feed, boolean addCache) {

        mLastRefreshTime = Calendar.getInstance();

        if (feed != null) {
            setDirectMessages(feed, addCache);
        }
        mConversationListAdapter.notifyDataSetChanged();
        mRefreshCallback = null;
    }

    /*
     *
	 */
    private void updateViewVisibility(boolean loadHasFinished) {

        mViewSwitcher.reset();

        if (!loadHasFinished
                && (mCurrentViewDirectMessageConversion == null || mCurrentViewDirectMessageConversion
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
                    && !listView.getAdapter().isEmpty()) {
                listView.setSelection(0);
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

    }

    public void UpdateTweetCache() {
        TwitterDirectMessages cachedDirectMessages = TwitterManager.get().getDirectMessages(mContentHandle);
        if (cachedDirectMessages != null) {
            setDirectMessages(cachedDirectMessages, true);
        }
        mConversationListAdapter.notifyDataSetChanged();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#clearLocalCache()
     */
    @Override
    public void clearLocalCache() {
        mCurrentViewDirectMessageConversion = null;
        mDirectMessages = null;
        mDirectMessagesCache = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#getContentToCache()
     */
    @Override
    public String getDataToCache() {

        if (mCurrentViewDirectMessageConversion == null || mCurrentViewDirectMessageConversion.size() == 0) {
            return null;
        }

        if (getOtherUserId() != null) {
            return null;
        }

        JSONObject object = new JSONObject();

        try {
            object.put(KEY_STATUSES, ConvertCacheIntoJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object.toString();
    }

    private JSONArray ConvertCacheIntoJSON() {
        JSONArray statusArray = new JSONArray();
        ArrayList<TwitterDirectMessage> messages = mDirectMessages.getAllMessages();
        int statusCount = messages.size();
        for (int i = 0; i < statusCount; ++i) {
            TwitterDirectMessage status = messages.get(i);
            statusArray.put(status.toString());
        }

        return statusArray;
    }

    /*
     *
	 */
    private final OnScrollListener mOnScrollListener = new OnScrollListener() {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            mConversationListView.onScroll(view, firstVisibleItem,
                    visibleItemCount, totalItemCount);

            setNotificationsRead();
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

    private void setNotificationsRead() {
        if (getLaneIndex() == getApp().getCurrentAccount().getCurrentLaneIndex(Constant.LaneType.DIRECT_MESSAGES)) {

            String notifcationType = SharedPreferencesConstants.NOTIFICATION_TYPE_DIRECT_MESSAGE;
            String pref = SharedPreferencesConstants.NOTIFICATION_LAST_DISPLAYED_DIRECT_MESSAGE_ID;

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseLaneActivity());
            long lastDisplayedMentionId = preferences.getLong(pref + getApp().getCurrentAccountKey(), 0);

            Notifier.saveLastNotificationActioned(getBaseLaneActivity(),
                    getApp().getCurrentAccountKey(), notifcationType, lastDisplayedMentionId);
            Notifier.cancel(getBaseLaneActivity(), getApp().getCurrentAccountKey(), notifcationType);
        }
    }

    /*
     *
	 */
    private final OnLastItemVisibleListener mOnLastItemVisibleListener = new OnLastItemVisibleListener() {

        @Override
        public void onLastItemVisible() {

            mRefreshCallback = new TwitterFetchDirectMessagesFinishedCallback() {

                @Override
                public void finished(TwitterContentHandle handle, TwitterFetchResult result,
                                     TwitterDirectMessages feed) {

                    onRefreshComplete(feed, true);

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

    @Override
    public void fetchNewestTweets() {
        super.fetchNewestTweets();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -5);
        if(mLastRefreshTime == null){
            fetchNewestTweets(true);
        }else if(mLastRefreshTime.before(cal)){
            fetchNewestTweets(true);
        }else{
            fetchNewestTweets(false);
        }
    }


    public void fetchNewestTweets(boolean fullRefresh) {
        super.fetchNewestTweets();

        mRefreshCallback = new TwitterFetchDirectMessagesFinishedCallback() {

            @Override
            public void finished(TwitterContentHandle contentHandle, TwitterFetchResult result,
                                 TwitterDirectMessages feed) {

                onRefreshComplete(feed, false);
                mDirectMessagesCache = mDirectMessages;
                mConversationListView.onRefreshComplete();
                mRefreshingNewestDirectMessageId = null;
            }
        };

        TwitterPaging paging = null;
        if (getOtherUserId() != null || !fullRefresh) {
            paging = TwitterPaging.createGetNewer(mNewestDirectMessageId);
            mRefreshCallback = new TwitterFetchDirectMessagesFinishedCallback() {

                @Override
                public void finished(TwitterContentHandle contentHandle, TwitterFetchResult result,
                                     TwitterDirectMessages feed) {

                    onRefreshComplete(feed, true);
                    mConversationListView.onRefreshComplete();
                    mRefreshingNewestDirectMessageId = null;
                }
            };
        }

        if (mRefreshingNewestDirectMessageId == null) {
            mRefreshingNewestDirectMessageId = mNewestDirectMessageId;
            TwitterDirectMessages directMessages = TwitterManager
                    .get()
                    .getDirectMessages(
                            mContentHandle,
                            paging,
                            mRefreshCallback);
            if (directMessages == null) {
                getBaseLaneActivity().finishCurrentActionMode();
            }
        }
    }

    /*
     *
	 */
    private final OnRefreshListener mOnRefreshListener = new OnRefreshListener() {

        @Override
        public void onRefresh() {
            fetchNewestTweets();
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
    private void onDirectMessageItemSingleTap(View view, int position) {

        if (mSelectedItems.size() == 0) {
            if (getOtherUserId() == null) {

                DirectMessageItemView directMessageItemView = (DirectMessageItemView) (view);

                TwitterDirectMessage directMessage = directMessageItemView
                        .getDirectMessage();

                DirectMessageActivity.createAndStartActivity(getActivity(),
                        mContentHandle, directMessage.getOtherUserId(),
                        directMessage.getOtherUserScreenName(), mDirectMessages.getAllConversation(directMessage.getOtherUserId()));
            }
        } else {
            onDirectMessageItemLongPress(view, position);
        }

    }

    private void onDirectMessageItemLongPress(View view, int position) {

        boolean isChecked = mConversationListView.getRefreshableView().getCheckedItemPositions().get(position);

        DirectMessageItemView directMessageItemView = (DirectMessageItemView) (view);

        for (int index = 0; index < mSelectedItems.size(); index++) {
            DirectMessageItemView item = mSelectedItems.get(index);
            if (item.getDirectMessage() != null && directMessageItemView.getDirectMessage() != null) {
                if (item.getDirectMessage().getId() == directMessageItemView.getDirectMessage().getId()) {
                    mSelectedItems.remove(index);
                    break;
                }
            }
        }

        if (!isChecked) {
            mSelectedItems.add(directMessageItemView);
        }

        mConversationListView.getRefreshableView().setItemChecked(position, !isChecked);
    }

    private TwitterDirectMessages getSelectedStatuses() {

        TwitterDirectMessages selectedList = new TwitterDirectMessages(getBaseLaneActivity().getApp().getCurrentAccount().getId());

        for (int i = 0; i < mSelectedItems.size(); i++) {
            DirectMessageItemView tweetFeedItemView = mSelectedItems.get(i);
            TwitterDirectMessage message = tweetFeedItemView.getDirectMessage();
            if (message != null) {
                selectedList.add(message);
            }
        }

        return selectedList.getAllMessages().size() > 0 ? selectedList : null;
    }

    private class MultipleDirectMessageSelectionCallback implements ListView.MultiChoiceModeListener {


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

            if (itemId == R.id.action_delete_status) {
                final TwitterDirectMessages selected = getSelectedStatuses();
                TwitterModifyDirectMessages.FinishedCallback callback =
                        TwitterManager.get().getSetDirectMessagesInstance().new FinishedCallback() {

                            @Override
                            public void finished(boolean successful, Integer value) {
                                if (successful) {

                                    showToast(getString(R.string.deleted_successfully));
                                    if (mDirectMessages != null) {
                                        mDirectMessages.remove(selected);
                                    }
                                    if (mDirectMessagesCache != null) {
                                        mDirectMessagesCache.remove(selected);
                                    }

                                    setDirectMessages(mDirectMessages, true);
                                    mConversationListAdapter.notifyDataSetChanged();
                                    mConversationListView.onRefreshComplete();
                                    updateViewVisibility(true);
                                } else {
                                    showToast(getString(R.string.deleted_un_successfully));
                                }
                            }
                        };

                TwitterManager.get().deleteDirectMessage(selected, callback);
                mode.finish();
            } else {
            }
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {

            mSelectedItems.clear();

            // Don't update the default status when TweetCompose has focus
            if (!getBaseLaneActivity().composeHasFocus()) {
                getBaseLaneActivity().clearCompose();
                getBaseLaneActivity().setComposeDefault();
            }
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            configure(mode);
        }

        void configure(ActionMode mode) {

            BaseLaneActivity baseLaneActivity = (BaseLaneActivity) getActivity();
            if (baseLaneActivity == null || baseLaneActivity.isComposing() || mConversationListView == null ||
                    mConversationListView.getRefreshableView() == null || mode == null) {
                return;
            }

            final int checkedCount = mConversationListView.getRefreshableView().getCheckedItemCount();
            switch (checkedCount) {
                case 0:
                    mode.setSubtitle(null);
                    break;
                case 1: {
                    mode.getMenu().clear();
                    MenuInflater inflater = getActivity().getMenuInflater();
                    inflater.inflate(R.menu.dm_selected, mode.getMenu());
                    mode.setTitle("");
                    mode.setSubtitle("");
                    break;
                }
                case 2: {
                    mode.getMenu().clear();
                    MenuInflater inflater = getActivity().getMenuInflater();
                    inflater.inflate(R.menu.dm_selected, mode.getMenu());
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
            if (mCurrentViewDirectMessageConversion != null) {
                if (getOtherUserId() == null) {
                    count = mCurrentViewDirectMessageConversion.size() + 1; // +1 for the
                    // trailing
                    // LoadMore
                    // view
                } else {
                    count = mCurrentViewDirectMessageConversion.size();
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

            int directMessageCount = mCurrentViewDirectMessageConversion != null ? mCurrentViewDirectMessageConversion
                    .size() : 0;

            View resultView;
            if (directMessageCount == 0 && position == getCount() - 1) {
                resultView = getLoadMoreView();
            } else if (position == directMessageCount) {
                resultView = getLoadMoreView();
            } else {
                resultView = getDirectMessageFeedItemView(position);
            }

            return resultView;
        }

        /*
         *
         */
        View getDirectMessageFeedItemView(int position) {

            TwitterDirectMessage directMessage = mCurrentViewDirectMessageConversion
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
            View convertView = mInflater.inflate(
                    R.layout.direct_message_feed_item_received, null);

            DirectMessageItemView directMessageItemView = (DirectMessageItemView) convertView
                    .findViewById(R.id.directMessageItem);

            DirectMessageItemViewCallbacks callbacks = new DirectMessageItemViewCallbacks() {

                @Override
                public void onLongPress(View view, int position) {
                    onDirectMessageItemLongPress(view, position);
                }

                @Override
                public boolean onSingleTapConfirmed(View view, int position) {
                    onDirectMessageItemSingleTap(view, position);
                    return true;
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
                    position + 1, messageType, otherUserId != null, callbacks);
            return directMessageItemView;
        }

        /*
         *
         */
        View getLoadMoreView() {

            View convertView = mInflater.inflate(R.layout.load_more, null);
            LoadMoreView loadMoreView = (LoadMoreView) convertView
                    .findViewById(R.id.loadMoreView);

            LoadMoreView.Mode mode;
            if (mCurrentViewDirectMessageConversion == null
                    || mCurrentViewDirectMessageConversion.size() == 0) {
                mode = LoadMoreView.Mode.NONE_FOUND;
            } else {
                mode = mMoreDirectMessagesAvailable ? LoadMoreView.Mode.LOADING
                        : LoadMoreView.Mode.NO_MORE;
            }

            loadMoreView.configure(mode);
            return loadMoreView;
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

        mRefreshCallback = new TwitterFetchDirectMessagesFinishedCallback() {

            @Override
            public void finished(TwitterContentHandle contentHandle, TwitterFetchResult fetchResult,
                                 TwitterDirectMessages directMessages) {
                onRefreshComplete(directMessages, true);
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
                mRefreshCallback.finished(mContentHandle, new TwitterFetchResult(true, null),
                        cachedDirectMessages);
            }
        } else {
            setInitialDownloadState(InitialDownloadState.DOWNLOADING);
        }
    }

}

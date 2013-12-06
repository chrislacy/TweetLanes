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
import android.os.Bundle;
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
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.Constant.SystemEvent;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.model.AccountDescriptor;
import com.tweetlanes.android.core.model.ComposeTweetDefault;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshBase.OnLastItemVisibleListener;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshListView;

import org.tweetalib.android.TwitterConstant.UsersType;
import org.tweetalib.android.TwitterContentHandle;
import org.tweetalib.android.TwitterContentHandleBase;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchUsers.FinishedCallback;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterPaging;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterUser;
import org.tweetalib.android.model.TwitterUsers;

import java.util.ArrayList;

public class UserFeedFragment extends BaseLaneFragment {

    /*
     *
	 */
    public static UserFeedFragment newInstance(int laneIndex,
                                               final TwitterContentHandleBase handleBase, final String screenName,
                                               final String laneIdentifier, final String currentAccountKey) {

        UserFeedFragment fragment = new UserFeedFragment();

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
                    }

                });

        return fragment;
    }

    private PullToRefreshListView mUserFeedListView;
    private UserFeedListAdapter mUserFeedListAdapter;
    private TwitterContentHandle mContentHandle;
    private TwitterUsers _mUsersFeed;
    private FinishedCallback mUserDataRefreshCallback;
    private ViewSwitcher mViewSwitcher;
    private final ArrayList<TwitterUser> mSelectedItems = new ArrayList<TwitterUser>();

    private Long mNewestUserId;
    private Long mRefreshingNewestUserId;
    private Long mOldestUserId;
    private Long mRefreshingUserTweetId;
    private boolean mMoreUsersAvailable = true;

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
        mUserFeedListAdapter = new UserFeedListAdapter(inflater);
        MultipleUserSelectionCallback multipleUserSelectionCallback = new MultipleUserSelectionCallback();
        mUserFeedListView = (PullToRefreshListView) resultView
                .findViewById(R.id.pull_to_refresh_listview);
        mUserFeedListView.getRefreshableView().setOnItemClickListener(
                mOnUserFeedItemClickListener);
        mUserFeedListView.getRefreshableView().setChoiceMode(
                ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mUserFeedListView.getRefreshableView().setMultiChoiceModeListener(
                multipleUserSelectionCallback);
        mUserFeedListView.getRefreshableView().setOnScrollListener(
                mUserFeedOnScrollListener);
        mUserFeedListView.getRefreshableView().setAdapter(mUserFeedListAdapter);
        mUserFeedListView.setOnRefreshListener(mUserFeedOnRefreshListener);
        mUserFeedListView
                .setOnLastItemVisibleListener(mUserFeedOnLastItemVisibleListener);

        //
        //
        //
        TwitterUsers cachedUsers;
        if (mContentHandle.getUsersType() == UsersType.RETWEETED_BY) {
            cachedUsers = TwitterManager.get().getUsers(mContentHandle,
                    TwitterPaging.createGetMostRecent(20));
        } else {
            cachedUsers = TwitterManager.get().getUsers(mContentHandle, null);
        }
        if (cachedUsers != null) {
            setUserFeed(cachedUsers);
        } else {
            setUserFeed(null);
        }

        if (getUserFeed() == null || getUserFeed().getUserCount() == 0) {
            updateViewVisibility(false);
            setInitialDownloadState(InitialDownloadState.WAITING);
        } else {
            setInitialDownloadState(InitialDownloadState.DOWNLOADED);
            updateViewVisibility(true);
        }

        return resultView;
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

    /*
     *
	 */
    private void setUserFeed(TwitterUsers users) {

        mNewestUserId = null;
        mOldestUserId = null;

        if (users == null) {
            _mUsersFeed = null;
        } else {
            _mUsersFeed = new TwitterUsers(users);
            if (users.getUserCount() > 0) {
                mNewestUserId = users.getUser(0).getId();
                mOldestUserId = users.getUser(users.getUserCount() - 1).getId();
            }
        }
    }

    /*
	 *
	 */
    private TwitterUsers getUserFeed() {
        return _mUsersFeed;
    }

    /*
	 *
	 */
    private void onRefreshComplete(TwitterUsers feed) {

        if (feed != null) {
            setUserFeed(feed);
        }
        mUserFeedListAdapter.notifyDataSetChanged();
        mUserDataRefreshCallback = null;
    }

    /*
	 *
	 */
    private void updateViewVisibility(boolean loadHasFinished) {

        mViewSwitcher.reset();

        if (!loadHasFinished
                && (getUserFeed() == null || getUserFeed().getUserCount() == 0)) {
            mViewSwitcher.setDisplayedChild(0);
        } else {
            mViewSwitcher.setDisplayedChild(1);
            mUserFeedListAdapter.notifyDataSetChanged();
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

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#onJumpToTop()
     */
    @Override
    public void onJumpToTop() {
        if (mUserFeedListView != null) {
            ListView listView = mUserFeedListView.getRefreshableView();
            if (listView != null && listView.getAdapter() != null
                    && !listView.getAdapter().isEmpty()) {
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
        _mUsersFeed = null;
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
    private final OnScrollListener mUserFeedOnScrollListener = new OnScrollListener() {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            mUserFeedListView.onScroll(view, firstVisibleItem,
                    visibleItemCount, totalItemCount);
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

            if (scrollState == 0) {
                // Restore scrollbar fading, which may have been set to false
                // when scrolling through items via the volume keys
                view.setScrollbarFadingEnabled(true);
            }

            mUserFeedListView.onScrollStateChanged(view, scrollState);
        }

    };

    /*
	 *
	 */
    private final OnLastItemVisibleListener mUserFeedOnLastItemVisibleListener = new OnLastItemVisibleListener() {

        @Override
        public void onLastItemVisible() {

            mUserDataRefreshCallback = TwitterManager.get()
                    .getFetchUsersInstance().new FinishedCallback() {

                @Override
                public void finished(TwitterFetchResult result,
                                     TwitterUsers feed) {

                    onRefreshComplete(feed);

                    if (mRefreshingUserTweetId.longValue() == mOldestUserId
                            .longValue()) {
                        mMoreUsersAvailable = false;
                    }
                    mRefreshingUserTweetId = null;
                }
            };

            if (mOldestUserId != null) {
                if (mRefreshingUserTweetId == null) {
                    // TODO: Bit of a hack, but does remove an unnecessary fetch
                    // that is triggered here when initializing
                    int count = mUserFeedListAdapter.getCount();
                    if (count > 2) {
                        mRefreshingUserTweetId = mOldestUserId;
                        TwitterManager.get().getUsers(mContentHandle,
                                TwitterPaging.createGetOlder(mOldestUserId),
                                mUserDataRefreshCallback);
                    }
                }
            }
        }
    };

    /*
	 *
	 */
    private final OnRefreshListener mUserFeedOnRefreshListener = new OnRefreshListener() {

        @Override
        public void onRefresh() {

            mUserDataRefreshCallback = TwitterManager.get()
                    .getFetchUsersInstance().new FinishedCallback() {

                @Override
                public void finished(TwitterFetchResult result,
                                     TwitterUsers feed) {

                    onRefreshComplete(feed);
                    mUserFeedListView.onRefreshComplete();
                    mRefreshingNewestUserId = null;
                }
            };

            if (mNewestUserId != null) {
                if (mRefreshingNewestUserId == null) {
                    mRefreshingNewestUserId = mNewestUserId;
                    TwitterUsers users = TwitterManager.get().getUsers(
                            mContentHandle,
                            TwitterPaging.createGetNewer(mNewestUserId),
                            mUserDataRefreshCallback);
                    if (users == null) {
                        getBaseLaneActivity().finishCurrentActionMode();
                    }
                }
            } else {
                TwitterManager.get().getUsers(mContentHandle, null,
                        mUserDataRefreshCallback);
                getBaseLaneActivity().finishCurrentActionMode();
            }
        }
    };

    /*
	 *
	 */
    private final OnItemClickListener mOnUserFeedItemClickListener = new OnItemClickListener() {

        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {

            // Ignore the PTR item
            position -= 1;

            if (position < getUserFeed().getUserCount()) {
                TwitterUser user = getUserFeed().getUser(position);
                Intent profileIntent = new Intent(getActivity(),
                        ProfileActivity.class);
                profileIntent.putExtra("userId", Long.valueOf(user.getId())
                        .toString());
                profileIntent.putExtra("userScreenName", user.getScreenName());
                getActivity().startActivityForResult(profileIntent, Constant.REQUEST_CODE_PROFILE);
            }
        }
    };

    /*
     *
     */
    private class MultipleUserSelectionCallback implements
            ListView.MultiChoiceModeListener {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {

            mode.getMenu().clear();
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.users_selected, mode.getMenu());

            mode.setTitle("Select Users");

            configure(mode);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            final int itemId = item.getItemId();
            if (itemId == R.id.action_retweet || itemId == R.id.action_favorite || itemId == R.id.action_manage_friendship) {
                showToast(getString(R.string.functionality_not_implemented));
                mode.finish();

            } else if (itemId == R.id.action_report_for_spam || itemId == R.id.action_block) {
                AccountDescriptor account = getApp().getCurrentAccount();
                if (account != null) {

                    TwitterUsers selected = getSelectedUsers();
                    if (selected != null && selected.getUserCount() > 0) {

                        ArrayList<Long> userIds = new ArrayList<Long>();
                        for (int i = 0; i < selected.getUserCount(); i++) {
                            userIds.add(selected.getUser(0).getId());
                        }

                        FinishedCallback callback = TwitterManager
                                .get().getFetchUsersInstance().new FinishedCallback() {

                            @Override
                            public void finished(TwitterFetchResult result,
                                                 TwitterUsers users) {
                                getBaseLaneActivity().finishCurrentActionMode();

                                if (getUserFeed() != null) {
                                    getUserFeed().remove(users);
                                }

                                onRefreshComplete(getUserFeed());
                                mUserFeedListView.onRefreshComplete();
                                updateViewVisibility(true);

                                if (result.isSuccessful() && users != null
                                        && users.getUserCount() > 0) {
                                    int userCount = users.getUserCount();
                                    String notice;
                                    if (itemId == R.id.action_report_for_spam) {
                                        if (userCount == 1) {
                                            notice = "Reported @"
                                                    + users.getUser(0)
                                                    .getScreenName()
                                                    + " for Spam.";
                                        } else {
                                            notice = "Reported " + userCount
                                                    + " users for Spam.";
                                        }
                                    } else {
                                        if (userCount == 1) {
                                            notice = "Blocked @"
                                                    + users.getUser(0)
                                                    .getScreenName()
                                                    + ".";
                                        } else {
                                            notice = "Blocked " + userCount
                                                    + " users.";
                                        }
                                    }
                                    if (notice != null) {
                                        showToast(notice);
                                    }
                                }
                            }
                        };

                        if (item.getItemId() == R.id.action_report_for_spam) {
                            TwitterManager.get().reportSpam(account.getId(),
                                    userIds, callback);
                        } else {
                            TwitterManager.get().createBlock(account.getId(),
                                    userIds, callback);
                        }
                    }
                }

            } else {
            }
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
            mSelectedItems.clear();
            if (!getBaseLaneActivity().composeHasFocus()) {
                getBaseLaneActivity().setComposeDefault();
            }
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {
            onUserFeedItemViewClick(position, checked);
            configure(mode);
        }

        void configure(ActionMode mode) {

            BaseLaneActivity baseLaneActivity = (BaseLaneActivity) getActivity();
            if (baseLaneActivity == null || baseLaneActivity.isComposing()
                    || mUserFeedListView == null
                    || mUserFeedListView.getRefreshableView() == null
                    || mode == null) {
                return;
            }

            final int checkedCount = mUserFeedListView.getRefreshableView()
                    .getCheckedItemCount();
            switch (checkedCount) {
                case 0:
                    mode.setSubtitle(null);
                    break;

                default:
                    mode.setSubtitle("" + checkedCount + " selected");
                    break;
            }
        }

    }

    /*
	 *
	 */
    private void onUserFeedItemViewClick(int position, boolean checked) {

        position -= 1; // remove the pull-to-refresh item

        TwitterUser user = getUserFeed().getUser(position);

        for (int index = 0; index < mSelectedItems.size(); index++) {
            TwitterUser selectedUser = mSelectedItems.get(index);
            if (selectedUser != null && user != null) {
                if (selectedUser.getId() == user.getId()) {
                    mSelectedItems.remove(index);
                    break;
                }
            }
        }

        if (checked) {
            mSelectedItems.add(user);
        }

        if (mSelectedItems.size() > 0) {
            getBaseLaneActivity()
                    .setComposeTweetDefault(
                            new ComposeTweetDefault(getApp()
                                    .getCurrentAccountScreenName(),
                                    getSelectedUsers()));
        } else {
            getBaseLaneActivity().setComposeDefault();
        }
    }

    /*
	 *
	 */
    private TwitterUsers getSelectedUsers() {

        TwitterUsers selectedList = new TwitterUsers();

        for (int i = 0; i < mSelectedItems.size(); i++) {
            TwitterUser selectedUser = mSelectedItems.get(i);
            if (selectedUser != null) {
                selectedList.add(selectedUser);
            }
        }

        return selectedList.getUserCount() > 0 ? selectedList : null;
    }

    /*
	 *
	 */
    /*
     * private TwitterUser getFirstSelectedUser() { for (int i = 0; i <
     * mSelectedItems.size(); i++) { UserFeedItemView userFeedItemView =
     * mSelectedItems.get(i); TwitterUser user =
     * userFeedItemView.getTwitterUser(); if (user != null) { return user; } }
     * return null; }
     */

    /*
     *
     */
    public interface UserFeedItemViewCallbacks {

        public Activity getActivity();

        public LazyImageLoader getProfileImageLoader();
    }

    /*
     *
     */
    private class UserFeedListAdapter extends BaseAdapter {

        public UserFeedListAdapter(LayoutInflater inflater) {
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
            if (getUserFeed() != null) {
                count = getUserFeed().getUserCount() + 1; // +1 for the trailing
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

            int userCount = getUserFeed() != null ? getUserFeed()
                    .getUserCount() : 0;

            View resultView;
            if (userCount == 0 && position == getCount() - 1) {
                resultView = getLoadMoreView();
            } else if (position == userCount) {
                resultView = getLoadMoreView();
            } else {
                resultView = getUserFeedItemView(position);
            }

            return resultView;

        }

        /*
         *
         */
        View getUserFeedItemView(int position) {

            View convertView = mInflater.inflate(R.layout.user_feed_item, null);

            TwitterUser user = getUserFeed().getUser(position);

            UserFeedItemView userFeedItemView = (UserFeedItemView) convertView
                    .findViewById(R.id.userFeedItem);

            UserFeedItemViewCallbacks callbacks = new UserFeedItemViewCallbacks() {

                @Override
                public Activity getActivity() {
                    return UserFeedFragment.this.getActivity();
                }

                @Override
                public LazyImageLoader getProfileImageLoader() {
                    return getApp().getProfileImageLoader();
                }

            };

            userFeedItemView.configure(user, position + 1, callbacks);
            return userFeedItemView;

        }

        /*
         *
         */
        View getLoadMoreView() {

            View convertView = mInflater.inflate(R.layout.load_more, null);
            LoadMoreView loadMoreView = (LoadMoreView) convertView
                    .findViewById(R.id.loadMoreView);

            LoadMoreView.Mode mode;
            if (getUserFeed() == null || getUserFeed().getUserCount() == 0) {
                mode = LoadMoreView.Mode.NONE_FOUND;
            } else {
                mode = mMoreUsersAvailable ? LoadMoreView.Mode.LOADING
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

        mUserDataRefreshCallback = TwitterManager.get().getFetchUsersInstance().new FinishedCallback() {

            @Override
            public void finished(TwitterFetchResult fetchResult,
                                 TwitterUsers users) {
                onRefreshComplete(users);
                if (mUserFeedListView != null) {
                    mUserFeedListView.onRefreshComplete();
                }

                updateViewVisibility(true);
                mUserDataRefreshCallback = null;

                setInitialDownloadState(InitialDownloadState.DOWNLOADED);
            }

        };

        TwitterUsers cachedUsers;
        if (mContentHandle.getUsersType() == UsersType.RETWEETED_BY) {
            cachedUsers = TwitterManager.get().getUsers(mContentHandle,
                    TwitterPaging.createGetMostRecent(20),
                    mUserDataRefreshCallback);
        } else {
            cachedUsers = TwitterManager.get().getUsers(mContentHandle, null,
                    mUserDataRefreshCallback);
        }

        if (cachedUsers != null) {
            if (mUserDataRefreshCallback != null) {
                mUserDataRefreshCallback.finished(new TwitterFetchResult(true,
                        null), cachedUsers);
            }
        } else {
            setInitialDownloadState(InitialDownloadState.DOWNLOADING);
        }
    }

}

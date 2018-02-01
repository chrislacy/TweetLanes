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

package org.tweetalib.android;

import android.util.Log;

import org.appdotnet4j.model.AdnUser;
import org.appdotnet4j.model.AdnUsers;
import org.asynctasktex.AsyncTaskEx;
import org.socialnetlib.android.AppdotnetApi;
import org.tweetalib.android.TwitterConstant.UsersType;
import org.tweetalib.android.model.TwitterIds;
import org.tweetalib.android.model.TwitterUser;
import org.tweetalib.android.model.TwitterUsers;

import java.util.ArrayList;
import java.util.HashMap;

import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class TwitterFetchUsers {

    private FetchUsersWorkerCallbacks mWorkerCallbacks;
    private final HashMap<String, TwitterIds> mIdsHashMap;
    private Integer mFetchUsersCallbackHandle;
    private final HashMap<Integer, FinishedCallback> mFinishedCallbackMap;

    /*
     *
	 */
    public void clearCallbacks() {
        if (mFinishedCallbackMap != null) {
            mFinishedCallbackMap.clear();
        }
    }

    /*
     *
	 */
    public interface FetchUsersWorkerCallbacks {

        public Twitter getTwitterInstance();

        public AppdotnetApi getAppdotnetInstance();

        public String getCurrentAccountKey();

        public void addUser(User user);

        public void addUser(AdnUser user);

        public TwitterUser getUser(Long userID);
    }

    /*
     *
	 */
    public interface FinishedCallbackInterface {

        public void finished(TwitterFetchResult result, TwitterUsers users);

    }

    /*
	 *
	 */
    public abstract class FinishedCallback implements FinishedCallbackInterface {

        static final int kInvalidHandle = -1;

        public FinishedCallback() {
            mHandle = kInvalidHandle;
        }

        private int mHandle;
    }

    /*
	 *
	 */
    public TwitterFetchUsers() {
        mFinishedCallbackMap = new HashMap<Integer, FinishedCallback>();
        mFetchUsersCallbackHandle = 0;
        mIdsHashMap = new HashMap<String, TwitterIds>();

    }

    /*
	 *
	 */
    public void setWorkerCallbacks(FetchUsersWorkerCallbacks callbacks) {
        mWorkerCallbacks = callbacks;
    }

    /*
	 *
	 */
    FinishedCallback getFetchUsersCallback(Integer callbackHandle) {
        return mFinishedCallbackMap.get(callbackHandle);
    }

    /*
	 *
	 */
    void removeFetchUsersCallback(FinishedCallback callback) {
        if (mFinishedCallbackMap.containsValue(callback)) {
            mFinishedCallbackMap.remove(callback.mHandle);
        }
    }

    /*
	 *
	 */
    Twitter getTwitterInstance() {
        return mWorkerCallbacks.getTwitterInstance();
    }

    AppdotnetApi getAppdotnetInstance() {
        return mWorkerCallbacks.getAppdotnetInstance();
    }

    /*
	 *
	 */
    TwitterIds setUsers(TwitterContentHandle contentHandle, IDs ids) {
        TwitterIds twitterIds = getUserIds(contentHandle);
        twitterIds.add(ids);
        return twitterIds;
    }

    TwitterIds setUsers(TwitterContentHandle contentHandle, long[] ids) {
        TwitterIds twitterIds = getUserIds(contentHandle);
        twitterIds.add(ids);
        return twitterIds;
    }

    /*
	 *
	 */
    TwitterIds getUserIds(TwitterContentHandle handle) {

        if (mIdsHashMap != null) {
            TwitterIds users = mIdsHashMap.get(handle.getKey());
            if (users == null) {
                mIdsHashMap.put(handle.getKey(), new TwitterIds());
            }

            return users;
        }

        return null;
    }

    /*
	 *
	 */
    public TwitterUsers getUsers(TwitterContentHandle contentHandle,
                                 TwitterPaging paging) {

        TwitterIds ids = getUserIds(contentHandle);

        if (ids == null || ids.getIdCount() == 0) {
            return null;
        }

        TwitterUsers result = new TwitterUsers();
        for (int i = 0; i < ids.getIdCount(); i++) {
            TwitterUser user = mWorkerCallbacks.getUser(ids.getId(i));
            if (user != null) {
                result.add(user);
            }
        }

        return result;
    }

    /*
	 *
	 */
    public TwitterUsers getUsers(TwitterContentHandle contentHandle,
                                 TwitterPaging paging, FinishedCallback callback,
                                 ConnectionStatus connectionStatus) {

        TwitterUsers result = getUsers(contentHandle, paging);
        if (result == null) {
            trigger(contentHandle, paging, callback, connectionStatus);
        } else {
            callback.finished(new TwitterFetchResult(true, null), result);
        }

        return result;
    }

    /*
	 *
	 */
    void trigger(TwitterContentHandle contentHandle,
                 TwitterPaging paging, FinishedCallback callback,
                 ConnectionStatus connectionStatus) {

        if (connectionStatus != null && !connectionStatus.isOnline()) {
            if (callback != null) {
                callback.finished(new TwitterFetchResult(false,
                        connectionStatus.getErrorMessageNoConnection()), null);
            }
            return;
        }

        assert (!mFinishedCallbackMap.containsValue(callback));

        mFinishedCallbackMap.put(mFetchUsersCallbackHandle, callback);
        new FetchUsersTask().execute(AsyncTaskEx.PRIORITY_MEDIUM,
                "Fetch Users", new FetchUsersTaskInput(
                mFetchUsersCallbackHandle, contentHandle,
                connectionStatus, paging));

        mFetchUsersCallbackHandle += 1;
    }

    /*
	 *
	 */
    public void cancel(FinishedCallback callback) {

        removeFetchUsersCallback(callback);
    }

    /*
	 *
	 */
    public void updateFriendshipUser(String currentUserScreenName,
                                     TwitterUser userToUpdate, boolean create,
                                     FinishedCallback callback, ConnectionStatus connectionStatus) {
        updateFriendshipUsers(currentUserScreenName, new TwitterUsers(
                userToUpdate), create, callback, connectionStatus);
    }

    public void updateFriendshipUsers(String currentUserScreenName,
                                      TwitterUsers usersToUpdate, boolean create,
                                      FinishedCallback callback, ConnectionStatus connectionStatus) {
        ArrayList<String> userScreenNames = new ArrayList<String>();
        for (int i = 0; i < usersToUpdate.getUserCount(); i++) {
            userScreenNames.add(usersToUpdate.getUser(i).getScreenName());
        }
        updateFriendshipScreenNames(currentUserScreenName, userScreenNames,
                create, callback, connectionStatus);
    }

    /*
	 *
	 */
    public void updateFriendshipScreenName(String currentUserScreenName,
                                           String screenNameToUpdate, boolean create,
                                           FinishedCallback callback, ConnectionStatus connectionStatus) {
        ArrayList<String> userScreenNames = new ArrayList<String>();
        userScreenNames.add(screenNameToUpdate);
        updateFriendshipScreenNames(currentUserScreenName, userScreenNames,
                create, callback, connectionStatus);
    }

    private static int _mFriendshipCounter = 0;

    public void updateFriendshipScreenNames(String currentUserScreenName,
                                            ArrayList<String> userScreenNamesToUpdate, boolean create,
                                            FinishedCallback callback, ConnectionStatus connectionStatus) {

        if (connectionStatus != null && !connectionStatus.isOnline()) {
            if (callback != null) {
                callback.finished(new TwitterFetchResult(false,
                        connectionStatus.getErrorMessageNoConnection()), null);
            }
            return;
        }

        _mFriendshipCounter += 1;
        TwitterContentHandle contentHandle = new TwitterContentHandle(
                new TwitterContentHandleBase(TwitterConstant.ContentType.USERS,
                        TwitterConstant.UsersType.UPDATE_FRIENDSHIP),
                currentUserScreenName, Integer.toString(_mFriendshipCounter), mWorkerCallbacks.getCurrentAccountKey());

        mFinishedCallbackMap.put(mFetchUsersCallbackHandle, callback);
        new FetchUsersTask().execute(AsyncTaskEx.PRIORITY_MEDIUM,
                "Fetch friendship",
                new FetchUsersTaskInput(mFetchUsersCallbackHandle,
                        contentHandle, connectionStatus,
                        userScreenNamesToUpdate, null, create));
    }

    /*
	 *
	 */
    public void updateFriendshipUserId(long currentUserId, Long userIdToUpdate,
                                       boolean create, FinishedCallback callback,
                                       ConnectionStatus connectionStatus) {
        ArrayList<Long> userIds = new ArrayList<Long>();
        userIds.add(userIdToUpdate);
        updateFriendshipUserIds(currentUserId, userIds, create, callback,
                connectionStatus);
    }

    public void updateFriendshipUserIds(long currentUserId,
                                        ArrayList<Long> userIdsToUpdate, boolean create,
                                        FinishedCallback callback, ConnectionStatus connectionStatus) {
        if (connectionStatus != null && !connectionStatus.isOnline()) {
            if (callback != null) {
                callback.finished(new TwitterFetchResult(false,
                        connectionStatus.getErrorMessageNoConnection()), null);
            }
            return;
        }

        _mFriendshipCounter += 1;
        TwitterContentHandle contentHandle = new TwitterContentHandle(
                new TwitterContentHandleBase(TwitterConstant.ContentType.USERS,
                        TwitterConstant.UsersType.UPDATE_FRIENDSHIP),
                Long.toString(currentUserId),
                Integer.toString(_mFriendshipCounter), mWorkerCallbacks.getCurrentAccountKey());

        mFinishedCallbackMap.put(mFetchUsersCallbackHandle, callback);
        new FetchUsersTask().execute(AsyncTaskEx.PRIORITY_MEDIUM,
                "Update Friendships", new FetchUsersTaskInput(
                mFetchUsersCallbackHandle, contentHandle,
                connectionStatus, null, userIdsToUpdate, create));
    }

    /*
	 *
	 */
    private void createBlockOrReportSpam(UsersType usersType,
                                         long currentUserId, Long userId, FinishedCallback callback,
                                         ConnectionStatus connectionStatus) {
        ArrayList<Long> userIds = new ArrayList<Long>();
        userIds.add(userId);
        createBlockOrReportSpam(usersType, currentUserId, userIds, callback,
                connectionStatus);
    }

    private void createBlockOrReportSpam(UsersType usersType,
                                         long currentUserId, ArrayList<Long> userIds,
                                         FinishedCallback callback, ConnectionStatus connectionStatus) {
        if (connectionStatus != null && !connectionStatus.isOnline()) {
            if (callback != null) {
                callback.finished(new TwitterFetchResult(false,
                        connectionStatus.getErrorMessageNoConnection()), null);
            }
            return;
        }

        _mFriendshipCounter += 1;
        TwitterContentHandle contentHandle = new TwitterContentHandle(
                new TwitterContentHandleBase(TwitterConstant.ContentType.USERS,
                        usersType), Long.toString(currentUserId),
                Integer.toString(_mFriendshipCounter), mWorkerCallbacks.getCurrentAccountKey());

        mFinishedCallbackMap.put(mFetchUsersCallbackHandle, callback);
        new FetchUsersTask().execute(AsyncTaskEx.PRIORITY_MEDIUM,
                "Block or Report", new FetchUsersTaskInput(
                mFetchUsersCallbackHandle, contentHandle,
                connectionStatus, userIds));
    }

    /*
	 *
	 */
    public void reportSpam(long currentUserId, Long userId,
                           FinishedCallback callback, ConnectionStatus connectionStatus) {
        createBlockOrReportSpam(UsersType.REPORT_SPAM, currentUserId, userId,
                callback, connectionStatus);
    }

    public void reportSpam(long currentUserId, ArrayList<Long> userIds,
                           FinishedCallback callback, ConnectionStatus connectionStatus) {
        createBlockOrReportSpam(UsersType.REPORT_SPAM, currentUserId, userIds,
                callback, connectionStatus);
    }

    /*
	 *
	 */
    public void createBlock(long currentUserId, Long userId,
                            FinishedCallback callback, ConnectionStatus connectionStatus) {
        createBlockOrReportSpam(UsersType.CREATE_BLOCK, currentUserId, userId,
                callback, connectionStatus);
    }

    public void createBlock(long currentUserId, ArrayList<Long> userIds,
                            FinishedCallback callback, ConnectionStatus connectionStatus) {
        createBlockOrReportSpam(UsersType.CREATE_BLOCK, currentUserId, userIds,
                callback, connectionStatus);
    }

    /*
	 *
	 */
    class FetchUsersTaskInput {

        FetchUsersTaskInput(Integer callbackHandle,
                            TwitterContentHandle contentHandle,
                            ConnectionStatus connectionStatus, TwitterPaging paging) {
            mCallbackHandle = callbackHandle;
            mContentHandle = contentHandle;
            mConnectionStatus = connectionStatus;
            mPaging = paging;
        }

        FetchUsersTaskInput(Integer callbackHandle,
                            TwitterContentHandle contentHandle,
                            ConnectionStatus connectionStatus, ArrayList<Long> userIds) {
            mCallbackHandle = callbackHandle;
            mContentHandle = contentHandle;
            mConnectionStatus = connectionStatus;
            mUserIds = userIds != null ? new ArrayList<Long>(userIds) : null;
        }

        FetchUsersTaskInput(Integer callbackHandle,
                            TwitterContentHandle contentHandle,
                            ConnectionStatus connectionStatus,
                            ArrayList<String> userScreenNames, ArrayList<Long> userIds,
                            boolean createFriendship) {
            mCallbackHandle = callbackHandle;
            mContentHandle = contentHandle;
            mConnectionStatus = connectionStatus;
            mScreenNames = userScreenNames != null ? new ArrayList<String>(
                    userScreenNames) : null;
            mUserIds = userIds != null ? new ArrayList<Long>(userIds) : null;
            mCreateFriendship = createFriendship;
        }

        final Integer mCallbackHandle;
        final TwitterContentHandle mContentHandle;
        TwitterPaging mPaging;
        final ConnectionStatus mConnectionStatus;
        ArrayList<String> mScreenNames;
        ArrayList<Long> mUserIds;
        boolean mCreateFriendship;
    }

    /*
	 *
	 */
    class FetchUsersTaskOutput {

        FetchUsersTaskOutput(TwitterFetchResult result, Integer callbackHandle,
                             TwitterUsers users) {
            mResult = result;
            mCallbackHandle = callbackHandle;
            mUsers = users;
        }

        final TwitterFetchResult mResult;
        final Integer mCallbackHandle;
        final TwitterUsers mUsers;
    }

    /*
	 *
	 */
    class FetchUsersTask extends
            AsyncTaskEx<FetchUsersTaskInput, Void, FetchUsersTaskOutput> {

        @Override
        protected FetchUsersTaskOutput doInBackground(
                FetchUsersTaskInput... inputArray) {

            FetchUsersTaskInput input = inputArray[0];
            TwitterUsers twitterUsers = null;
            Twitter twitter = getTwitterInstance();
            AppdotnetApi appdotnet = getAppdotnetInstance();
            String errorDescription = null;

            if (input.mConnectionStatus != null && !input.mConnectionStatus.isOnline()) {
                return new FetchUsersTaskOutput(new TwitterFetchResult(false,
                        input.mConnectionStatus.getErrorMessageNoConnection()),
                        input.mCallbackHandle, null);
            }

            UsersType usersType = input.mContentHandle.getUsersType();

            if (appdotnet != null) {
                long[] ids = null;
                AdnUsers users = null;

                switch (usersType) {
                    case FRIENDS: {
                        ids = appdotnet.getAdnFollowing();
                        setUsers(input.mContentHandle, ids);
                        break;
                    }

                    case FOLLOWERS: {
                        ids = appdotnet.getAdnFollowedBy();
                        setUsers(input.mContentHandle, ids);
                        break;
                    }

                    case RETWEETED_BY: {
                        try {
                            long postId = Long.valueOf(input.mContentHandle
                                    .getIdentifier());
                            users = appdotnet.getUsersWhoReposted(postId);
                            setUsers(input.mContentHandle, ids);
                        } catch (NumberFormatException e) {
                        }
                        break;
                    }

                    case UPDATE_FRIENDSHIP: {
                        twitterUsers = new TwitterUsers();

                        if (input.mScreenNames != null) {
                            for (String screenName : input.mScreenNames) {
                                AdnUser user = null;
                                // We can't follow ourself...
                                if (!screenName.toLowerCase().equals(
                                        input.mContentHandle.getScreenName()
                                                .toLowerCase())) {
                                    if (input.mCreateFriendship) {
                                        user = appdotnet.setAdnFollow(screenName,
                                                true);
                                    } else {
                                        user = appdotnet.setAdnFollow(screenName,
                                                false);
                                    }
                                }
                                if (user != null) {
                                    twitterUsers.add(new TwitterUser(user));
                                }
                            }
                        } else if (input.mUserIds != null) {

                            long currentUserId = Long
                                    .parseLong(input.mContentHandle.getScreenName());

                            for (Long userId : input.mUserIds) {
                                AdnUser user = null;
                                // We can't follow ourself...
                                if (currentUserId != userId) {
                                    if (input.mCreateFriendship) {
                                        user = appdotnet.setAdnFollow(userId, true);
                                    } else {
                                        user = appdotnet
                                                .setAdnFollow(userId, false);
                                    }
                                }
                                if (user != null) {
                                    twitterUsers.add(new TwitterUser(user));
                                }
                            }
                        }
                    }

                }

                if (ids != null) {
                    int max = input.mPaging == null ? 40 : input.mPaging.getCount();
                    int numberToFetch = Math.min(max, ids.length);
                    long[] longArray = new long[numberToFetch];
                    System.arraycopy(ids, 0, longArray, 0, numberToFetch);

                    users = appdotnet.getAdnMultipleUsers(longArray);
                }
                if (users != null && users.mUsers != null
                        && users.mUsers.size() > 0 && twitterUsers == null) {
                    twitterUsers = new TwitterUsers();
                    for (AdnUser user : users.mUsers) {
                        mWorkerCallbacks.addUser(user);
                        twitterUsers.add(new TwitterUser(user));
                    }
                }

                return new FetchUsersTaskOutput(new TwitterFetchResult(
                        errorDescription == null,
                        errorDescription), input.mCallbackHandle, twitterUsers);

            } else if (twitter != null) {

                IDs userIds = null;
                ResponseList<User> users = null;
                Paging paging = null;
                if (input.mPaging != null) {
                    paging = input.mPaging.getT4JPaging();
                }

                try {
                    switch (usersType) {
                        case FRIENDS: {
                            Log.d("api-call", "getFriendsIDs");
                            userIds = twitter.getFriendsIDs(-1);
                            setUsers(input.mContentHandle, userIds);
                            break;
                        }

                        case FOLLOWERS: {
                            Log.d("api-call", "getFollowersIDs");
                            userIds = twitter.getFollowersIDs(-1);
                            setUsers(input.mContentHandle, userIds);
                            break;
                        }

                        case RETWEETED_BY: {
                            Log.d("api-call", "getRetweets");
                            long statusId = Long.parseLong(input.mContentHandle
                                    .getIdentifier());
                            ResponseList<twitter4j.Status> statuses = twitter
                                    .getRetweets(statusId);

                            if (statuses != null) {
                                twitterUsers = new TwitterUsers();
                                for (twitter4j.Status status : statuses) {
                                    mWorkerCallbacks.addUser(status.getUser());
                                    twitterUsers.add(new TwitterUser(status
                                            .getUser()));
                                }
                            }
                            break;
                        }

                        case PEOPLE_SEARCH: {
                            Log.d("api-call", "searchUsers");
                            String searchTerm = input.mContentHandle
                                    .getScreenName();
                            users = twitter.searchUsers(searchTerm, 0);
                            break;
                        }

                        case UPDATE_FRIENDSHIP: {
                            twitterUsers = new TwitterUsers();

                            if (input.mScreenNames != null) {
                                for (String screenName : input.mScreenNames) {
                                    User user = null;
                                    // We can't follow ourself...
                                    if (!screenName.toLowerCase().equals(
                                            input.mContentHandle.getScreenName()
                                                    .toLowerCase())) {
                                        if (input.mCreateFriendship) {
                                            Log.d("api-call", "createFriendship");
                                            user = twitter
                                                    .createFriendship(screenName);
                                        } else {
                                            Log.d("api-call", "destroyFriendship");
                                            user = twitter
                                                    .destroyFriendship(screenName);
                                        }
                                    }
                                    if (user != null) {
                                        twitterUsers.add(new TwitterUser(user));
                                    }
                                }
                            } else if (input.mUserIds != null) {

                                long currentUserId = Long
                                        .parseLong(input.mContentHandle
                                                .getScreenName());

                                for (Long userId : input.mUserIds) {
                                    User user = null;
                                    // We can't follow ourself...
                                    if (currentUserId != userId) {
                                        if (input.mCreateFriendship) {
                                            Log.d("api-call", "createFriendship");
                                            user = twitter.createFriendship(userId);
                                        } else {
                                            Log.d("api-call", "destroyFriendship");
                                            user = twitter
                                                    .destroyFriendship(userId);
                                        }
                                    }
                                    if (user != null) {
                                        twitterUsers.add(new TwitterUser(user));
                                    }
                                }
                            }

                            if (twitterUsers.getUserCount() == 0) {
                                twitterUsers = null;
                            }

                            break;
                        }

                        case CREATE_BLOCK:
                        case REPORT_SPAM: {
                            twitterUsers = new TwitterUsers();
                            long currentUserId = Long
                                    .parseLong(input.mContentHandle.getScreenName());
                            for (Long userId : input.mUserIds) {
                                User user = null;
                                // We can't act on ourself...
                                if (currentUserId != userId) {
                                    if (usersType == UsersType.CREATE_BLOCK) {
                                        Log.d("api-call", "createBlock");
                                        user = twitter.createBlock(userId);
                                    } else if (usersType == UsersType.REPORT_SPAM) {
                                        Log.d("api-call", "reportSpam");
                                        user = twitter.reportSpam(userId);
                                    }
                                    if (user != null) {
                                        twitterUsers.add(new TwitterUser(user));
                                    }
                                }
                            }

                            if (twitterUsers.getUserCount() == 0) {
                                twitterUsers = null;
                            }

                            break;
                        }

                    }

                    if (userIds != null) {
                        long[] ids = userIds.getIDs();
                        int numberToFetch = paging == null ? 40 : paging.getCount();
                        int start = 0;
                        int finish = numberToFetch;
                        ArrayList<Long> fetchIds = new ArrayList<Long>();
                        boolean check = true;
                        while (check) {
                            //Establish ids for this batch
                            for (int i = start; i < finish; i++) {

                                if (ids.length == 0 || ids.length - 1 == i) {
                                    check = false;
                                    break;
                                }

                                fetchIds.add(ids[i]);
                            }

                            //Mark where to start and end next time round
                            start = start + numberToFetch;
                            finish = finish + numberToFetch;

                            //Convert arraylist into long[]
                            long[] longArray = new long[fetchIds.size()];
                            for (int i = 0; i < fetchIds.size(); i++) {
                                longArray[i] = fetchIds.get(i);
                            }
                            fetchIds.clear();

                            //Get this batch of users
                            if (users == null) {
                                users = twitter.lookupUsers(longArray);
                            } else {
                                users.addAll(twitter.lookupUsers(longArray));
                            }
                        }
                    }

                } catch (TwitterException e) {
                    e.printStackTrace();
                    errorDescription = e.getErrorMessage();
                    Log.e("api-call", errorDescription, e);
                    if (e.getRateLimitStatus() != null && e.getRateLimitStatus().getRemaining() <= 0) {
                        errorDescription += "\nTry again in " + e.getRateLimitStatus().getSecondsUntilReset()
                                + " " + "seconds";
                    }
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    errorDescription = e.getMessage();
                    Log.e("api-call", errorDescription, e);
                }

                if (users != null && twitterUsers == null) {
                    twitterUsers = new TwitterUsers();
                    for (User user : users) {
                        mWorkerCallbacks.addUser(user);
                        twitterUsers.add(new TwitterUser(user));
                    }
                }
                return new FetchUsersTaskOutput(new TwitterFetchResult(
                        errorDescription == null,
                        errorDescription), input.mCallbackHandle, twitterUsers);
            }

            return new FetchUsersTaskOutput(new TwitterFetchResult(
                    errorDescription == null, errorDescription),
                    input.mCallbackHandle, twitterUsers);
        }

        @Override
        protected void onPostExecute(FetchUsersTaskOutput output) {

            FinishedCallback callback = getFetchUsersCallback(output.mCallbackHandle);
            if (callback != null) {
                callback.finished(output.mResult, output.mUsers);
                removeFetchUsersCallback(callback);
            }

            super.onPostExecute(output);
        }
    }

}

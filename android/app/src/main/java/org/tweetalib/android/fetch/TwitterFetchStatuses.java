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

package org.tweetalib.android.fetch;

import android.util.Log;

import org.appdotnet4j.model.AdnInteractions;
import org.appdotnet4j.model.AdnPaging;
import org.appdotnet4j.model.AdnPost;
import org.appdotnet4j.model.AdnPosts;
import org.appdotnet4j.model.AdnUser;
import org.asynctasktex.AsyncTaskEx;
import org.socialnetlib.android.AppdotnetApi;
import org.tweetalib.android.ConnectionStatus;
import org.tweetalib.android.TwitterContentHandle;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterPaging;
import org.tweetalib.android.TwitterUtil;
import org.tweetalib.android.callback.TwitterFetchStatusesFinishedCallback;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatuses;
import org.tweetalib.android.model.TwitterStatuses.AddUserCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class TwitterFetchStatuses {

    private FetchStatusesWorkerCallbacks mCallbacks;
    private final HashMap<String, TwitterStatuses> mStatusesHashMap;
    private Integer mFetchStatusesCallbackHandle;
    private final HashMap<Integer, TwitterFetchStatusesFinishedCallback> mFinishedCallbackMap;
    private final HashMap<String, String> mHashtagMap;

    /*
     *
	 */
    public void clearCallbacks() {
        if (mFinishedCallbackMap != null) {
            for (Integer key : mFinishedCallbackMap.keySet()) {
                TwitterFetchStatusesFinishedCallback callback = mFinishedCallbackMap.get(key);
                cancel(callback);
            }
            mFinishedCallbackMap.clear();
        }
    }

    /*
     *
	 */
    public interface FetchStatusesWorkerCallbacks {

        public AppdotnetApi getAppdotnetApi();

        public Twitter getTwitterInstance();

        public void addUser(User user);

        public void addUser(AdnUser user);
    }

    /*
     *
	 */
    public TwitterFetchStatuses() {
        mFinishedCallbackMap = new HashMap<Integer, TwitterFetchStatusesFinishedCallback>();
        mFetchStatusesCallbackHandle = 0;
        mStatusesHashMap = new HashMap<String, TwitterStatuses>();
        mHashtagMap = new HashMap<String, String>();
    }

    /*
     *
	 */
    public void setWorkerCallbacks(FetchStatusesWorkerCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    /*
     *
	 */

    /*
     *
	 */
    TwitterFetchStatusesFinishedCallback getFetchStatusesCallback(Integer callbackHandle) {
        return mFinishedCallbackMap.get(callbackHandle);
    }

    /*
     *
	 */
    void removeFetchStatusesCallback(TwitterFetchStatusesFinishedCallback callback) {
        if (mFinishedCallbackMap.containsValue(callback)) {
            mFinishedCallbackMap.remove(callback.getHandle());
        }
    }

    /*
     *
	 */
    Twitter getTwitterInstance() {
        return mCallbacks.getTwitterInstance();
    }

    AppdotnetApi getAppdotnetApi() {
        return mCallbacks.getAppdotnetApi();
    }

    /*
     *
	 */
    TwitterStatuses setStatuses(TwitterContentHandle contentHandle, QueryResult result) {
        TwitterStatuses feed = getStatuses(contentHandle);
        feed.add(result);
        return feed;
    }

    /*
     *
	 */
    TwitterStatuses setStatuses(TwitterContentHandle contentHandle, ResponseList<twitter4j.Status> statuses) {
        TwitterStatuses feed = getStatuses(contentHandle);
        AddUserCallback addUserCallback = new AddUserCallback() {

            @Override
            public void addUser(User user) {
                mCallbacks.addUser(user);
            }

            @Override
            public void addUser(AdnUser user) {
                mCallbacks.addUser(user);
            }
        };

        if (feed == null) {
            feed = new TwitterStatuses();
        }
        feed.add(statuses, addUserCallback);
        return feed;
    }

    /*
	 *
	 */
    TwitterStatuses setStatuses(TwitterContentHandle contentHandle, AdnPosts posts) {
        TwitterStatuses feed = getStatuses(contentHandle);
        AddUserCallback addUserCallback = new AddUserCallback() {

            @Override
            public void addUser(User user) {
                mCallbacks.addUser(user);
            }

            @Override
            public void addUser(AdnUser user) {
                mCallbacks.addUser(user);
            }
        };

        if (feed == null) {
            feed = new TwitterStatuses();
        }

        if (posts != null && posts.mPosts != null && posts.mPosts.size() > 0) {
            feed.add(posts, addUserCallback);
        } else {
            feed.setFeedFullyRefreshed();
        }
        return feed;
    }

    /*
	 *
	 */
    public TwitterStatuses setStatuses(TwitterContentHandle contentHandle, TwitterStatuses statuses,
                                       boolean resetExisting) {
        TwitterStatuses feed = getStatuses(contentHandle);
        if (resetExisting) {
            feed.reset();
        }
        feed.add(statuses);
        return feed;
    }

    /*
	 *
	 */
    public TwitterStatuses getStatuses(TwitterContentHandle handle) {
        if (mStatusesHashMap != null) {
            TwitterStatuses feed = mStatusesHashMap.get(handle.getKey());
            if (feed == null) {
                mStatusesHashMap.put(handle.getKey(), new TwitterStatuses());
            }

            // TODO: This is a bug right? No feed will be returned when it is
            // created??
            return feed;
        }

        return null;
    }

    public void removeFromHashMap(TwitterStatuses removeStatuses) {
        if (mStatusesHashMap != null) {

            for (String key : mStatusesHashMap.keySet()) {
                TwitterStatuses feed = mStatusesHashMap.get(key);
                feed.remove(removeStatuses);
            }
        }
    }

    /*
	 *
	 */
    public void trigger(TwitterContentHandle contentHandle, TwitterPaging paging,
                        TwitterFetchStatusesFinishedCallback callback, ConnectionStatus connectionStatus, int priorityOffset) {

        if (connectionStatus != null && !connectionStatus.isOnline()) {
            if (callback != null) {
                callback.finished(new TwitterFetchResult(false, connectionStatus.getErrorMessageNoConnection()),
                        null, contentHandle);
            }
            return;
        }

        if (mFinishedCallbackMap.containsValue(callback)) {
            throw new RuntimeException("Shouldn't be");
        }

        mFinishedCallbackMap.put(mFetchStatusesCallbackHandle, callback);
        new FetchStatusesTask().execute(AsyncTaskEx.PRIORITY_NOT_QUITE_HIGHEST + priorityOffset, "Fetch Statuses",
                new FetchStatusesTaskInput(mFetchStatusesCallbackHandle, contentHandle, paging, connectionStatus));

        mFetchStatusesCallbackHandle += 1;
    }

    /*
	 *
	 */
    public void cancel(TwitterFetchStatusesFinishedCallback callback) {

        removeFetchStatusesCallback(callback);
    }

    /*
	 *
	 */
    class FetchStatusesTaskInput {

        FetchStatusesTaskInput(Integer callbackHandle, TwitterContentHandle contentHandle, TwitterPaging paging,
                               ConnectionStatus connectionStatus) {
            mCallbackHandle = callbackHandle;
            mContentHandle = contentHandle;
            mPaging = paging;
            mConnectionStatus = connectionStatus;
        }

        final Integer mCallbackHandle;
        final TwitterContentHandle mContentHandle;
        final TwitterPaging mPaging;
        final ConnectionStatus mConnectionStatus;
    }

    /*
	 *
	 */
    class FetchStatusesTaskOutput {

        FetchStatusesTaskOutput(TwitterFetchResult result, Integer callbackHandle, TwitterStatuses feed,
                                TwitterContentHandle contentHandle) {
            mResult = result;
            mCallbackHandle = callbackHandle;
            mContentHandle = contentHandle;
            mFeed = feed;
        }

        final TwitterFetchResult mResult;
        final Integer mCallbackHandle;
        final TwitterContentHandle mContentHandle;
        final TwitterStatuses mFeed;
    }

    /*
	 *
	 */
    class FetchStatusesTask extends AsyncTaskEx<FetchStatusesTaskInput, Void, FetchStatusesTaskOutput> {

        @Override
        protected FetchStatusesTaskOutput doInBackground(FetchStatusesTaskInput... inputArray) {

            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            TwitterStatuses contentFeed = null;
            FetchStatusesTaskInput input = inputArray[0];
            String errorDescription = null;

            if (input.mConnectionStatus != null && !input.mConnectionStatus.isOnline()) {
                return new FetchStatusesTaskOutput(
                        new TwitterFetchResult(false, input.mConnectionStatus.getErrorMessageNoConnection()),
                        input.mCallbackHandle, null, input.mContentHandle);
            }

            AppdotnetApi appdotnetApi = getAppdotnetApi();
            if (appdotnetApi != null) {

                AdnPaging defaultPaging = new AdnPaging(1);
                AdnPaging paging;

                if (input.mPaging != null) {
                    paging = input.mPaging.getAdnPaging();
                } else {
                    paging = defaultPaging;
                }

                switch (input.mContentHandle.getStatusesType()) {
                    case USER_HOME_TIMELINE: {
                        AdnPosts posts = appdotnetApi.getAdnStream(paging);
                        contentFeed = setStatuses(input.mContentHandle, posts);
                        break;
                    }

                    case USER_TIMELINE: {
                        String userIdAsString = input.mContentHandle.getIdentifier();
                        try {
                            int userId = Integer.valueOf(userIdAsString);
                            AdnPosts posts = appdotnetApi.getAdnUserStream(userId, paging);
                            contentFeed = setStatuses(input.mContentHandle, posts);
                        } catch (NumberFormatException e) {
                        }
                        break;
                    }

                    case RETWEETS_OF_ME: {
                        AdnInteractions interactions = appdotnetApi.getAdnInteractions();
                        AdnPosts posts = null;
                        if (interactions != null) {
                            posts = interactions.getAsPosts();
                        }
                        contentFeed = setStatuses(input.mContentHandle, posts);
                        break;
                    }

                    case SCREEN_NAME_SEARCH:
                    case USER_MENTIONS: {
                        String userIdAsString = input.mContentHandle.getIdentifier();
                        try {
                            int userId = Integer.valueOf(userIdAsString);
                            AdnPosts posts = appdotnetApi.getAdnMentions(userId, paging);
                            contentFeed = setStatuses(input.mContentHandle, posts);
                        } catch (NumberFormatException e) {
                        }
                        break;
                    }

                    case USER_FAVORITES: {
                        String userIdAsString = input.mContentHandle.getIdentifier();
                        AdnPosts posts = appdotnetApi.getAdnFavorites(userIdAsString, paging);
                        contentFeed = setStatuses(input.mContentHandle, posts);
                        break;
                    }

                    case STATUS_SEARCH: {
                        String searchTerm = input.mContentHandle.getScreenName();
                        if (searchTerm.length() > 1 && searchTerm.charAt(0) == '#') {
                            searchTerm = searchTerm.substring(1);
                        }
                        AdnPosts posts = appdotnetApi.getAdnTagPosts(searchTerm, paging);
                        contentFeed = setStatuses(input.mContentHandle, posts);
                        break;
                    }

                    case GLOBAL_FEED: {
                        AdnPosts posts = appdotnetApi.getAdnGlobalStream(paging);
                        contentFeed = setStatuses(input.mContentHandle, posts);
                        break;
                    }

                    case PREVIOUS_CONVERSATION: {
                        TwitterStatuses statuses = new TwitterStatuses();
                        long statusId = Long.parseLong(input.mContentHandle.getIdentifier());
                        AdnPost post = appdotnetApi.getAdnPost(statusId);
                        if (post != null) {
                            TwitterStatus status = new TwitterStatus(post);
                            if (status.mInReplyToStatusId != null) {
                                long inReplyToStatusId = status.mInReplyToStatusId;
                                for (int i = 0; i < 4; i++) {
                                    TwitterStatus reply = new TwitterStatus(appdotnetApi.getAdnPost(inReplyToStatusId));
                                    statuses.add(reply, false);
                                    if (reply.mInReplyToStatusId != null) {
                                        inReplyToStatusId = reply.mInReplyToStatusId;
                                    } else {
                                        break;
                                    }
                                }
                            }

                            statuses.add(status, false);
                            if (statuses.getStatusCount() > 0) {
                                statuses.sort();
                                contentFeed = setStatuses(input.mContentHandle, statuses, false);
                            }
                        }
                    }

                    case FULL_CONVERSATION: {
                        long statusId = Long.parseLong(input.mContentHandle.getIdentifier());

                        AddUserCallback addUserCallback = new AddUserCallback() {

                            @Override
                            public void addUser(User user) {
                                mCallbacks.addUser(user);
                            }

                            @Override
                            public void addUser(AdnUser user) {
                                mCallbacks.addUser(user);
                            }
                        };

                        AdnPosts conversation = appdotnetApi.getAdnConversation(statusId, paging);
                        if (conversation != null && conversation.mPosts != null && conversation.mPosts.size() > 0) {
                            TwitterStatuses statuses = new TwitterStatuses();
                            statuses.add(conversation, addUserCallback);
                            contentFeed = setStatuses(input.mContentHandle, statuses, true);
                        }
                    }
                    break;

                    default:
                        break;
                }
            } else {

                Twitter twitter = getTwitterInstance();
                if (twitter != null) {

                    Paging defaultPaging = new Paging(1);
                    defaultPaging.setCount(TwitterPaging.DEFAULT_STATUS_COUNT);
                    Paging paging;
                    if (input.mPaging != null) {
                        paging = input.mPaging.getT4JPaging();
                    } else {
                        paging = defaultPaging;
                    }

                    try {
                        switch (input.mContentHandle.getStatusesType()) {
                            case USER_HOME_TIMELINE: {
                                Log.d("api-call", "getHomeTimeline");
                                ResponseList<twitter4j.Status> statuses;
                                statuses = twitter.getHomeTimeline(paging);
                                contentFeed = setStatuses(input.mContentHandle, statuses);
                                break;
                            }

                            case USER_TIMELINE: {
                                Log.d("api-call", "getUserTimeline");
                                ResponseList<twitter4j.Status> statuses =
                                        twitter.getUserTimeline(input.mContentHandle.getScreenName(), paging);
                                contentFeed = setStatuses(input.mContentHandle, statuses);
                                break;
                            }

                            case USER_MENTIONS: {
                                Log.d("api-call", "getMentionsTimeline");
                                ResponseList<twitter4j.Status> statuses = twitter.getMentionsTimeline(paging);
                                contentFeed = setStatuses(input.mContentHandle, statuses);
                                break;
                            }

                            case USER_LIST_TIMELINE: {
                                String listIdAsString = input.mContentHandle.getIdentifier();
                                try {
                                    Log.d("api-call", "getUserListStatuses");
                                    int listId = Integer.valueOf(listIdAsString);
                                    ResponseList<twitter4j.Status> statuses = twitter.getUserListStatuses(listId, paging);
                                    contentFeed = setStatuses(input.mContentHandle, statuses);
                                } catch (NumberFormatException e) {
                                } catch (OutOfMemoryError e) {
                                    errorDescription = "There was an out of memory doing that!";
                                }
                                break;
                            }

                            case USER_FAVORITES: {
                                Log.d("api-call", "getFavorites");
                                ResponseList<twitter4j.Status> statuses =
                                        twitter.getFavorites(input.mContentHandle.getScreenName(), paging);
                                contentFeed = setStatuses(input.mContentHandle, statuses);
                                break;
                            }

                            case RETWEETS_OF_ME: {
                                Log.d("api-call", "getRetweetsOfMe");
                                ResponseList<twitter4j.Status> statuses = twitter.getRetweetsOfMe(paging);
                                contentFeed = setStatuses(input.mContentHandle, statuses);
                                break;
                            }

                            case SCREEN_NAME_SEARCH: {
                                Log.d("api-call", "search");
                                Query query = new Query("@" + input.mContentHandle.getScreenName());
                                query = TwitterUtil.updateQueryWithPaging(query, paging);
                                QueryResult result = twitter.search(query);
                                contentFeed = setStatuses(input.mContentHandle, result);
                                break;
                            }

                            case STATUS_SEARCH: {
                                Log.d("api-call", "search");
                                Query query = new Query(input.mContentHandle.getScreenName());
                                query = TwitterUtil.updateQueryWithPaging(query, paging);
                                QueryResult result = twitter.search(query);
                                contentFeed = setStatuses(input.mContentHandle, result);
                                break;
                            }


                            case PREVIOUS_CONVERSATION:
                            case FULL_CONVERSATION: {
                                Log.d("api-call", "showStatus");
                                TwitterStatuses statuses = new TwitterStatuses();
                                long statusId = Long.parseLong(input.mContentHandle.getIdentifier());
                                TwitterStatus status = new TwitterStatus(twitter.showStatus(statusId));
                                if (status.mInReplyToStatusId != null) {
                                    long inReplyToStatusId = status.mInReplyToStatusId;
                                    for (int i = 0; i < 4; i++) {
                                        Log.d("api-call", "showStatus");
                                        TwitterStatus reply = new TwitterStatus(twitter.showStatus(inReplyToStatusId));
                                        statuses.add(reply, false);
                                        if (reply.mInReplyToStatusId != null) {
                                            inReplyToStatusId = reply.mInReplyToStatusId;
                                        } else {
                                            break;
                                        }
                                    }
                                }

                                statuses.add(status, false);

                                if (statuses.getStatusCount() > 0) {
                                    statuses.sort();
                                    contentFeed = setStatuses(input.mContentHandle, statuses, true);
                                }

                                break;
                            }

                            default:
                                break;
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
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorDescription = e.getMessage();
                        Log.e("api-call", errorDescription, e);
                    }
                }
            }

            cacheHashtags(contentFeed);

            return new FetchStatusesTaskOutput(
                    new TwitterFetchResult(errorDescription == null, errorDescription),
                    input.mCallbackHandle, contentFeed, input.mContentHandle);
        }

        @Override
        protected void onPostExecute(FetchStatusesTaskOutput output) {

            TwitterFetchStatusesFinishedCallback callback = getFetchStatusesCallback(output.mCallbackHandle);
            if (callback != null) {
                callback.finished(output.mResult, output.mFeed, output.mContentHandle);
                removeFetchStatusesCallback(callback);
            }

            super.onPostExecute(output);
        }
    }

    public List<String> getCachedHashtags() {
        if (mHashtagMap == null) {
            return null;
        }
        return new ArrayList<String>(mHashtagMap.values());
    }

    public void cacheHashtags(TwitterStatuses statuses) {
        if (statuses == null) {
            return;
        }

        for (int i = 0; i < statuses.getStatusCount(); ++i) {
            for (String hastag : statuses.getStatus(i).getHashtags()) {
                mHashtagMap.put(hastag, hastag);
            }
        }
    }

}

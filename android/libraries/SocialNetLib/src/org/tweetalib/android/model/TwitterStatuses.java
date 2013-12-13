/*
 * Copyright (C) 2013 Chris Lacy
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.tweetalib.android.model;

import org.appdotnet4j.model.AdnPost;
import org.appdotnet4j.model.AdnPosts;
import org.appdotnet4j.model.AdnUser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tweetalib.android.model.TwitterStatusesFilter.FilterType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.User;

public class TwitterStatuses {

    /*
     *
	 */
    public TwitterStatuses(TwitterStatuses another) {
        _mStatuses = new ArrayList<TwitterStatus>(another._mStatuses);
        mCounts = another.mCounts.clone();
        mGetNewStatusesMaxId = another.mGetNewStatusesMaxId;
    }

    /*
     *
	 */
    public TwitterStatuses() {
        _mStatuses = new ArrayList<TwitterStatus>();
    }

    /*
	 * 
	 */
    public TwitterStatuses(TwitterStatus status) {
        this();
        add(status);
    }

    /*
	 * 
	 */
    private static final String KEY_STATUSES = "statuses";

    public TwitterStatuses(String jsonAsString) {
        _mStatuses = new ArrayList<TwitterStatus>();

        try {
            JSONObject object = new JSONObject(jsonAsString);
            if (object.has(KEY_STATUSES)) {
                String statusesAsString = object.getString(KEY_STATUSES);
                JSONArray jsonArray = new JSONArray(statusesAsString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    String statusString = jsonArray.getString(i);
                    TwitterStatus status = new TwitterStatus(statusString);
                    add(status);
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
	 * 
	 */
    public String toString() {
        if (_mStatuses != null && _mStatuses.size() > 0) {
            JSONObject object = new JSONObject();
            JSONArray statusArray = new JSONArray();
            for (TwitterStatus status : _mStatuses) {
                statusArray.put(status.toString());
            }
            try {
                object.put(KEY_STATUSES, statusArray);
                return object.toString();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return null;
    }

    /*
	 * 
	 */
    public int getStatusCount() {
        return size();
    }

    public int getStatusCount(TwitterStatusesFilter filter) {
        int size = size();
        FilterType filterType = filter.getFilterType();
        if (filterType != FilterType.ALL) {
            int filteredOut = mCounts[filterType.ordinal()];
            size -= filteredOut;
        }
        return size;
    }

    /*
	 * 
	 */
    public TwitterStatus getStatus(int index) {
        return get(index);
    }

    /*
	 * 
	 */
    public TwitterStatus getStatus(int index, TwitterStatusesFilter filter) {

        FilterType filterType = filter.getFilterType();
        if (filterType == FilterType.ALL) {
            return getStatus(index);
        }

        int size = size();
        int filteredIndex = 0;
        // int filterCount = 0;
        for (int i = 0; i < size; i++) {
            TwitterStatus status = get(i);

            if (status != null) {

                boolean isReply = status.mInReplyToStatusId != null;
                boolean isRetweet = status.mIsRetweet;

                switch (filterType) {
                    case HIDE_RETWEETS:
                        if (isRetweet) {
                            // filterCount += 1;
                            continue;
                        }
                        break;
                    case HIDE_REPLIES:
                        if (isReply) {
                            // filterCount += 1;
                            continue;
                        }
                        break;
                    case HIDE_RETWEETS_REPLIES:
                        if (isReply || isRetweet) {
                            // filterCount += 1;
                            continue;
                        }
                        break;

                    default:
                        break;
                }

                if (filteredIndex == index) {
                    return status;
                }
                filteredIndex += 1;
            }
        }

        // Log.d("done", "done (" + filterCount + ")");
        return null;
    }

    /*
	 * 
	 */
    /*
     * public TwitterStatus getStatus(Iterator iterator) { if
     * (iterator.finished() == false) { return get(iterator.mLastIndex); }
     * return null; }
     */

    /*
	 * 
	 */
    public void add(TwitterStatus status) {
        // TODO: Ensure no duplicates
        _mStatuses.add(status);
        updateCount(status);
    }

    /*
	 * 
	 */
    private void updateCount(TwitterStatus status) {

        if (status.mIsRetweet) {
            mCounts[FilterType.HIDE_RETWEETS.ordinal()] += 1;
        }
        if (status.mInReplyToStatusId != null) {
            mCounts[FilterType.HIDE_REPLIES.ordinal()] += 1;
        }

        if (status.mIsRetweet || status.mInReplyToStatusId != null) {
            mCounts[FilterType.HIDE_RETWEETS_REPLIES.ordinal()] += 1;
        }
    }

    /*
	 * 
	 */
    public void add(QueryResult result) {

        List<Status> tweets = result.getTweets();
        if (tweets != null) {
            for (int i = 0; i < tweets.size(); i++) {
                Status tweet = tweets.get(i);
                add(new TwitterStatus(tweet));
            }
        }
        sort();
    }

    public void setFeedFullyRefreshed() {
        mGetNewStatusesMaxId = null;
    }

    /*
	 * 
	 */
    public interface AddUserCallback {

        public void addUser(User user);

        public void addUser(AdnUser user);
    }

    /*
	 * 
	 */
    public void add(ResponseList<twitter4j.Status> statuses,
                    AddUserCallback addUserCallback) {

        TwitterStatus firstItem = size() > 0 ? get(0) : null;
        int addCount = 0;

        boolean stillMore = true;
        TwitterStatus lastAddedStatus = null;

        mGetNewStatusesMaxId = null;

        for (Status status : statuses) {
            if (firstItem != null && status.getId() == firstItem.mId) {
                stillMore = false;
                break;
            }

            lastAddedStatus = new TwitterStatus(status);
            add(lastAddedStatus);

            if (addUserCallback != null) {
                addUserCallback.addUser(status.getUser());
                if (status.isRetweet()) {
                    Status retweetedStatus = status.getRetweetedStatus();
                    if (retweetedStatus != null) {
                        addUserCallback.addUser(retweetedStatus.getUser());
                    }
                }
            }

            addCount += 1;
        }

        if (stillMore && lastAddedStatus != null) {
            mGetNewStatusesMaxId = lastAddedStatus.mId;
        }

        if (addCount > 0) {
            sort();
        }
    }

    /*
	 * 
	 */
    public void add(AdnPosts posts, AddUserCallback addUserCallback) {

        TwitterStatus firstItem = size() > 0 ? get(0) : null;
        int addCount = 0;

        boolean stillMore = true;
        TwitterStatus lastAddedStatus = null;

        mGetNewStatusesMaxId = null;

        for (AdnPost post : posts.mPosts) {
            if (firstItem != null && post.mId == firstItem.mId) {
                stillMore = false;
                break;
            }

            lastAddedStatus = new TwitterStatus(post);
            add(lastAddedStatus);

            if (addUserCallback != null) {
                addUserCallback.addUser(post.mUser);
            }

            addCount += 1;
        }

        if (stillMore && lastAddedStatus != null) {
            mGetNewStatusesMaxId = lastAddedStatus.mId;
        }

        if (addCount > 0) {
            sort();
        }
    }

    /*
	 * 
	 */
    public void add(TwitterStatuses statuses) {

        TwitterStatus firstItem = size() > 0 ? get(0) : null;
        int addCount = 0;
        for (int i = 0; i < statuses.getStatusCount(); i++) {
            TwitterStatus status = statuses.getStatus(i);
            if (firstItem != null && status.mId == firstItem.mId) {
                break;
            }
            add(status);
            addCount += 1;
        }

        if (addCount > 0) {
            sort();
        }
    }

    /*
	 * 
	 */
    public void add(TwitterStatus status, boolean sort) {
        add(status);
        if (sort) {
            sort();
        }
    }

    /*
	 * 
	 */
    public void insert(TwitterStatuses statuses) {
        int addCount = 0;
        for (int i = 0; i < statuses.getStatusCount(); i++) {
            TwitterStatus status = statuses.getStatus(i);

            if (findByStatusId(status.mId) == null) {
                add(status);
                addCount += 1;
            }
        }

        if (addCount > 0) {
            sort();
        }
    }

    /*
	 * 
	 */
    public void remove(TwitterStatuses statuses) {

        int size = size();
        if (size != 0) {
            ArrayList<Integer> toRemoveList = new ArrayList<Integer>();

            for (int existingIndex = 0; existingIndex < size; existingIndex++) {
                TwitterStatus user = get(existingIndex);
                for (int i = 0; i < statuses.getStatusCount(); i++) {
                    TwitterStatus statusToRemove = statuses.getStatus(i);
                    if (statusToRemove.mId == user.mId) {
                        toRemoveList.add(existingIndex);
                    }
                }
            }

            for (int toRemoveIndex : toRemoveList) {
                remove(toRemoveIndex);
            }
        }
    }

    /*
	 * 
	 */
    public void sort() {
        Collections.sort(_mStatuses);
    }

    /*
     * Find the given status ID in a list via a binary search. Assumes the list
     * is sorted.
     */
    public TwitterStatus findByStatusId(long statusId) {
        Integer statusIndex = getStatusIndex(statusId);

        if (statusIndex != null) {
            return get(statusIndex.intValue());
        }

        return null;
    }

    /*
	 * 
	 */
    public Integer getStatusIndex(long statusId) {
        if (size() == 0) {
            return null;
        }
        int low = 0;
        int high = size() - 1;

        while (low <= high) {
            int middle = ((low + high) / 2);
            TwitterStatus status = get(middle);
            if (statusId > status.mId) {
                high = middle - 1;
            } else if (statusId < status.mId) {
                low = middle + 1;
            } else if (statusId == status.mId) {
                // The element has been found
                return middle;
            } else {
                // Couldn't find the element
                break;
            }
        }

        //nest down to try and get the status by an original statusID.
        //Useful in situtations like a retweet of a retweet, where you don't have the statusId
        return getStatusIndexFromOriginalStatusId(statusId);
    }

    Integer getStatusIndexFromOriginalStatusId(long originalStatusId) {
        if (size() == 0) {
            return null;
        }

        //Since original status ids have no order, a long search through each is required.

        for (int i = 0; i < size(); i++) {
            TwitterStatus status = get(i);
            if (originalStatusId == status.mOriginalRetweetId) {
                return i;
            }
        }

        return null;
    }

    /*
	 * 
	 */
    private int size() {
        return _mStatuses.size();
    }

    private TwitterStatus get(int index) {

        if (index >= _mStatuses.size()) {
            return null;
        }

        return _mStatuses.get(index);
    }

    private void remove(int index) {
        _mStatuses.remove(index);
    }

    /*
	 * 
	 */
    private final ArrayList<TwitterStatus> _mStatuses;
    private int[] mCounts = new int[FilterType.FILTER_MAX.ordinal()];

    private Long mGetNewStatusesMaxId = null;

    public Long getNewStatusesMaxId() {
        return mGetNewStatusesMaxId;
    }

    /*
	 * 
	 */
    public void reset() {
        _mStatuses.clear();
        mCounts = new int[FilterType.FILTER_MAX.ordinal()];
        mGetNewStatusesMaxId = null;
    }
}
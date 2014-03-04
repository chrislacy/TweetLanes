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

package org.tweetalib.android;

import android.util.Log;

import org.appdotnet4j.model.AdnPost;
import org.asynctasktex.AsyncTaskEx;
import org.socialnetlib.android.AppdotnetApi;
import org.tweetalib.android.TwitterConstant.StatusesType;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatuses;

import java.util.HashMap;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TwitterModifyStatuses {

    private ModifyStatusesWorkerCallbacks mCallbacks;
    private Integer mModifyStatusesCallbackHandle;
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
    public interface ModifyStatusesWorkerCallbacks {

        public Twitter getTwitterInstance();

        public AppdotnetApi getAppdotnetApi();
    }

    /*
     *
	 */
    public interface FinishedCallbackInterface {

        public void finished(boolean successful, TwitterStatuses statuses, Integer value);

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
    public abstract class FinishedDeleteCallback extends FinishedCallback {

        static final int kInvalidHandle = -1;
        private final TwitterStatuses mStatuses;


        public FinishedDeleteCallback(TwitterStatuses statuses) {
            mStatuses = statuses;
            mHandle = kInvalidHandle;
        }

        void setHandle(int handle) {
            mHandle = handle;
        }

        private int mHandle;

        public TwitterStatuses getStatuses() {
            return mStatuses;
        }
    }

    /*
     *
	 */
    public TwitterModifyStatuses() {
        mFinishedCallbackMap = new HashMap<Integer, FinishedCallback>();
        mModifyStatusesCallbackHandle = 0;
    }

    /*
     *
	 */
    public void setWorkerCallbacks(ModifyStatusesWorkerCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    /*
     *
	 */

    /*
     *
	 */
    FinishedCallback getModifyStatusesCallback(Integer callbackHandle) {
        return mFinishedCallbackMap.get(callbackHandle);
    }

    /*
	 *
	 */
    void removeModifyStatusesCallback(FinishedCallback callback) {
        if (mFinishedCallbackMap.containsValue(callback)) {
            mFinishedCallbackMap.remove(callback.mHandle);
        }
    }

    /*
	 *
	 */
    Twitter getTwitterInstance() {
        return mCallbacks.getTwitterInstance();
    }

    public void cancel(FinishedCallback callback) {

        removeModifyStatusesCallback(callback);
    }

    /*
	 *
	 */
    public void setFavorite(TwitterStatus status, boolean isFavorite, FinishedCallback callback) {

        setFavorite(new TwitterStatuses(status), isFavorite, callback);
    }

    /*
	 *
	 */
    public void setFavorite(TwitterStatuses statuses, boolean isFavorite, FinishedCallback callback) {

        mFinishedCallbackMap.put(mModifyStatusesCallbackHandle, callback);
        new ModifyStatusesTask().execute(AsyncTaskEx.PRIORITY_HIGH, "Set Favorite",
                new ModifyStatusesTaskInput(mModifyStatusesCallbackHandle, StatusesType.SET_FAVORITE, statuses,
                        isFavorite ? 1 : 0));
        mModifyStatusesCallbackHandle += 1;
    }

    /*
	 *
	 */
    public void deleteTweets(TwitterStatuses statuses, FinishedCallback callback) {

        mFinishedCallbackMap.put(mModifyStatusesCallbackHandle, callback);
        new ModifyStatusesTask().execute(AsyncTaskEx.PRIORITY_HIGH, "Delete",
                new ModifyStatusesTaskInput(mModifyStatusesCallbackHandle, StatusesType.DELETE, statuses, 1));
        mModifyStatusesCallbackHandle += 1;
    }

    AppdotnetApi getAppdotnetApi() {
        return mCallbacks.getAppdotnetApi();
    }

    /*
	 *
	 */
    class ModifyStatusesTaskInput {

        public ModifyStatusesTaskInput(Integer callbackHandle, StatusesType statusesType, TwitterStatuses statuses,
                                       Integer value) {
            mCallbackHandle = callbackHandle;
            mStatusesType = statusesType;
            mStatuses = new TwitterStatuses(statuses);
            mValue = value;
        }

        final Integer mCallbackHandle;
        final StatusesType mStatusesType;
        final TwitterStatuses mStatuses;
        final Integer mValue;
    }

    /*
	 *
	 */
    class ModifyStatusesTaskOutput {

        ModifyStatusesTaskOutput(TwitterFetchResult result, Integer callbackHandle, TwitterStatuses feed,
                                 Integer outputValue) {
            mCallbackHandle = callbackHandle;
            mFeed = feed;
            mValue = outputValue;
            mResult = result;
        }

        final Integer mCallbackHandle;
        final TwitterStatuses mFeed;
        final Integer mValue;
        final TwitterFetchResult mResult;
    }

    /*
	 *
	 */
    class ModifyStatusesTask extends AsyncTaskEx<ModifyStatusesTaskInput, Void, ModifyStatusesTaskOutput> {

        @Override
        protected ModifyStatusesTaskOutput doInBackground(ModifyStatusesTaskInput... inputArray) {

            TwitterStatuses contentFeed = new TwitterStatuses();
            ModifyStatusesTaskInput input = inputArray[0];
            Twitter twitter = getTwitterInstance();
            String errorDescription = null;

            AppdotnetApi appdotnetApi = getAppdotnetApi();
            if (appdotnetApi != null) {
                switch (input.mStatusesType) {
                    case DELETE: {
                        if (input.mStatuses != null) {
                            for (int i = 0; i < input.mStatuses.getStatusCount(); i++) {
                                TwitterStatus twitterStatus = input.mStatuses.getStatus(i);
                                AdnPost post = appdotnetApi.deleteTweet(twitterStatus.mId);
                                if (post == null) {
                                    errorDescription = "Unable to delete status";
                                }
                            }
                        }
                        break;
                    }

                    case SET_FAVORITE: {
                        boolean favorite = input.mValue == 1;

                        if (input.mStatuses != null) {
                            for (int i = 0; i < input.mStatuses.getStatusCount(); i++) {
                                TwitterStatus twitterStatus = input.mStatuses.getStatus(i);

                                AdnPost post = appdotnetApi.setAdnFavorite(twitterStatus.mId, favorite);

                                if (post != null) {
                                    twitterStatus = new TwitterStatus(post);
                                    twitterStatus.setFavorite(favorite);
                                    contentFeed.add(twitterStatus);
                                }
                            }
                        }
                        break;
                    }
                }
            } else if (twitter != null) {

                try {
                    switch (input.mStatusesType) {
                        case DELETE: {
                            if (input.mStatuses != null) {
                                for (int i = 0; i < input.mStatuses.getStatusCount(); i++) {
                                    TwitterStatus twitterStatus = input.mStatuses.getStatus(i);
                                    twitter.destroyStatus(twitterStatus.mId);
                                }
                            }
                            break;
                        }

                        case SET_FAVORITE: {
                            boolean favorite = input.mValue == 1;

                            if (input.mStatuses != null) {
                                for (int i = 0; i < input.mStatuses.getStatusCount(); i++) {
                                    TwitterStatus twitterStatus = input.mStatuses.getStatus(i);
                                    try {
                                        twitter4j.Status status;
                                        if (favorite) {
                                            status = twitter.createFavorite(twitterStatus.mId);
                                        } else {
                                            status = twitter.destroyFavorite(twitterStatus.mId);
                                        }

                                        // Yuck: See the comment for
                                        // TwitterStatus.setFavorite() for
                                        // reasons for this
                                        twitterStatus = new TwitterStatus(status);
                                        twitterStatus.setFavorite(favorite);

                                        contentFeed.add(twitterStatus);
                                    } catch (TwitterException e) {
                                        e.printStackTrace();
                                        errorDescription = e.getErrorMessage();
                                        Log.e("api-call", errorDescription, e);
                                        if (e.getRateLimitStatus() != null && e.getRateLimitStatus().getRemaining() <= 0) {
                                            throw e;
                                        }
                                    }
                                }

                            }

                            break;
                        }
                    }
                } catch (TwitterException e) {
                    e.printStackTrace();
                    errorDescription = e.getErrorMessage();
                    Log.e("api-call", errorDescription, e);
                    if (e.getRateLimitStatus() != null && e.getRateLimitStatus().getRemaining() <= 0) {
                        errorDescription +=
                                "\nTry again in " + e.getRateLimitStatus().getSecondsUntilReset() + " " + "seconds";
                    }
                }
            }

            return new ModifyStatusesTaskOutput(
                    new TwitterFetchResult(errorDescription == null, errorDescription),
                    input.mCallbackHandle, contentFeed, null);
        }

        @Override
        protected void onPostExecute(ModifyStatusesTaskOutput output) {

            FinishedCallback callback = getModifyStatusesCallback(output.mCallbackHandle);
            if (callback != null) {
                callback.finished(output.mResult.mErrorMessage == null, output.mFeed, output.mValue);
                removeModifyStatusesCallback(callback);
            }

            super.onPostExecute(output);
        }
    }

}

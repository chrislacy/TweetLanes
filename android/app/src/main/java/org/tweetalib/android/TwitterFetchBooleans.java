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

import org.asynctasktex.AsyncTaskEx;
import org.socialnetlib.android.AppdotnetApi;
import org.tweetalib.android.TwitterConstant.BooleanType;
import org.tweetalib.android.model.TwitterUser;

import java.util.ArrayList;
import java.util.HashMap;

import twitter4j.Friendship;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TwitterFetchBooleans {

    private FetchBooleansWorkerCallbacks mCallbacks;
    private Integer mFetchBooleanCallbackHandle;
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
    public interface FetchBooleansWorkerCallbacks {

        public Twitter getTwitterInstance();

        public AppdotnetApi getAppdotnetInstance();
    }

    /*
	 *
	 */
    public interface FinishedCallbackInterface {

        public void finished(TwitterFetchResult result,
                             ArrayList<Boolean> returnValues);

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
    public TwitterFetchBooleans() {
        mFinishedCallbackMap = new HashMap<Integer, FinishedCallback>();
        mFetchBooleanCallbackHandle = 0;
    }

    /*
	 *
	 */
    public void setWorkerCallbacks(FetchBooleansWorkerCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    /*
	 *
	 */

    /*
	 *
	 */
    FinishedCallback getFetchBooleanCallback(Integer callbackHandle) {
        return mFinishedCallbackMap.get(callbackHandle);
    }

    /*
	 *
	 */
    void removeFetchBooleanCallback(FinishedCallback callback) {
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

    AppdotnetApi getAppdotnetInstance() {
        return mCallbacks.getAppdotnetInstance();
    }

    /*
	 *
	 */
    public void getFriendshipExists(String userScreenName,
                                    String userScreenNameToCheck, FinishedCallback callback,
                                    ConnectionStatus connectionStatus) {

        triggerFetchBooleanTask(new FetchBooleanTaskInput(
                mFetchBooleanCallbackHandle, connectionStatus,
                BooleanType.FRIENDSHIP_EXISTS, userScreenName,
                userScreenNameToCheck), callback, connectionStatus);
    }

    /*
	 *
	 */
    void triggerFetchBooleanTask(FetchBooleanTaskInput taskInput,
                                 FinishedCallback callback, ConnectionStatus connectionStatus) {

        if (connectionStatus != null && !connectionStatus.isOnline()) {
            if (callback != null) {
                callback.finished(new TwitterFetchResult(false,
                        connectionStatus.getErrorMessageNoConnection()), null);
            }
            return;
        }

        mFinishedCallbackMap.put(mFetchBooleanCallbackHandle, callback);
        new FetchBooleanTask().execute(AsyncTaskEx.PRIORITY_MEDIUM,
                "Fetch Bools", taskInput);
        mFetchBooleanCallbackHandle += 1;
    }

    /*
	 *
	 */
    public void cancel(FinishedCallback callback) {

        removeFetchBooleanCallback(callback);
    }

    /*
	 *
	 */
    class FetchBooleanTaskInput {

        FetchBooleanTaskInput(Integer callbackHandle,
                              ConnectionStatus connectionStatus, BooleanType booleanType,
                              String userScreenName, String userScreenNameToCheck) {
            mCallbackHandle = callbackHandle;
            mConnectionStatus = connectionStatus;
            mBooleanType = booleanType;
            mUserScreenName = userScreenName;
            mUserScreenNameToCheck = userScreenNameToCheck;
        }

        final Integer mCallbackHandle;
        final ConnectionStatus mConnectionStatus;
        final BooleanType mBooleanType;
        final String mUserScreenName;
        final String mUserScreenNameToCheck;
    }

    /*
	 *
	 */
    class FetchBooleanTaskOutput {

        FetchBooleanTaskOutput(TwitterFetchResult result,
                               Integer callbackHandle, ArrayList<Boolean> returnValues) {
            mResult = result;
            mCallbackHandle = callbackHandle;
            if (returnValues != null) {
                mReturnValues = new ArrayList<Boolean>(returnValues);
            }
        }

        final TwitterFetchResult mResult;
        final Integer mCallbackHandle;
        ArrayList<Boolean> mReturnValues;
    }

    /*
	 *
	 */
    class FetchBooleanTask extends
            AsyncTaskEx<FetchBooleanTaskInput, Void, FetchBooleanTaskOutput> {

        @Override
        protected FetchBooleanTaskOutput doInBackground(
                FetchBooleanTaskInput... inputArray) {

            ArrayList<Boolean> result = new ArrayList<Boolean>();
            FetchBooleanTaskInput input = inputArray[0];
            Twitter twitter = getTwitterInstance();
            AppdotnetApi appdotnet = getAppdotnetInstance();
            String errorDescription = null;

            if (input.mConnectionStatus != null && !input.mConnectionStatus.isOnline()) {
                return new FetchBooleanTaskOutput(new TwitterFetchResult(false,
                        input.mConnectionStatus.getErrorMessageNoConnection()),
                        input.mCallbackHandle, null);
            }

            if (twitter != null) {
                try {
                    switch (input.mBooleanType) {
                        case FRIENDSHIP_EXISTS: {
                            if (!input.mUserScreenName.toLowerCase().equals(
                                    input.mUserScreenNameToCheck.toLowerCase())) {
                                ResponseList<Friendship> response = twitter
                                        .lookupFriendships(new String[]{input.mUserScreenName});
                                if (response != null && response.size() == 1) {
                                    result.add(response.get(0).isFollowedBy());
                                    result.add(response.get(0).isFollowing());
                                }
                            }
                            break;
                        }
                        default:
                            break;
                    }

                } catch (TwitterException e) {
                    e.printStackTrace();
                    errorDescription = e.getErrorMessage();
                }
            } else if (appdotnet != null) {
                switch (input.mBooleanType) {
                    case FRIENDSHIP_EXISTS: {
                        if (!input.mUserScreenName.toLowerCase().equals(
                                input.mUserScreenNameToCheck.toLowerCase())) {
                            TwitterUser user = appdotnet
                                    .getAdnUser(input.mUserScreenName);
                            if (user != null) {
                                result.add(user.getFollowsCurrentUser());
                                result.add(user.getCurrentUserFollows());
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }
            }

            if (result.size() == 0) {
                result = null;
            }

            return new FetchBooleanTaskOutput(new TwitterFetchResult(
                    errorDescription == null, errorDescription),
                    input.mCallbackHandle, result);
        }

        @Override
        protected void onPostExecute(FetchBooleanTaskOutput output) {

            FinishedCallback callback = getFetchBooleanCallback(output.mCallbackHandle);
            if (callback != null) {
                callback.finished(output.mResult, output.mReturnValues);
                removeFetchBooleanCallback(callback);
            }

            super.onPostExecute(output);
        }
    }

}

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

import org.asynctasktex.AsyncTaskEx;
import org.tweetalib.android.TwitterConstant.StatusesType;
import org.tweetalib.android.model.TwitterDirectMessage;
import org.tweetalib.android.model.TwitterDirectMessages;

import java.util.ArrayList;
import java.util.HashMap;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TwitterModifyDirectMessages {

    private ModifyDirectMessagesWorkerCallbacks mCallbacks;
    private Integer mModifyDirectMessagesCallbackHandle;
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
    public interface ModifyDirectMessagesWorkerCallbacks {

        public Twitter getTwitterInstance();
    }

    /*
     *
	 */
    public interface FinishedCallbackInterface {

        public void finished(boolean successful, Integer value);

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
    public TwitterModifyDirectMessages() {
        mFinishedCallbackMap = new HashMap<Integer, FinishedCallback>();
        mModifyDirectMessagesCallbackHandle = 0;
    }

    /*
     *
	 */
    public void setWorkerCallbacks(ModifyDirectMessagesWorkerCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    /*
     *
	 */

    /*
	 *
	 */
    FinishedCallback getModifyDirectMessagesCallback(Integer callbackHandle) {
        return mFinishedCallbackMap.get(callbackHandle);
    }

    /*
	 *
	 */
    void removeModifyDirectMessagesCallback(FinishedCallback callback) {
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

        removeModifyDirectMessagesCallback(callback);
    }

    /*
	 *
	 */
    public void deleteDirectMessages(TwitterDirectMessages statuses, FinishedCallback callback) {

        mFinishedCallbackMap.put(mModifyDirectMessagesCallbackHandle, callback);
        new ModifyDirectMessagesTask().execute(AsyncTaskEx.PRIORITY_HIGH, "Delete",
                new ModifyDirectMessagesTaskInput(mModifyDirectMessagesCallbackHandle, StatusesType.DELETE, statuses, 1));
        mModifyDirectMessagesCallbackHandle += 1;
    }

    /*
	 *
	 */
    class ModifyDirectMessagesTaskInput {

        public ModifyDirectMessagesTaskInput(Integer callbackHandle, StatusesType statusesType, TwitterDirectMessages messages,
                                             Integer value) {
            mCallbackHandle = callbackHandle;
            mStatusesType = statusesType;
            mMessages = new TwitterDirectMessages(0);
            mMessages.add(messages);
            mValue = value;
        }

        final Integer mCallbackHandle;
        final StatusesType mStatusesType;
        final TwitterDirectMessages mMessages;
        final Integer mValue;
    }

    /*
	 *
	 */
    class ModifyDirectMessagesTaskOutput {

        ModifyDirectMessagesTaskOutput(TwitterFetchResult result, Integer callbackHandle,
                                       Integer outputValue) {
            mCallbackHandle = callbackHandle;
            mValue = outputValue;
            mResult = result;
        }

        final Integer mCallbackHandle;
        final Integer mValue;
        final TwitterFetchResult mResult;
    }

    /*
	 *
	 */
    class ModifyDirectMessagesTask extends AsyncTaskEx<ModifyDirectMessagesTaskInput, Void, ModifyDirectMessagesTaskOutput> {

        @Override
        protected ModifyDirectMessagesTaskOutput doInBackground(ModifyDirectMessagesTaskInput... inputArray) {

            ModifyDirectMessagesTaskInput input = inputArray[0];
            Twitter twitter = getTwitterInstance();
            String errorDescription = null;

            if (twitter != null) {

                try {
                    switch (input.mStatusesType) {
                        case DELETE: {
                            if (input.mMessages != null) {
                                ArrayList<TwitterDirectMessage> messages = input.mMessages.getAllMessages();
                                for (int i = 0; i < input.mMessages.getAllMessages().size(); i++) {
                                    TwitterDirectMessage directMessage = messages.get(i);
                                    try {
                                        twitter.directMessages().destroyDirectMessage(directMessage.getId());
                                    } catch (TwitterException e) {
                                        String errorMessage = e.getErrorMessage();
                                        Log.d("api-call", errorMessage);
                                        if (errorMessage.toLowerCase().equals("sorry, that page does not exist")) {
                                            Log.d("api-call", "Delete found page doesn't exist, just carry on.");
                                        } else {
                                            throw e;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                } catch (TwitterException e) {
                    errorDescription = e.getErrorMessage();
                    Log.e("api-call", errorDescription, e);
                    if (e.getRateLimitStatus() != null && e.getRateLimitStatus().getRemaining() <= 0) {
                        errorDescription +=
                                "\nTry again in " + e.getRateLimitStatus().getSecondsUntilReset() + " " + "seconds";
                    }
                }
            }

            return new ModifyDirectMessagesTaskOutput(
                    new TwitterFetchResult(errorDescription == null, errorDescription),
                    input.mCallbackHandle, null);
        }

        @Override
        protected void onPostExecute(ModifyDirectMessagesTaskOutput output) {

            FinishedCallback callback = getModifyDirectMessagesCallback(output.mCallbackHandle);
            if (callback != null) {
                callback.finished(output.mResult.mErrorMessage == null, output.mValue);
                removeModifyDirectMessagesCallback(callback);
            }

            super.onPostExecute(output);
        }
    }

}

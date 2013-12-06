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

package org.tweetalib.android.fetch;

import android.util.Log;

import org.asynctasktex.AsyncTaskEx;
import org.tweetalib.android.ConnectionStatus;
import org.tweetalib.android.TwitterConstant;
import org.tweetalib.android.TwitterContentHandle;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterPaging;
import org.tweetalib.android.callback.TwitterFetchDirectMessagesFinishedCallback;
import org.tweetalib.android.model.TwitterDirectMessages;
import org.tweetalib.android.model.TwitterDirectMessages.AddUserCallback;

import java.util.HashMap;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class TwitterFetchDirectMessages {

    private FetchMessagesWorkerCallbacks mCallbacks;
    private final HashMap<String, TwitterDirectMessages> mMessagesHashMap;
    private Integer mFetchMessagesCallbackHandle;
    private final HashMap<Integer, TwitterFetchDirectMessagesFinishedCallback> mFinishedCallbackMap;

    /*
     *
	 */
    public void clearCallbacks() {
        if (mFinishedCallbackMap != null) {
            mFinishedCallbackMap.clear();
        }
    }

    public TwitterDirectMessages setDirectMessages(TwitterContentHandle contentHandle, TwitterDirectMessages messages) {
        TwitterDirectMessages cachedMessages = getDirectMessages(contentHandle);
        cachedMessages.add(messages);
        return cachedMessages;
    }

    /*
     *
	 */
    public interface FetchMessagesWorkerCallbacks {

        public Twitter getTwitterInstance();

        public void addUser(User user);
    }

    /*
     *
	 */
    public TwitterFetchDirectMessages() {
        mFinishedCallbackMap = new HashMap<Integer, TwitterFetchDirectMessagesFinishedCallback>();
        mFetchMessagesCallbackHandle = 0;
        mMessagesHashMap = new HashMap<String, TwitterDirectMessages>();
    }

    /*
	 *
	 */
    public void setWorkerCallbacks(FetchMessagesWorkerCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    /*
	 *
	 */
    TwitterFetchDirectMessagesFinishedCallback getFetchStatusesCallback(
            Integer callbackHandle) {
        return mFinishedCallbackMap
                .get(callbackHandle);
    }

    /*
	 *
	 */
    void removeFetchStatusesCallback(
            TwitterFetchDirectMessagesFinishedCallback callback) {
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

    public void removeFromDirectMessageHashMap(TwitterDirectMessages mesages) {
        if (mMessagesHashMap != null) {
            for (String key : mMessagesHashMap.keySet()) {
                TwitterDirectMessages feed = mMessagesHashMap.get(key);
                feed.remove(mesages);
            }
        }
    }

    /*
	 *
	 */
    public TwitterDirectMessages getDirectMessages(
            TwitterContentHandle contentHandle) {

        if (mMessagesHashMap != null) {
            TwitterDirectMessages messages = mMessagesHashMap.get(contentHandle
                    .getKey());
            if (messages == null) {
                String id = contentHandle.getIdentifier();
                long idLong = Long.parseLong(id);
                messages = new TwitterDirectMessages(idLong);
                mMessagesHashMap.put(contentHandle.getKey(), messages);
            }

            return messages;
        }

        return null;
    }

    /*
	 *
	 */
    public TwitterDirectMessages getDirectMessages(
            TwitterContentHandle contentHandle, TwitterPaging paging,
            TwitterFetchDirectMessagesFinishedCallback callback,
            ConnectionStatus connectionStatus) {

        if (connectionStatus != null && !connectionStatus.isOnline()) {
            if (callback != null) {
                callback.finished(contentHandle, new TwitterFetchResult(false,
                        connectionStatus.getErrorMessageNoConnection()), null);
            }
            return null;
        }

        if (mFinishedCallbackMap.containsValue(callback)) {
            throw new RuntimeException("Shouldn't be");
        }

        mFinishedCallbackMap.put(mFetchMessagesCallbackHandle, callback);
        new FetchStatusesTask().execute(AsyncTaskEx.PRIORITY_HIGH,
                "Fetch DMs", new FetchDirectMessagesTaskInput(
                mFetchMessagesCallbackHandle, contentHandle, paging,
                connectionStatus));
        mFetchMessagesCallbackHandle += 1;

        return null;
    }

    /*
	 *
	 */
    public void sendDirectMessage(long userId, String recipientScreenName,
                                  String statusText,
                                  TwitterContentHandle contentHandle,
                                  TwitterFetchDirectMessagesFinishedCallback callback,
                                  ConnectionStatus connectionStatus) {
        if (connectionStatus != null && !connectionStatus.isOnline()) {
            if (callback != null) {
                callback.finished(contentHandle, new TwitterFetchResult(false,
                        connectionStatus.getErrorMessageNoConnection()), null);
            }
        }

        mFinishedCallbackMap.put(mFetchMessagesCallbackHandle, callback);
        new FetchStatusesTask().execute(AsyncTaskEx.PRIORITY_HIGH,
                "Fetch DMs", new FetchDirectMessagesTaskInput(
                mFetchMessagesCallbackHandle, userId,
                recipientScreenName, statusText, connectionStatus));
        mFetchMessagesCallbackHandle += 1;
    }

    /*
	 *
	 */
    public void cancel(TwitterFetchDirectMessagesFinishedCallback callback) {

        removeFetchStatusesCallback(callback);
    }

    /*
	 *
	 */
    class FetchDirectMessagesTaskInput {

        FetchDirectMessagesTaskInput(Integer callbackHandle,
                                     TwitterContentHandle contentHandle, TwitterPaging paging,
                                     ConnectionStatus connectionStatus) {
            mCallbackHandle = callbackHandle;
            mContentHandle = contentHandle;
            mPaging = paging;
            mConnectionStatus = connectionStatus;
        }

        FetchDirectMessagesTaskInput(Integer callbackHandle, Long userId,
                                     String recipientScreenName, String statusText,
                                     ConnectionStatus connectionStatus) {
            mCallbackHandle = callbackHandle;
            mUserId = userId;
            mRecipientScreenName = recipientScreenName;
            mStatusText = statusText;
            mConnectionStatus = connectionStatus;
        }

        final Integer mCallbackHandle;
        Long mUserId;
        String mRecipientScreenName;
        String mStatusText;
        TwitterContentHandle mContentHandle;
        TwitterPaging mPaging;
        final ConnectionStatus mConnectionStatus;
    }

    /*
	 *
	 */
    class FetchDirectMessagesTaskOutput {

        FetchDirectMessagesTaskOutput(TwitterContentHandle contentHandle, TwitterFetchResult result,
                                      Integer callbackHandle, TwitterDirectMessages messages) {
            mContentHandle = contentHandle;
            mResult = result;
            mCallbackHandle = callbackHandle;
            mMessages = messages;
        }

        final TwitterContentHandle mContentHandle;
        final TwitterFetchResult mResult;
        final Integer mCallbackHandle;
        final TwitterDirectMessages mMessages;
    }

    /*
	 *
	 */
    class FetchStatusesTask
            extends
            AsyncTaskEx<FetchDirectMessagesTaskInput, Void, FetchDirectMessagesTaskOutput> {

        @Override
        protected FetchDirectMessagesTaskOutput doInBackground(
                FetchDirectMessagesTaskInput... inputArray) {

            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            FetchDirectMessagesTaskInput input = inputArray[0];
            TwitterDirectMessages messages = null;
            Twitter twitter = getTwitterInstance();
            String errorDescription = null;

            if (input.mConnectionStatus != null && !input.mConnectionStatus.isOnline()) {
                return new FetchDirectMessagesTaskOutput(input.mContentHandle,
                        new TwitterFetchResult(false,
                                input.mConnectionStatus
                                        .getErrorMessageNoConnection()),
                        input.mCallbackHandle, null);
            }

            if (twitter != null) {

                try {
                    if (input.mStatusText != null) {

                        Log.d("api-call", "sendDirectMessage");
                        DirectMessage dm = twitter.sendDirectMessage(
                                input.mRecipientScreenName, input.mStatusText);
                        messages = new TwitterDirectMessages(input.mUserId);
                        messages.add(dm);

                    } else {
                        Paging defaultPaging = new Paging(1);
                        defaultPaging.setCount(30);
                        Paging paging;
                        if (input.mPaging != null) {
                            paging = input.mPaging.getT4JPaging();
                        } else {
                            paging = defaultPaging;
                        }

                        switch (input.mContentHandle.getDirectMessagesType()) {
                            case RECIEVED_MESSAGES:
                            case ALL_MESSAGES:
                            case ALL_MESSAGES_FRESH: {

                                if (input.mContentHandle.getDirectMessagesType() == TwitterConstant.DirectMessagesType.ALL_MESSAGES_FRESH) {
                                    mMessagesHashMap.remove(input.mContentHandle.getKey());
                                }

                                messages = getDirectMessages(input.mContentHandle);

                                // Annoyingly, DMs can't be retrieved in a threaded
                                // format. Handle this
                                // by getting sent and received and managing
                                // ourselves...
                                Log.d("api-call", "getDirectMessages");
                                ResponseList<DirectMessage> receivedDirectMessages = twitter
                                        .getDirectMessages(paging);

                                ResponseList<DirectMessage> sentDirectMessages = null;
                                if (input.mContentHandle.getDirectMessagesType() == TwitterConstant.DirectMessagesType.ALL_MESSAGES ||
                                        input.mContentHandle.getDirectMessagesType() == TwitterConstant.DirectMessagesType.ALL_MESSAGES_FRESH) {
                                    Log.d("api-call", "getSendDirectMessages");
                                    sentDirectMessages = twitter
                                            .getSentDirectMessages(paging);
                                }
                                AddUserCallback addUserCallback = new AddUserCallback() {

                                    @Override
                                    public void addUser(User user) {
                                        mCallbacks.addUser(user);
                                    }
                                };

                                messages.add(sentDirectMessages,
                                        receivedDirectMessages, addUserCallback);
                                break;
                            }
                            default:
                                break;
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
                }
            }

            return new FetchDirectMessagesTaskOutput(input.mContentHandle, new TwitterFetchResult(
                    errorDescription == null, errorDescription),
                    input.mCallbackHandle, messages);
        }

        @Override
        protected void onPostExecute(FetchDirectMessagesTaskOutput output) {

            TwitterFetchDirectMessagesFinishedCallback callback = getFetchStatusesCallback(output.mCallbackHandle);
            if (callback != null) {
                callback.finished(output.mContentHandle, output.mResult, output.mMessages);
                removeFetchStatusesCallback(callback);
            }

            super.onPostExecute(output);
        }
    }

}

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

import java.util.HashMap;

import org.tweetalib.android.ConnectionStatus;
import org.tweetalib.android.TwitterContentHandle;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterPaging;
import org.tweetalib.android.callback.TwitterFetchDirectMessagesFinishedCallback;
import org.tweetalib.android.model.TwitterDirectMessages;
import org.tweetalib.android.model.TwitterDirectMessages.AddUserCallback;

import org.asynctasktex.AsyncTaskEx;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class TwitterFetchDirectMessages {

    private FetchMessagesWorkerCallbacks mCallbacks;
    private HashMap<String, TwitterDirectMessages> mMessagesHashMap;
    private Integer mFetchMessagesCallbackHandle;
    private HashMap<Integer, TwitterFetchDirectMessagesFinishedCallback> mFinishedCallbackMap;

    /*
	 *
	 */
    public void clearCallbacks() {
        if (mFinishedCallbackMap != null ) {
            mFinishedCallbackMap.clear();
        }
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
        TwitterFetchDirectMessagesFinishedCallback callback = mFinishedCallbackMap
                .get(callbackHandle);
        return callback;
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

        if (connectionStatus.isOnline() == false) {
            if (callback != null) {
                callback.finished(new TwitterFetchResult(false,
                        connectionStatus.getErrorMessageNoConnection()), null);
            }
            return null;
        }

        if (mFinishedCallbackMap.containsValue(callback)) {
            throw new RuntimeException("Shouldn't be");
        }

        mFinishedCallbackMap.put(mFetchMessagesCallbackHandle, callback);
        new FetchStatusesTask().execute(AsyncTaskEx.PRIORITY_MEDIUM,
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
            TwitterFetchDirectMessagesFinishedCallback callback,
            ConnectionStatus connectionStatus) {
        if (connectionStatus.isOnline() == false) {
            if (callback != null) {
                callback.finished(new TwitterFetchResult(false,
                        connectionStatus.getErrorMessageNoConnection()), null);
            }
        }

        mFinishedCallbackMap.put(mFetchMessagesCallbackHandle, callback);
        new FetchStatusesTask().execute(AsyncTaskEx.PRIORITY_MEDIUM,
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

        Integer mCallbackHandle;
        Long mUserId;
        String mRecipientScreenName;
        String mStatusText;
        TwitterContentHandle mContentHandle;
        TwitterPaging mPaging;
        ConnectionStatus mConnectionStatus;
    }

    /*
	 *
	 */
    class FetchDirectMessagesTaskOutput {

        FetchDirectMessagesTaskOutput(TwitterFetchResult result,
                Integer callbackHandle, TwitterDirectMessages messages) {
            mResult = result;
            mCallbackHandle = callbackHandle;
            mMessages = messages;
        }

        TwitterFetchResult mResult;
        Integer mCallbackHandle;
        TwitterDirectMessages mMessages;
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

            if (input.mConnectionStatus.isOnline() == false) {
                return new FetchDirectMessagesTaskOutput(
                        new TwitterFetchResult(false,
                                input.mConnectionStatus
                                        .getErrorMessageNoConnection()),
                        input.mCallbackHandle, null);
            }

            if (twitter != null) {

                try {
                    if (input.mStatusText != null) {

                        DirectMessage dm = twitter.sendDirectMessage(
                                input.mRecipientScreenName, input.mStatusText);
                        messages = new TwitterDirectMessages(input.mUserId);
                        messages.add(dm);

                    } else {
                        Paging defaultPaging = new Paging(1);
                        defaultPaging.setCount(30);
                        Paging paging = null;
                        if (input.mPaging != null) {
                            paging = input.mPaging.getT4JPaging();
                        } else {
                            paging = defaultPaging;
                        }

                        switch (input.mContentHandle.getDirectMessagesType()) {
                        case ALL_MESSAGES: {
                            messages = getDirectMessages(input.mContentHandle);
                            // Annoyingly, DMs can't be retrieved in a threaded
                            // format. Handle this
                            // by getting sent and received and managing
                            // ourselves...
                            ResponseList<DirectMessage> receivedDirectMessages = twitter
                                    .getDirectMessages(paging);
                            ResponseList<DirectMessage> sentDirectMessages = twitter
                                    .getSentDirectMessages(paging);

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
                }
            }

            return new FetchDirectMessagesTaskOutput(new TwitterFetchResult(
                    errorDescription == null ? true : false, errorDescription),
                    input.mCallbackHandle, messages);
        }

        @Override
        protected void onPostExecute(FetchDirectMessagesTaskOutput output) {

            TwitterFetchDirectMessagesFinishedCallback callback = getFetchStatusesCallback(output.mCallbackHandle);
            if (callback != null) {
                callback.finished(output.mResult, output.mMessages);
                removeFetchStatusesCallback(callback);
            }

            super.onPostExecute(output);
        }
    }

}

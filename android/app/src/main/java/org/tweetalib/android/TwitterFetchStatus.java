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

import org.appdotnet4j.model.AdnPost;
import org.appdotnet4j.model.AdnPostCompose;
import org.asynctasktex.AsyncTaskEx;
import org.socialnetlib.android.AppdotnetApi;
import org.tweetalib.android.TwitterConstant.StatusType;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatusUpdate;

import java.util.HashMap;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class TwitterFetchStatus {

    private FetchStatusWorkerCallbacks mCallbacks;
    private Integer mFetchStatusCallbackHandle;
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
    public interface FetchStatusWorkerCallbacks {

        public Twitter getTwitterInstance();

        public AppdotnetApi getAppdotnetApi();

        public void addUser(User user);
    }

    /*
	 *
	 */
    public interface FinishedCallbackInterface {

        public void finished(TwitterFetchResult result, TwitterStatus status);

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
    public TwitterFetchStatus() {
        mFinishedCallbackMap = new HashMap<Integer, FinishedCallback>();
        mFetchStatusCallbackHandle = 0;
    }

    /*
	 *
	 */
    public void setWorkerCallbacks(FetchStatusWorkerCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    /*
	 *
	 */

    /*
	 *
	 */
    FinishedCallback getFetchStatusCallback(Integer callbackHandle) {
        return mFinishedCallbackMap.get(callbackHandle);
    }

    /*
	 *
	 */
    void removeFetchStatusCallback(FinishedCallback callback) {
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

    AppdotnetApi getAppdotnetApi() {
        return mCallbacks.getAppdotnetApi();
    }

    /*
	 *
	 */
    public TwitterStatus getStatus(long statusId, FinishedCallback callback,
                                   ConnectionStatus connectionStatus) {

        triggerFetchStatusTask(new FetchStatusTaskInput(
                mFetchStatusCallbackHandle, StatusType.GET_STATUS, statusId,
                connectionStatus), callback, connectionStatus);
        return null;
    }

    /*
	 *
	 */
    public void setStatus(TwitterStatusUpdate statusUpdate,
                          FinishedCallback callback, ConnectionStatus connectionStatus) {

        triggerFetchStatusTask(new FetchStatusTaskInput(
                mFetchStatusCallbackHandle, statusUpdate, connectionStatus),
                callback, connectionStatus);
    }

    /*
	 *
	 */
    public void setRetweet(long statusId, FinishedCallback callback,
                           ConnectionStatus connectionStatus) {
        triggerFetchStatusTask(new FetchStatusTaskInput(
                mFetchStatusCallbackHandle, StatusType.SET_RETWEET, statusId,
                connectionStatus), callback, connectionStatus);
    }

    /*
	 *
	 */
    void triggerFetchStatusTask(FetchStatusTaskInput taskInput,
                                FinishedCallback callback, ConnectionStatus connectionStatus) {

        if (connectionStatus != null && !connectionStatus.isOnline()) {
            if (callback != null) {
                callback.finished(new TwitterFetchResult(false,
                        connectionStatus.getErrorMessageNoConnection()), null);
            }
            return;
        }

        mFinishedCallbackMap.put(mFetchStatusCallbackHandle, callback);
        new FetchStatusTask().execute(AsyncTaskEx.PRIORITY_HIGHEST,
                "Fetch Status", taskInput);
        mFetchStatusCallbackHandle += 1;
    }

    /*
	 *
	 */
    public void cancel(FinishedCallback callback) {

        removeFetchStatusCallback(callback);
    }

    /*
	 *
	 */
    class FetchStatusTaskInput {

        FetchStatusTaskInput(Integer callbackHandle,
                             TwitterStatusUpdate statusUpdate,
                             ConnectionStatus connectionStatus) {
            mCallbackHandle = callbackHandle;
            mStatusUpdate = statusUpdate;
            mStatusType = StatusType.SET_STATUS;
            mConnectionStatus = connectionStatus;
        }

        FetchStatusTaskInput(Integer callbackHandle, StatusType statusType,
                             long existingStatus, ConnectionStatus connectionStatus) {
            mCallbackHandle = callbackHandle;
            mExistingStatusId = existingStatus;
            mStatusType = statusType;
            mConnectionStatus = connectionStatus;
        }

        final Integer mCallbackHandle;
        final StatusType mStatusType;
        TwitterStatusUpdate mStatusUpdate;
        Long mExistingStatusId;
        final ConnectionStatus mConnectionStatus;
    }

    /*
	 *
	 */
    class FetchStatusTaskOutput {

        FetchStatusTaskOutput(TwitterFetchResult result,
                              Integer callbackHandle, TwitterStatus status) {
            mResult = result;
            mCallbackHandle = callbackHandle;
            mStatus = status;
        }

        final TwitterFetchResult mResult;
        final Integer mCallbackHandle;
        final TwitterStatus mStatus;
    }

    /*
	 *
	 */
    class FetchStatusTask extends
            AsyncTaskEx<FetchStatusTaskInput, Void, FetchStatusTaskOutput> {

        @Override
        protected FetchStatusTaskOutput doInBackground(
                FetchStatusTaskInput... inputArray) {

            FetchStatusTaskInput input = inputArray[0];
            Twitter twitter = getTwitterInstance();
            TwitterStatus twitterStatus = null;
            String errorDescription = null;

            if (input.mConnectionStatus != null && !input.mConnectionStatus.isOnline()) {
                return new FetchStatusTaskOutput(new TwitterFetchResult(false,
                        input.mConnectionStatus.getErrorMessageNoConnection()),
                        input.mCallbackHandle, null);
            }

            AppdotnetApi appdotnetApi = getAppdotnetApi();
            if (appdotnetApi != null) {
                AdnPost status = null;
                switch (input.mStatusType) {
                    case SET_STATUS:
                        AdnPostCompose post = input.mStatusUpdate.getAdnComposePost();
                        appdotnetApi.setAdnStatus(post);
                        break;

                    case GET_STATUS: {
                        status = appdotnetApi.getAdnPost(input.mExistingStatusId);
                        break;
                    }

                    case SET_RETWEET: {
                        status = appdotnetApi.setAdnRepost(input.mExistingStatusId);
                        break;
                    }

                    default:
                        break;
                }

                if (status != null) {
                    twitterStatus = new TwitterStatus(status);
                }
            } else if (twitter != null) {
                twitter4j.Status status = null;
                try {

                    switch (input.mStatusType) {
                        case GET_STATUS: {
                            Log.d("api-call", "showStatus");
                            status = twitter.showStatus(input.mExistingStatusId);
                            break;
                        }

                        case SET_STATUS: {
                            Log.d("api-call", "updateStatus");
                            StatusUpdate statusUpdate = input.mStatusUpdate
                                    .getT4JStatusUpdate();
                            status = twitter.updateStatus(statusUpdate);

                        /*
                         * Configuration conf = getConfiguration();
                         * ImageUploadFactory factory = new
                         * ImageUploadFactory(conf); ImageUpload upload =
                         * factory.getInstance(MediaProvider.YFROG); try { File
                         * imageFile = input.mStatusUpdate.getMediaFile();
                         * upload.upload(imageFile); } catch (Exception e) {
                         * e.printStackTrace(); }
                         */

                        /*
                         * WORKS String url = null; String mMediaFilePath =
                         * input.mStatusUpdate.getMediaFilePath(); try {
                         * InputStream mediaInputStream = new
                         * BufferedInputStream(new
                         * FileInputStream(mMediaFilePath)); ImageUploadFactory
                         * factory = new ImageUploadFactory(getConfiguration());
                         * ImageUpload upload =
                         * factory.getInstance(MediaProvider.TWITTER); url =
                         * upload.upload(input.mStatusUpdate.getMediaFilePath(),
                         * mediaInputStream, "blerg"); } catch
                         * (FileNotFoundException e) { e.printStackTrace(); }
                         */

                        /*
                         * InputStream is =
                         * getClass().getResourceAsStream(input.
                         * mStatusUpdate.getMediaFilePath()); try {
                         * ImageUploadFactory factory = new
                         * ImageUploadFactory(); ImageUpload upload =
                         * factory.getInstance(MediaProvider.TWITTER); url =
                         * upload.upload(input.mStatusUpdate.getMediaFilePath(),
                         * is, "blerg"); } finally { try { is.close(); } catch
                         * (IOException e) { // TODO Auto-generated catch block
                         * e.printStackTrace(); } }
                         */

                            // status = twitter.showStatus(181681943774117888L);
                            break;
                        }

                        case SET_RETWEET: {
                            Log.d("api-call", "retweetStatus");
                            status = twitter.retweetStatus(input.mExistingStatusId);
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
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    errorDescription = e.getMessage();
                    Log.e("api-call", errorDescription, e);
                }

                if (status != null) {
                    twitterStatus = new TwitterStatus(status);
                }
            }

            return new FetchStatusTaskOutput(new TwitterFetchResult(
                    errorDescription == null, errorDescription),
                    input.mCallbackHandle, twitterStatus);
        }

        @Override
        protected void onPostExecute(FetchStatusTaskOutput output) {

            FinishedCallback callback = getFetchStatusCallback(output.mCallbackHandle);
            if (callback != null) {
                callback.finished(output.mResult, output.mStatus);
                removeFetchStatusCallback(callback);
            }

            super.onPostExecute(output);
        }
    }

}

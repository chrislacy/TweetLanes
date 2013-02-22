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

package com.tweetlanes.android.view;

import java.io.File;

import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchStatus.FinishedCallback;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatusUpdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.tweetlanes.android.AppSettings;
import com.tweetlanes.android.Constant;
import com.tweetlanes.android.NotificationHelper;
import com.tweetlanes.android.NotificationHelper.Builder;
import com.tweetlanes.android.R;
import com.tweetlanes.android.model.ComposeTweetDefault;
import com.tweetlanes.android.util.Util;
import com.twitter.Validator;

public class ComposeTweetFragment extends ComposeBaseFragment {

    private ImageView mAttachImagePreview;

    /*
         * 
         */
    FinishedCallback mOnSetStatusCallback = TwitterManager.get()
            .getFetchStatusInstance().new FinishedCallback() {
    
        @Override
        public void finished(TwitterFetchResult result, TwitterStatus status) {
    
            /*
             * if (mSendingNotification != null) {
             * NotificationHelper.get().cancel(getActivity(),
             * mSendingNotification); mSendingNotification = null; }
             */
    
            NotificationHelper.Builder builder = null;
    
            mUpdatingStatus = false;
            mEditText.setEnabled(true);
            mSendButton.setEnabled(true);
    
            Activity activity = getActivity();
    
            if (result.isSuccessful()) {
                releaseFocus(false);
    
                /*
                 * if (Constant.SHOW_NOTIFICATION_AFTER_TWEET_POSTED) { Intent i
                 * = new Intent(getActivity().getApplicationContext(),
                 * TweetSpotlightActivity.class);
                 * i.putExtra(TweetSpotlightActivity.STATUS_ID_KEY,
                 * Long.toString(status.getId())); PendingIntent pendingIntent =
                 * PendingIntent.getActivity(getActivity(), 0, i,
                 * PendingIntent.FLAG_UPDATE_CURRENT); builder =
                 * NotificationHelper.get().new Builder(getActivity(), true);
                 * builder.setContentIntent(pendingIntent);
                 * builder.setContentTitle
                 * (getString(R.string.tweet_posted_success));
                 * builder.setContentText(status.getStatus());
                 * builder.setTicker(getString(R.string.tweet_posted_success));
                 * builder.setAutoCancel(true); } else {
                 */
                if (getActivity() != null
                        && getActivity().getApplicationContext() != null) {
                    Toast.makeText(getActivity().getApplicationContext(),
                            getString(R.string.tweet_posted_success),
                            Constant.DEFAULT_TOAST_DISPLAY_TIME).show();
                }
                // }
    
                if (mListener != null) {
                    mListener.saveDraft(null);
                    mListener.onStatusUpdateSuccess();
                }
    
            } else if (result.getErrorMessage() != null && activity != null) {
                builder = NotificationHelper.get().new Builder(activity, true);
                builder.setContentTitle(getString(R.string.tweet_posted_error));
                builder.setContentText(result.getErrorMessage());
                builder.setTicker(getString(R.string.tweet_posted_error));
                builder.setAutoCancel(true);
            }
    
            if (builder != null) {
                NotificationHelper.get().notify(activity, builder);
            }
    
            updateStatusHint();
        }
    
    };

    /*
         * 
         */
    TwitterStatus mRetweetStatus;

    /*
     * (non-Javadoc)
     * @see
     * com.tweetlanes.android.view.ComposeBaseFragment#onCreateView(android.
     * view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View resultView = super.onCreateView(inflater, container,
                savedInstanceState);

        mAttachImagePreview = (ImageView) resultView
                .findViewById(R.id.statusImage);
        mAttachImagePreview.setVisibility(View.GONE);

        return resultView;
    }

    /*
	 * 
	 */
    @Override
    protected void setComposeTweetDefault(
            ComposeTweetDefault composeTweetDefault) {
        super.setComposeTweetDefault(composeTweetDefault);
        setMediaPreviewVisibility();
    }

    /*
	 * 
	 */
    @Override
    protected void updateComposeTweetDefault() {

        ComposeTweetDefault composeTweetDefault = null;
        String currentStatus = mEditText.getText().toString();

        if (Util.isValidString(currentStatus)) {
            composeTweetDefault = new ComposeTweetDefault(getApp()
                    .getCurrentAccountScreenName(), currentStatus,
                    getInReplyToId(), getMediaFilePath());
        }

        setComposeTweetDefault(composeTweetDefault);
    }

    /*
	 * 
	 */
    @Override
    protected String getTweetDefaultDraft() {

        if (getComposeTweetDefault() != null) {
            String result = getComposeTweetDefault().toString();
            if (result != null && result.equals("") == false) {
                return result;
            }
        }
        return null;
    }

    /*
	 * 
	 */
    @Override
    protected void setTweetDefaultFromDraft(String tweetDraftAsJson) {
        if (tweetDraftAsJson != null) {
            ComposeTweetDefault tweetDefault = new ComposeTweetDefault(
                    tweetDraftAsJson);
            setComposeTweetDefault(tweetDefault);
        }
    }

    /*
	 * 
	 */
    String getDefaultQuoteStatus(TwitterStatus statusToQuote) {
        if (statusToQuote.mStatus.length() > 0) {
            String quote = null;
            switch (AppSettings.get().getCurrentQuoteType()) {

            case RT:
                quote = "RT @" + statusToQuote.getAuthorScreenName() + ": "
                        + statusToQuote.mStatus;
                break;

            case Via:
                quote = statusToQuote.mStatus + " (via @"
                        + statusToQuote.getAuthorScreenName() + ")";
                break;

            case Standard:
            default:
                quote = "\"@" + statusToQuote.getAuthorScreenName() + ": "
                        + statusToQuote.mStatus + "\"";
                break;
            }
            return quote;
        }
        return "";
    }

    /*
	 * 
	 */
    Long getInReplyToId() {

        if (getComposeTweetDefault() != null) {
            return getComposeTweetDefault().getInReplyToStatusId();
        }
        return null;
    }

    /*
	 * 
	 */
    String getMediaFilePath() {
        if (getComposeTweetDefault() != null) {
            return getComposeTweetDefault().getMediaFilePath();
        }
        return null;
    }

    /*
	 * 
	 */
    @Override
    protected void saveCurrentAsDraft() {
        ComposeTweetDefault composeDraft = null;

        String currentStatus = mEditText.getText().toString();
        if (currentStatus != null && currentStatus.equals("") == false) {
            if (mStatusValidator.getTweetLength(currentStatus) > 0) {
                if (getComposeTweetDefault() != null) {
                    getComposeTweetDefault().updateStatus(currentStatus);
                    if (getComposeTweetDefault().isPlaceholderStatus() == false) {
                        composeDraft = getComposeTweetDefault();
                    }
                } else {
                    composeDraft = new ComposeTweetDefault(getApp()
                            .getCurrentAccountScreenName(), currentStatus);
                }
            }
        }

        if (mListener != null) {
            mListener.saveDraft(composeDraft == null ? null : composeDraft
                    .toString());
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * com.tweetlanes.android.view.ComposeBaseFragment#clearCompose(boolean)
     */
    @Override
    void clearCompose(boolean saveCurrentTweet) {

        super.clearCompose(saveCurrentTweet);
        mAttachImagePreview.setVisibility(View.GONE);
    }

    /*
     * (non-Javadoc)
     * @see
     * com.tweetlanes.android.view.ComposeBaseFragment#onSendClick(java.lang
     * .String)
     */
    @Override
    protected void onSendClick(String status) {
        if (status != null) {
            int statusLength = mStatusValidator.getTweetLength(status);
            if (mStatusValidator.isValidTweet(status) == false) {
                showSimpleAlert(mStatusValidator.getTweetLength(status) <= Validator.MAX_TWEET_LENGTH ? R.string.alert_status_invalid
                        : R.string.alert_status_too_long);
            } else if (statusLength > 0) {
                // Too long:
                // status =
                // "Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. Hi there. ";
                TwitterStatusUpdate statusUpdate = new TwitterStatusUpdate(
                        status, getInReplyToId());
                statusUpdate.setMediaFilePath(getMediaFilePath());

                mUpdatingStatus = true;
                mEditText.setHint(null);
                mEditText.setEnabled(false);
                mSendButton.setEnabled(false);

                TwitterManager.get().setStatus(statusUpdate,
                        mOnSetStatusCallback);

                /*
                 * NotificationHelper.Builder builder =
                 * NotificationHelper.get().new Builder(getActivity(), true);
                 * builder
                 * .setContentTitle(getString(R.string.posting_tweet_ongoing));
                 * builder.setContentText(status);
                 * builder.setTicker(getString(R.string.posting_tweet_ongoing));
                 * builder.setAutoCancel(true); builder.setOngoing(true);
                 */

                showToast(getString(R.string.posting_tweet_ongoing));

                ComposeTweetDefault currentStatus = new ComposeTweetDefault(
                        getApp().getCurrentAccountScreenName(), status,
                        getInReplyToId(), getMediaFilePath());

                if (mListener != null) {
                    mListener.saveDraft(currentStatus.toString());
                    mListener.onStatusUpdateRequest();
                }

                setComposeTweetDefault(currentStatus);

                updateStatusHint();

                // mSendingNotification =
                // NotificationHelper.get().notify(getActivity(), builder);
            }
        }
    }

    /*
	 * 
	 */
    private String getStatusHint(ComposeTweetDefault composeTweetDefault) {

        String hint = null;

        if (composeTweetDefault != null) {
            String lastStatus = composeTweetDefault.getStatus();
            if (lastStatus != null) {
                lastStatus = Util.trimLeftRight(lastStatus);
                if (composeTweetDefault.getInReplyToStatusId() != null) {
                    hint = getString(R.string.compose_tweet_reply_to) + " "
                            + getStatusHintSnippet(lastStatus, 20);
                } else {
                    String[] words = lastStatus.split(" ");
                    if (words.length == 1 && words[0].length() > 0
                            && words[0].charAt(0) == '@') {
                        hint = getString(R.string.compose_tweet_to_user_hint)
                                + " " + getStatusHintSnippet(words[0], 18);
                    } else {
                        hint = getString(R.string.compose_tweet_finish) + " \""
                                + getStatusHintSnippet(lastStatus, 16) + "\"";
                    }
                }
            }
        }

        return hint;
    }

    /*
	 * 
	 */
    @Override
    protected void updateStatusHint() {

        if (mUpdatingStatus == true) {
            mEditText.setHint(R.string.posting_tweet_ongoing);
            return;
        }

        String hint = getStatusHint(getComposeTweetDefault());

        if (hint == null && mListener != null) {
            String draftAsJsonString = mListener.getDraft();
            if (draftAsJsonString != null) {
                ComposeTweetDefault draft = new ComposeTweetDefault(
                        draftAsJsonString);
                hint = getStatusHint(draft);
            }
        }

        if (hint == null) {
            hint = getString(R.string.compose_tweet_default_hint);
        }

        mEditText.setHint(hint);

        if (mListener != null) {
            mListener.onStatusHintUpdate();
        }
    }

    /*
	 * 
	 */
    void setMediaFilePath(String filePath) {

        if (getComposeTweetDefault() == null) {
            setComposeTweetDefault(new ComposeTweetDefault(getApp()
                    .getCurrentAccountScreenName(), null, filePath));
        } else {
            getComposeTweetDefault().setMediaFilePath(filePath);
        }

        setMediaPreviewVisibility();
    }

    /*
	 * 
	 */
    private void setMediaPreviewVisibility() {
        mAttachImagePreview.setVisibility(View.GONE);

        if (_mComposeDefault != null
                && _mComposeDefault.getMediaFilePath() != null) {
            File imgFile = new File(_mComposeDefault.getMediaFilePath());
            if (imgFile.exists()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile
                            .getAbsolutePath());
                    mAttachImagePreview.setImageBitmap(Bitmap
                            .createScaledBitmap(bitmap, 200, 200, false));
                    mAttachImagePreview.setVisibility(View.VISIBLE);

                    if (mListener != null) {
                        mListener.onMediaAttach();
                    }
                } catch (OutOfMemoryError e) {
                }
            }
        }
    }

    /*
	 * 
	 */
    public void beginQuote(TwitterStatus statusToQuote) {
        setComposeTweetDefault(null);

        showCompose(getDefaultQuoteStatus(statusToQuote));
    }

    /*
	 * 
	 */
    public void beginShare(String initialShareString) {
        setComposeTweetDefault(null);

        showCompose(initialShareString);
    }

    public void retweetSelected(TwitterStatus status) {

        mRetweetStatus = status;

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .create();
        alertDialog.setMessage(getString(R.string.alert_retweet_options));
        // TODO: The order these buttons are set looks wrong, but appears
        // correctly. Have to ensure this is consistent on other devices.
        alertDialog.setButton2(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
        alertDialog.setButton3(getString(R.string.quote),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if (mRetweetStatus != null) {
                            beginQuote(mRetweetStatus);
                            mRetweetStatus = null;
                        }
                    }
                });
        alertDialog.setButton(getString(R.string.retweet),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if (mRetweetStatus != null) {
                            TwitterManager.get().setRetweet(mRetweetStatus.mId,
                                    null);
                            mRetweetStatus = null;
                        }
                    }
                });
        alertDialog.show();
    }

    /*
     * (non-Javadoc)
     * @see
     * com.tweetlanes.android.view.ComposeBaseFragment#getLayoutResourceId()
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.compose_tweet;
    }

    /*
     * (non-Javadoc)
     * @see com.tweetlanes.android.view.ComposeBaseFragment#onShowCompose()
     */
    @Override
    protected void onShowCompose() {

    }

    /*
     * (non-Javadoc)
     * @see com.tweetlanes.android.view.ComposeBaseFragment#onHideCompose()
     */
    @Override
    protected void onHideCompose() {

    }
}

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

import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.callback.TwitterFetchDirectMessagesFinishedCallback;
import org.tweetalib.android.model.TwitterDirectMessages;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.tweetlanes.android.Constant;
import com.tweetlanes.android.NotificationHelper;
import com.tweetlanes.android.R;
import com.tweetlanes.android.model.AccountDescriptor;
import com.tweetlanes.android.model.ComposeTweetDefault;
import com.tweetlanes.android.util.Util;

public class ComposeDirectMessageFragment extends ComposeBaseFragment {

    private EditText mSendToEditText;
    private String mOtherUserScreenName;
    private Handler mHandler = new Handler();

    /*
     * (non-Javadoc)
     * @see
     * com.tweetlanes.android.view.ComposeBaseFragment#onCreateView(android.
     * view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	View resultView = super.onCreateView(inflater, container, savedInstanceState);

	mSendToEditText = (EditText) resultView.findViewById(R.id.usernameEditText);
	mSendToEditText.setVisibility(View.GONE);

	return resultView;
    }

    /*
	 * 
	 */
    public void setOtherUserScreenName(String otherUserScreenName) {
	mOtherUserScreenName = otherUserScreenName;
    }

    /*
	 * 
	 */
    private String getOtherUserScreenName() {
	String otherUserScreenName = mOtherUserScreenName;
	if (otherUserScreenName == null) {
	    otherUserScreenName = mSendToEditText.getText().toString();
	}
	if (otherUserScreenName.isEmpty()) {
	    otherUserScreenName = null;
	}
	if (otherUserScreenName != null && otherUserScreenName.charAt(0) == '@') {
	    if (otherUserScreenName.length() > 1) {
		otherUserScreenName = otherUserScreenName.substring(1);
	    } else {
		otherUserScreenName = null;
	    }
	}

	return otherUserScreenName;
    }

    /*
	 * 
	 */
    @Override
    protected void updateComposeTweetDefault() {

	ComposeTweetDefault composeTweetDefault = null;
	String currentStatus = mEditText.getText().toString();

	if (Util.isValidString(currentStatus)) {
	    composeTweetDefault = new ComposeTweetDefault(getApp().getCurrentAccountScreenName(),
		    currentStatus, null, null);
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
	    ComposeTweetDefault tweetDefault = new ComposeTweetDefault(tweetDraftAsJson);
	    setComposeTweetDefault(tweetDefault);
	}
    }

    /*
	 * 
	 */
    TwitterFetchDirectMessagesFinishedCallback mOnSetStatusCallback = new TwitterFetchDirectMessagesFinishedCallback() {

	public void finished(TwitterFetchResult result, TwitterDirectMessages messages) {

	    /*
	     * if (mSendingNotification != null) {
	     * NotificationHelper.get().cancel(getActivity(),
	     * mSendingNotification); mSendingNotification = null; }
	     */

	    NotificationHelper.Builder builder = null;

	    mUpdatingStatus = false;
	    mEditText.setEnabled(true);
	    mSendButton.setEnabled(true);

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
		if (getActivity() != null && getActivity().getApplicationContext() != null) {
		    Toast.makeText(getActivity().getApplicationContext(),
			    getString(R.string.direct_message_posted_success),
			    Constant.DEFAULT_TOAST_DISPLAY_TIME).show();
		}
		// }

		if (mListener != null) {
		    mListener.saveDraft(null);
		    mListener.onStatusUpdateSuccess();
		}

	    } else if (result.getErrorMessage() != null) {
		builder = NotificationHelper.get().new Builder(getActivity(), true);
		builder.setContentTitle(getString(R.string.direct_message_posted_error));
		builder.setContentText(result.getErrorMessage());
		builder.setTicker(getString(R.string.direct_message_posted_error));
		builder.setAutoCancel(true);
	    }

	    if (builder != null) {
		NotificationHelper.get().notify(getActivity(), builder);
	    }

	    updateStatusHint();
	}
    };

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
		    composeDraft = new ComposeTweetDefault(getApp().getCurrentAccountScreenName(),
			    currentStatus);
		}
	    }
	}

	if (mListener != null) {
	    mListener.saveDraft(composeDraft == null ? null : composeDraft.toString());
	}
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

	    String otherUserScreenName = getOtherUserScreenName();
	    int statusLength = mStatusValidator.getTweetLength(status);
	    if (otherUserScreenName == null) {
		showSimpleAlert(R.string.alert_direct_message_no_recipient);
	    } else if (mStatusValidator.isValidTweet(status) == false) {
		showSimpleAlert(mStatusValidator.getTweetLength(status) <= getMaxPostLength() ? R.string.alert_direct_message_invalid
			: R.string.alert_direct_message_too_long);
	    } else if (statusLength > 0) {

		AccountDescriptor account = getApp().getCurrentAccount();
		if (account != null) {
		    mUpdatingStatus = true;
		    mEditText.setHint(null);
		    mEditText.setEnabled(false);
		    mSendButton.setEnabled(false);

		    TwitterManager.get().sendDirectMessage(account.getId(), otherUserScreenName,
			    status, mOnSetStatusCallback);

		    showToast(getString(R.string.posting_direct_message_ongoing));

		    /*
		     * ComposeTweetDefault currentStatus = new
		     * ComposeTweetDefault
		     * (getApp().getCurrentAccountScreenName(), status,
		     * getInReplyToId(), getMediaFilePath()); if (mListener !=
		     * null) { mListener.saveDraft(currentStatus.toString());
		     * mListener.onStatusUpdateRequest(); }
		     * setComposeTweetDefault(currentStatus);
		     */
		    updateStatusHint();

		    // mSendingNotification =
		    // NotificationHelper.get().notify(getActivity(), builder);
		}
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
		hint = getString(R.string.compose_direct_message_finish) + " \""
			+ getStatusHintSnippet(lastStatus, 16) + "\"";
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
	    mEditText.setHint(R.string.posting_direct_message_ongoing);
	    return;
	}

	String hint = getStatusHint(getComposeTweetDefault());

	if (hint == null && mListener != null) {
	    String draftAsJsonString = mListener.getDraft();
	    if (draftAsJsonString != null) {
		ComposeTweetDefault draft = new ComposeTweetDefault(draftAsJsonString);
		hint = getStatusHint(draft);
	    }
	}

	if (hint == null) {
	    hint = getString(R.string.compose_direct_message_default_hint);
	}

	mEditText.setHint(hint);

	if (mListener != null) {
	    mListener.onStatusHintUpdate();
	}
    }

    /*
     * (non-Javadoc)
     * @see com.tweetlanes.android.view.ComposeBaseFragment#onShowCompose()
     */
    @Override
    protected void onShowCompose() {
	if (mOtherUserScreenName == null) {
	    mSendToEditText.setVisibility(View.VISIBLE);

	    mHandler.post(new Runnable() {

		public void run() {
		    mSendToEditText.requestFocus();
		}
	    });

	} else {
	    mSendToEditText.setVisibility(View.GONE);
	}
    }

    /*
     * (non-Javadoc)
     * @see com.tweetlanes.android.view.ComposeBaseFragment#onHideCompose()
     */
    @Override
    protected void onHideCompose() {
	mSendToEditText.setVisibility(View.GONE);
    }

    /*
     * (non-Javadoc)
     * @see
     * com.tweetlanes.android.view.ComposeBaseFragment#getLayoutResourceId()
     */
    @Override
    protected int getLayoutResourceId() {
	return R.layout.compose_direct_message;
    }
}

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

import java.util.Date;

import org.socialnetlib.android.SocialNetConstant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.tweetlanes.android.App;
import com.tweetlanes.android.Constant;
import com.tweetlanes.android.R;
import com.tweetlanes.android.model.ComposeTweetDefault;
import com.tweetlanes.android.widget.EditClearText;
import com.tweetlanes.android.widget.EditClearText.EditClearTextListener;
import com.twitter.Validator;

public abstract class ComposeBaseFragment extends Fragment {

    /*
	 *
	 */
    public interface ComposeListener {

        public void onShowCompose();

        public void onHideCompose();

        public void onMediaAttach();

        public void onMediaDetach();

        public void onBackButtonPressed();

        public void onStatusUpdateRequest();

        public void onStatusUpdateSuccess();

        public void onStatusHintUpdate();

        public void saveDraft(String draftAsJsonString);

        public String getDraft();
    }

    // TODO: Replace with non-hardcoded values
    final int SHORT_URL_LENGTH = 20;
    final int SHORT_URL_LENGTH_HTTPS = 21;

    ImageButton mSendButton;
    EditClearText mEditText;
    TextView mCharacterCountTextView;
    Long mShowStartTime;
    Validator mStatusValidator = new Validator();

    ComposeListener mListener;
    boolean mHasFocus = false;
    boolean mIgnoreFocusChange = false;
    boolean mUpdatingStatus = false;

    /*
	 *
	 */
    public App getApp() {
        return (App) getActivity().getApplication();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View resultView = inflater.inflate(getLayoutResourceId(), null);

        mEditText = (EditClearText) resultView
                .findViewById(R.id.statusEditText);
        mEditText.addTextChangedListener(mTextChangedListener);
        mEditText.setOnFocusChangeListener(mOnFocusChangeListener);
        mEditText.setEditClearTextListener(mEditClearTextListener);

        mSendButton = (ImageButton) resultView
                .findViewById(R.id.sendTweetButton);
        mSendButton.setOnClickListener(mOnSendTweetClickListener);

        mCharacterCountTextView = (TextView) resultView
                .findViewById(R.id.characterCount);

        updateStatusHint();

        return resultView;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (hasFocus()) {
            saveCurrentAsDraft();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        if (hasFocus()) {
            showCompose();
        }
    }

    protected int getMaxPostLength() {
        return getApp().getCurrentAccount().getSocialNetType() == SocialNetConstant.Type.Appdotnet ? 256
                : 140;
    }

    /*
	 *
	 */
    void showToast(String message) {
        if (getActivity() != null
                && getActivity().getApplicationContext() != null) {
            Toast.makeText(getActivity().getApplicationContext(), message,
                    Constant.DEFAULT_TOAST_DISPLAY_TIME).show();
        }
    }

    /*
	 *
	 */
    void showSimpleAlert(int stringID) {
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .create();
        alertDialog.setMessage(getString(stringID));
        alertDialog.setButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
        alertDialog.show();
    }

    /*
	 *
	 */
    public boolean hasFocus() {
        return mHasFocus;
    }

    /*
	 *
	 */
    void setComposeTweetListener(ComposeListener listener) {
        mListener = listener;
    }

    /*
	 *
	 */
    String getFormattedStatus(String status) {
        status.replaceAll("\\s+$", "");
        return status;
    }

    /*
	 *
	 */
    public boolean releaseFocus(boolean saveCurrentTweet) {

        clearCompose(saveCurrentTweet);
        return hideCompose();
    }

    /*
	 *
	 */
    boolean hideCompose() {

        if (mHasFocus == true) {

            hideKeyboard();

            if (mListener != null) {
                mListener.onHideCompose();
            }
            onHideCompose();

            mHasFocus = false;
            return true;
        }
        return false;
    }

    /*
	 *
	 */
    void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(mEditText,
                InputMethodManager.SHOW_IMPLICIT);
    }

    void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mEditText.getWindowToken(),
                0);
    }

    /*
	 *
	 */
    OnFocusChangeListener mOnFocusChangeListener = new OnFocusChangeListener() {

        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus == true && mIgnoreFocusChange == false) {
                showCompose();
            }
        }
    };

    /*
     * Used as a bit of a hack to prevent the Compose Tweet view enabling when
     * coming back from the ActionBar Search
     */
    void setIgnoreFocusChange(boolean ignoreFocusChange) {
        mIgnoreFocusChange = ignoreFocusChange;
    }

    /*
	 *
	 */
    TextWatcher mTextChangedListener = new TextWatcher() {

        public void afterTextChanged(Editable s) {
            String asString = s.toString();
            configureCharacterCountForString(asString);
            if (asString == null || asString.equals("") == true) {
                setComposeTweetDefault(null);
                updateStatusHint();
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
        }
    };

    /*
	 *
	 */
    OnClickListener mOnSendTweetClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            String status = mEditText.getText().toString();
            onSendClick(status);
        }
    };

    protected abstract void onSendClick(String status);

    /*
	 *
	 */
    EditClearTextListener mEditClearTextListener = new EditClearTextListener() {

        @Override
        public boolean canClearText() {
            if (mShowStartTime != null) {
                long diff = new Date().getTime() - mShowStartTime.longValue();
                if (diff < 500) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean onBackButtonPressed() {
            if (mListener != null) {
                mListener.onBackButtonPressed();
            }
            hideCompose();
            return true;
        }

        @Override
        public void onClearPressed() {
            clearCompose(false);
        }

        @Override
        public void onTouch(View v, MotionEvent event) {
            if (mHasFocus == false) {
                showCompose();
            }
        }

        @Override
        public void onSizeChanged(int xNew, int yNew, int xOld, int yOld,
                int xInitial, int yInitial) {

            if (mEditText.getLineCount() >= 2) {
                mCharacterCountTextView.setVisibility(View.VISIBLE);
            } else {
                mCharacterCountTextView.setVisibility(View.GONE);
            }
        }

    };

    /*
	 *
	 */
    void clearCompose(boolean saveCurrentTweet) {

        if (saveCurrentTweet) {
            saveCurrentAsDraft();
        }

        setComposeTweetDefault(null);
        if (mListener != null) {
            mListener.onMediaDetach();
        }

        // NOTE: Changing these text values causes a crash during the copy/paste
        // process.
        mEditText.setText(null);
        updateStatusHint();
    }

    /*
	 *
	 */
    void showCompose() {
        showCompose(null);
    }

    void showCompose(String defaultStatus) {

        if (mHasFocus == false) {

            mHasFocus = true;
            mShowStartTime = new Date().getTime();
            if (defaultStatus == null) {
                if (getComposeTweetDefault() == null && mListener != null) {
                    String savedDraftAsJsonString = mListener.getDraft();
                    if (savedDraftAsJsonString != null) {
                        setComposeTweetDefault(new ComposeTweetDefault(
                                savedDraftAsJsonString));
                    }
                }

                if (getComposeTweetDefault() != null) {
                    defaultStatus = getComposeTweetDefault().getStatus();
                }
            }
            mEditText.setText(defaultStatus);

            if (defaultStatus != null && defaultStatus.length() > 1) {
                mEditText.setSelection(defaultStatus.length());
            }

            if (mListener != null) {
                mListener.onShowCompose();
            }

            onShowCompose();
        }

        showKeyboard();
    }

    /*
	 *
	 */
    protected static String getStatusHintSnippet(String status, int maxLength) {

        if (status.length() == 0) {
            return null;
        } else if (status.length() < maxLength) {
            return status;
        }

        return status.substring(0, maxLength) + "…";
    }

    /*
	 *
	 */
    protected ComposeTweetDefault _mComposeDefault;

    protected ComposeTweetDefault getComposeTweetDefault() {
        return _mComposeDefault;
    }

    /*
	 *
	 */
    protected void setComposeTweetDefault(
            ComposeTweetDefault composeTweetDefault) {
        _mComposeDefault = composeTweetDefault;
    }

    /*
	 *
	 */
    public void setComposeDefault(ComposeTweetDefault other) {
        if (other != null) {
            setComposeTweetDefault(new ComposeTweetDefault(other));
        } else {
            setComposeTweetDefault(null);
        }
        updateStatusHint();
    }

    /*
	 *
	 */
    void configureCharacterCountForString(String string) {

        int length = mStatusValidator.getTweetLength(string);
        if (length > 0) {
            int remaining = getMaxPostLength() - length;
            if (_mComposeDefault != null
                    && _mComposeDefault.getMediaFilePath() != null) {
                remaining -= SHORT_URL_LENGTH_HTTPS - 1;
            }

            mCharacterCountTextView.setText("" + remaining);
        }
    }

    protected abstract void saveCurrentAsDraft();

    protected abstract void updateStatusHint();

    protected abstract void updateComposeTweetDefault();

    protected abstract String getTweetDefaultDraft();

    protected abstract void setTweetDefaultFromDraft(String tweetDraftAsJson);

    protected abstract int getLayoutResourceId();

    protected abstract void onShowCompose();

    protected abstract void onHideCompose();
}

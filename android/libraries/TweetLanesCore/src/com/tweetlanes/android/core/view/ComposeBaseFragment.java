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

package com.tweetlanes.android.core.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.model.AccountDescriptor;
import com.tweetlanes.android.core.model.ComposeTweetDefault;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.widget.EditClearText;
import com.tweetlanes.android.core.widget.EditClearText.EditClearTextListener;
import com.twitter.Validator;

import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

abstract class ComposeBaseFragment extends Fragment {

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

    ImageButton mSendButton;
    EditClearText mEditText;
    private EditText mAutocompleteTarget;
    private TextView mCharacterCountTextView;
    private Long mShowStartTime;
    final Validator mStatusValidator = new Validator();
    private ListView mAutocompleteListView;

    ComposeListener mListener;
    private boolean mHasFocus = false;
    private boolean mIgnoreFocusChange = false;
    boolean mUpdatingStatus = false;

    /*
     *
	 */
    App getApp() {
        if (getActivity() == null || getActivity().getApplication() == null) {
            return null;
        }
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

        mCharacterCountTextView.setVisibility(View.VISIBLE);

        mAutocompleteListView = (ListView) resultView.findViewById(R.id.autocompleteListView);

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

    int getMaxPostLength() {
        if (getApp() == null) {
            return 140;
        }

        AccountDescriptor account = getApp().getCurrentAccount();

        if (account == null) {
            return 140;
            //best to use the lower number in case
        } else {
            return account.getSocialNetType() == SocialNetConstant.Type.Appdotnet ? 256 : 140;
        }
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
    public void releaseFocus(boolean saveCurrentTweet) {

        clearCompose(saveCurrentTweet);
        setMediaPreviewVisibility();
        hideCompose();
    }

    /*
	 *
	 */
    boolean hideCompose() {

        if (mHasFocus) {

            hideKeyboard();
            if (mAutocompleteListView != null) {
                mAutocompleteListView.setVisibility(View.GONE);
            }

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
    private final OnFocusChangeListener mOnFocusChangeListener = new OnFocusChangeListener() {

        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus && !mIgnoreFocusChange) {
                showCompose();
                setMediaPreviewVisibility();
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
    private final TextWatcher mTextChangedListener = new TextWatcher() {

        public void afterTextChanged(Editable s) {
            String asString = s.toString();
            configureCharacterCountForString(asString);
            if (asString == null || asString.equals("")) {
                if (mListener.getDraft() == null) {
                    setComposeTweetDefault(null);
                    updateStatusHint();
                }
            }

            autoComplete(asString, mEditText);
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
        }
    };

    void autoComplete(String text, EditText editText) {
        if (mAutocompleteListView == null) {
            return;
        }

        if (text == null || text.length() == 0) {
            mAutocompleteListView.setVisibility(View.GONE);
            return;
        }

        int currentStart = editText.getSelectionStart();
        int currentEnd = editText.getSelectionEnd();
        int currentPosition = text.length() - 1;
        if (currentStart == currentEnd) {
            currentPosition = currentStart;
        }
        if (currentPosition < 0) currentPosition = 0;

        String lastWholeWord = "";

        for (int i = currentPosition; i > 0; i--) {
            String nextChar = text.substring(i-1,i).toLowerCase();
            if (nextChar.equals(" ") || nextChar.equals(".")) {
                break;
            }
            lastWholeWord = nextChar + lastWholeWord;
        }

        if (lastWholeWord.startsWith("@")) {
            List<TwitterUser> autoCompleteMentions = getAutoCompleteMentions(lastWholeWord);

            mAutocompleteListView.setVisibility(View.VISIBLE);
            mAutocompleteListView.setAdapter(new AutoCompleteMentionAdapter(this.getActivity(), autoCompleteMentions));
            mAutocompleteTarget = editText;
            mAutocompleteListView.setOnItemClickListener(mOnAutoCompleteItemClickListener);
        } else if (lastWholeWord.startsWith("#")) {
            List<String> autoCompleteHashtags = getAutoCompleteHashtags(lastWholeWord);

            mAutocompleteListView.setVisibility(View.VISIBLE);
            mAutocompleteListView.setAdapter(new AutoCompleteHashtagAdapter(this.getActivity(), autoCompleteHashtags));
            mAutocompleteTarget = editText;
            mAutocompleteListView.setOnItemClickListener(mOnAutoCompleteItemClickListener);
        } else {
            mAutocompleteListView.setVisibility(View.GONE);
        }
    }

    private class AutoCompleteMentionAdapter extends android.widget.BaseAdapter {

        final Context mContext;
        final List<TwitterUser> mData;

        public AutoCompleteMentionAdapter(Context context, List<TwitterUser> data) {
            mContext = context;
            mData = data;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public TwitterUser getItem(int i) {
            return mData.get(i);
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            UserHolder holder;

            if (row == null) {
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                row = inflater.inflate(R.layout.autocompletemention_row, parent, false);

                holder = new UserHolder();
                holder.AvatarImage = (ImageView) row.findViewById(R.id.autoCompleteAvatar);
                holder.ScreenName = (TextView) row.findViewById(R.id.autoCompleteScreenName);
                holder.FullName = (TextView) row.findViewById(R.id.autoCompleteFullName);

                int dimensionValue = mContext.getResources().getDimensionPixelSize(R.dimen.font_size_medium);
                int imageSize = (int) mContext.getResources().getDimension(R.dimen.font_size_medium);

                holder.ScreenName.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimensionValue);
                holder.FullName.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimensionValue);
                holder.AvatarImage.setMaxWidth(imageSize);
                holder.AvatarImage.setMaxHeight(imageSize);

                row.setTag(holder);
            } else {
                holder = (UserHolder) row.getTag();
            }

            TwitterUser user = mData.get(position);

            if (user == null) {
                return row;
            }

            holder.ScreenName.setText("@" + user.getScreenName(), TextView.BufferType.NORMAL);
            holder.FullName.setText(user.getName(), TextView.BufferType.NORMAL);
            setProfileImage(user, holder.AvatarImage);

            return row;
        }

        class UserHolder {
            public ImageView AvatarImage;
            public TextView ScreenName;
            public TextView FullName;
        }
    }

    private class AutoCompleteHashtagAdapter extends android.widget.BaseAdapter {

        final Context mContext;
        final List<String> mData;

        public AutoCompleteHashtagAdapter(Context context, List<String> data) {
            mContext = context;
            mData = data;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public String getItem(int i) {
            return mData.get(i);
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            HashtagHolder holder;

            if (row == null) {
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                row = inflater.inflate(R.layout.autocompletehashtag_row, parent, false);

                holder = new HashtagHolder();
                holder.Hashtag = (TextView) row.findViewById(R.id.autoCompleteHashtag);

                int dimensionValue = mContext.getResources().getDimensionPixelSize(R.dimen.font_size_medium);

                holder.Hashtag.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimensionValue);

                row.setTag(holder);
            } else {
                holder = (HashtagHolder) row.getTag();
            }

            String hashtag = mData.get(position);

            if (hashtag == null) {
                return row;
            }

            holder.Hashtag.setText(hashtag, TextView.BufferType.NORMAL);

            return row;
        }

        class HashtagHolder {
            public TextView Hashtag;
        }
    }


    private void setProfileImage(TwitterUser user, ImageView avatar) {
        String profileImageUrl = user.getProfileImageUrl(TwitterManager.ProfileImageSize.NORMAL);
        if (profileImageUrl != null) {

            if (AppSettings.get().downloadFeedImages()) {

                LazyImageLoader profileImageLoader = getApp().getProfileImageLoader();
                if (profileImageLoader != null) {

                    profileImageLoader.displayImage(profileImageUrl, avatar);
                }
            }
        }
    }


    private final AdapterView.OnItemClickListener mOnAutoCompleteItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TextView textView = (TextView) view.findViewById(R.id.autoCompleteScreenName);
            if (textView == null) {
                textView = (TextView) view.findViewById(R.id.autoCompleteHashtag);
            }

            String autoCompleteText = String.valueOf(textView.getText());
            String editText = String.valueOf(mAutocompleteTarget.getText());
            int currentStart = mAutocompleteTarget.getSelectionStart();
            int currentEnd = mAutocompleteTarget.getSelectionEnd();
            int currentPosition = editText.length() - 1;
            if (currentStart == currentEnd) {
                currentPosition = currentStart;
            }
            if (currentPosition < 0) currentPosition = 0;

            int autoCompleteStart = 0;

            for (int i = currentPosition; i > 0; i--) {
                String nextChar = editText.substring(i-1,i).toLowerCase();
                if (nextChar.equals(" ") || nextChar.equals(".")) {
                    break;
                }
                autoCompleteStart = i-1;
            }


            String newText = editText.substring(0,autoCompleteStart) + autoCompleteText + " " + editText.substring(currentPosition);
            mAutocompleteTarget.setText(newText);
            mAutocompleteTarget.setSelection(autoCompleteStart + autoCompleteText.length());

            mAutocompleteListView.setVisibility(View.GONE);
        }
    };

    private ArrayList<TwitterUser> getAutoCompleteMentions(String text) {
        ArrayList<TwitterUser> list = new ArrayList<TwitterUser>();
        List<TwitterUser> users = TwitterManager.get().getFetchUserInstance().getCachedUsers();

        text = text.replace("@", "");

        for (TwitterUser user : users) {

            if (user.getScreenName().toLowerCase().contains(text.toLowerCase()) ||
                    user.getName().toLowerCase().contains(text.toLowerCase())) {
                list.add(user);
            }
        }

        Collections.sort(list, new SortUserName());

        return list;
    }

    private ArrayList<String> getAutoCompleteHashtags(String text) {
        ArrayList<String> list = new ArrayList<String>();
        List<String> hashtags = TwitterManager.get().getFetchStatusesInstance().getCachedHashtags();

        if (hashtags == null) {
            return list;
        }

        for (String tag : hashtags) {
            if (tag.toLowerCase().startsWith(text.toLowerCase())) {
                list.add(tag);
            }
        }

        Collections.sort(list, new SortAlpha());

        return list;
    }


    private class SortUserName implements Comparator<Object> {
        public int compare(Object o1, Object o2) {
            TwitterUser s1 = (TwitterUser) o1;
            TwitterUser s2 = (TwitterUser) o2;
            return s1.getScreenName().toLowerCase().compareTo(s2.getScreenName().toLowerCase());
        }
    }

    private class SortAlpha implements Comparator<Object> {
        public int compare(Object o1, Object o2) {
            String s1 = (String) o1;
            String s2 = (String) o2;
            return s1.toLowerCase().compareTo(s2.toLowerCase());
        }

    }

    protected void lockScreenRotation() {
        if (getActivity() != null) {
            switch (getActivity().getResources().getConfiguration().orientation) {
                case Configuration.ORIENTATION_PORTRAIT:
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
                case Configuration.ORIENTATION_LANDSCAPE:
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
            }
        }
    }

    protected void resetScreenRotation() {
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    /*
     *
     */
    private final OnClickListener mOnSendTweetClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            lockScreenRotation();

            String status = mEditText.getText().toString();
            if (status == null || status.equals("")) {
                ComposeTweetDefault composeTweetDefault = getComposeTweetDefault();
                if (composeTweetDefault != null) {
                    status = composeTweetDefault.getStatus();
                }
            }

            onSendClick(status);
        }
    };

    protected abstract void onSendClick(String status);

    protected abstract void setMediaPreviewVisibility();

    /*
     */
    private final EditClearTextListener mEditClearTextListener = new EditClearTextListener() {

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
            setMediaPreviewVisibility();
            return true;
        }

        @Override
        public void onTouch(View v, MotionEvent event) {
            if (!mHasFocus) {
                showCompose();
                setMediaPreviewVisibility();
            }
        }
    };

    /*
	 *
	 */
    void clearCompose(boolean saveCurrentTweet) {

        if (saveCurrentTweet) {
            saveCurrentAsDraft();
            updateComposeTweetDefault();
        } else {
            setComposeTweetDefault(null);

            if (mListener != null) {
                mListener.onMediaDetach();
            }

            if (getApp() != null) {
                getApp().clearTweetDraft();
            }
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

        if (!mHasFocus) {

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
                configureCharacterCountForString(defaultStatus);
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
    static String getStatusHintSnippet(String status, int maxLength) {

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
    ComposeTweetDefault _mComposeDefault;

    ComposeTweetDefault getComposeTweetDefault() {
        return _mComposeDefault;
    }

    /*
	 *
	 */
    void setComposeTweetDefault(
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

        if (string != null) {
            int length = mStatusValidator.getTweetLength(string);
            if (length > 0) {
                int remaining = getMaxPostLength() - length;
                if (_mComposeDefault != null && _mComposeDefault.getMediaFilePath() != null) {
                    remaining -= 22;
                }

                mCharacterCountTextView.setText("" + remaining);
            } else {
                mCharacterCountTextView.setText("" + getMaxPostLength());
            }
        } else {
            mCharacterCountTextView.setText("");
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

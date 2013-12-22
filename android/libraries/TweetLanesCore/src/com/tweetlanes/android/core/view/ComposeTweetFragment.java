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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.model.ComposeTweetDefault;
import com.tweetlanes.android.core.util.Util;

import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchStatus.FinishedCallback;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatusUpdate;

import java.io.File;
import java.io.IOException;

public class ComposeTweetFragment extends ComposeBaseFragment {
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;

    private ImageView mAttachImagePreview;

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.ComposeBaseFragment#onCreateView(android.
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

        mAttachImagePreview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                if (mListener != null) {
                    mListener.onMediaDetach();
                }
                getComposeTweetDefault().clearMediaFilePath();
                mAttachImagePreview.setVisibility(View.GONE);
                return true;
            }
        });

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

        String currentStatus = mEditText.getText().toString();

        if (Util.isValidString(currentStatus)) {
            ComposeTweetDefault composeTweetDefault = new ComposeTweetDefault(getApp()
                    .getCurrentAccountScreenName(), currentStatus,
                    getInReplyToId(), getMediaFilePath());

            setComposeTweetDefault(composeTweetDefault);
        }
    }

    /*
	 *
	 */
    @Override
    protected String getTweetDefaultDraft() {

        if (getComposeTweetDefault() != null) {
            String result = getComposeTweetDefault().toString();
            if (result != null && !result.equals("")) {
                return result;
            }
        } else {
            if (mListener != null) {
                String draftAsJsonString = mListener.getDraft();
                if (draftAsJsonString != null && !draftAsJsonString.equals("")) {
                    return draftAsJsonString;
                }
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
    private final FinishedCallback mOnSetStatusCallback = TwitterManager.get()
            .getFetchStatusInstance().new FinishedCallback() {

        @Override
        public void finished(TwitterFetchResult result, TwitterStatus status) {

            /*
             * if (mSendingNotification != null) {
             * NotificationHelper.get().cancel(getActivity(),
             * mSendingNotification); mSendingNotification = null; }
             */

            mUpdatingStatus = false;
            mEditText.setEnabled(true);
            mSendButton.setEnabled(true);

            if (result.isSuccessful()) {
                releaseFocus(false);

                if (mListener != null) {
                    mListener.saveDraft(null);
                    mListener.onStatusUpdateSuccess();
                }

            }
            updateStatusHint();

            resetScreenRotation();
        }

    };

    /*
	 *
	 */
    String getDefaultQuoteStatus(TwitterStatus statusToQuote) {
        if (statusToQuote.mStatus.length() > 0) {
            String quote;
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
        if (currentStatus != null && !currentStatus.equals("")) {
            if (mStatusValidator.getTweetLength(currentStatus) > 0) {
                if (getComposeTweetDefault() != null) {
                    getComposeTweetDefault().updateStatus(currentStatus);
                    if (!getComposeTweetDefault().isPlaceholderStatus()) {
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
     *
     * @see
     * com.tweetlanes.android.core.view.ComposeBaseFragment#clearCompose(boolean)
     */
    @Override
    void clearCompose(boolean saveCurrentTweet) {

        super.clearCompose(saveCurrentTweet);
        mAttachImagePreview.setVisibility(View.GONE);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.ComposeBaseFragment#onSendClick(java.lang
     * .String)
     */
    @Override
    protected void onSendClick(String status) {
        if (status != null) {
            int statusLength = mStatusValidator.getTweetLength(status);
            if (statusLength == 0) {
                showSimpleAlert(getApp().getCurrentAccount().getSocialNetType() == SocialNetConstant.Type.Twitter ? R
                        .string.alert_status_empty : R.string.alert_status_empty_adn);
            } else if (!mStatusValidator.isValidTweet(status, getMaxPostLength())) {
                showSimpleAlert(mStatusValidator.getTweetLength(status) <= getMaxPostLength() ? R.string.alert_status_invalid
                        : (getApp().getCurrentAccount().getSocialNetType() == SocialNetConstant.Type.Twitter ? R
                        .string.alert_status_too_long : R.string.alert_status_too_long_adn));
            } else if (statusLength > 0) {
                TwitterStatusUpdate statusUpdate = new TwitterStatusUpdate(
                        status, getInReplyToId());
                statusUpdate.setMediaFilePath(getMediaFilePath());

                mUpdatingStatus = true;
                mEditText.setHint(null);
                mEditText.setEnabled(false);
                mSendButton.setEnabled(false);

                TwitterManager.get().setStatus(statusUpdate,
                        mOnSetStatusCallback);

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
            }
        }
    }

    /*
	 *
	 */
    private String getStatusHint(ComposeTweetDefault composeTweetDefault) {

        String hint = null;

        if (composeTweetDefault != null && getApp() != null) {
            String lastStatus = composeTweetDefault.getStatus();
            if (lastStatus != null && lastStatus != "") {
                lastStatus = Util.trimLeftRight(lastStatus);
                if (composeTweetDefault.getInReplyToStatusId() != null) {
                    hint = getString(R.string.compose_tweet_reply_to) + " "
                            + getStatusHintSnippet(lastStatus, 20);
                } else {
                    SocialNetConstant.Type socialNetType = getApp().getCurrentAccount().getSocialNetType();
                    String[] words = lastStatus.split(" ");
                    if (words.length == 1 && words[0].length() > 0
                            && words[0].charAt(0) == '@') {
                        hint = getString(socialNetType == SocialNetConstant.Type.Twitter ? R.string
                                .compose_tweet_to_user_hint : R.string.compose_tweet_to_user_hint_adn)
                                + " " + getStatusHintSnippet(words[0], 18);
                    } else {
                        hint = getString(socialNetType == SocialNetConstant.Type.Twitter ? R.string
                                .compose_tweet_finish : R.string.compose_tweet_finish_adn) + " \""
                                + getStatusHintSnippet(lastStatus, 16) + "\"";
                        super.configureCharacterCountForString(lastStatus);
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

        if (mUpdatingStatus) {
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
                setComposeTweetDefault(draft);
            }
        }

        if (hint == null && isAdded()) {
            hint = getString(R.string.compose_tweet_default_hint) + getApp().getCurrentAccount().getScreenName() + "?";
            configureCharacterCountForString("");
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
        updateStatusHint();
    }

    /*
	 *
	 */
    public void setMediaPreviewVisibility() {
        mAttachImagePreview.setVisibility(View.GONE);

        if (_mComposeDefault != null && _mComposeDefault.getMediaFilePath() != null) {
            File imgFile = new File(_mComposeDefault.getMediaFilePath());
            if (imgFile.exists()) {
                try {
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                    options.inSampleSize = calculateInSampleSize(options, THUMBNAIL_WIDTH,
                            THUMBNAIL_HEIGHT);

                    options.inJustDecodeBounds = false;
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                    try {
                        ExifInterface exif = new ExifInterface(imgFile.getAbsolutePath());
                        int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL);
                        int rotationInDegrees = exifToDegrees(exifOrientation);
                        if (rotationInDegrees != 0f) {
                            Matrix matrix = new Matrix();
                            matrix.preRotate(rotationInDegrees);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                    bitmap.getHeight(), matrix, true);
                        }
                    } catch (IOException e) {
                        Log.e(ComposeTweetFragment.class.getName(), "could not get EXIF info", e);
                    }

                    mAttachImagePreview.setImageBitmap(Bitmap.createScaledBitmap(bitmap,
                            THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, false));
                    mAttachImagePreview.setVisibility(View.VISIBLE);

                    if (mListener != null) {
                        mListener.onMediaAttach();
                    }
                } catch (OutOfMemoryError e) {
                    Log.e(ComposeTweetFragment.class.getName(), "out of memory when adding image",
                            e);
                }
            }
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
                                             int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    private static int exifToDegrees(final int exifOrientation) {
        int degrees = 0;
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            degrees = 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            degrees = 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            degrees = 270;
        }
        return degrees;
    }

    /*
	 *
	 */
    public void beginQuote(TwitterStatus statusToQuote) {
        String statusString = getDefaultQuoteStatus(statusToQuote);
        setComposeTweetDefault(new ComposeTweetDefault(getApp().getCurrentAccountScreenName(), statusString));

        showCompose(statusString);
    }

    /*
	 *
	 */
    public void beginShare(String initialShareString) {
        setComposeTweetDefault(new ComposeTweetDefault(getApp().getCurrentAccountScreenName(), initialShareString));

        showCompose(initialShareString);
    }

    /*
	 *
	 */
    private TwitterStatus mRetweetStatus;

    public void retweetSelected(TwitterStatus status, final FinishedCallback callback, final FinishedCallback showRTCallback) {

        mRetweetStatus = status;

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .create();
        alertDialog.setMessage(getString(R.string.alert_retweet_options));
        // TODO: The order these buttons are set looks wrong, but appears
        // correctly. Have to ensure this is consistent on other devices.
        alertDialog.setButton2(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        callback.finished(new TwitterFetchResult(true, "CancelPressed"), null);
                    }
                });
        alertDialog.setButton3(getString(R.string.quote),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if (mRetweetStatus != null) {
                            callback.finished(new TwitterFetchResult(true, "QutotePressed"), null);
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
                                    callback);

                            mRetweetStatus = null;
                        }
                        showRTCallback.finished(new TwitterFetchResult(true, "Show"), null);
                    }
                });
        alertDialog.show();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.core.view.ComposeBaseFragment#getLayoutResourceId()
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.compose_tweet;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.ComposeBaseFragment#onShowCompose()
     */
    @Override
    protected void onShowCompose() {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.ComposeBaseFragment#onHideCompose()
     */
    @Override
    protected void onHideCompose() {
    }
}

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
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Layout;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.AppSettings.StatusSize;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.util.Util;
import com.tweetlanes.android.core.widget.urlimageviewhelper.UrlImageViewCallback;
import com.tweetlanes.android.core.widget.urlimageviewhelper.UrlImageViewHelper;

import org.appdotnet4j.model.AdnMedia;
import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterManager.ProfileImageSize;
import org.tweetalib.android.model.TwitterMediaEntity;
import org.tweetalib.android.model.TwitterMediaEntity.Size;
import org.tweetalib.android.model.TwitterStatus;

public class TweetFeedItemView extends LinearLayout {

    private Context mContext;
    private int mPosition;
    private boolean mLoadsTweetSpotlight;
    private TwitterStatus mTwitterStatus;
    private Callbacks mCallbacks;
    private TextView mAuthorScreenNameTextView;
    private TextView mAuthorNameTextView;
    private TextView mStatusTextView;
    private TextView mTweetDetailsView;
    private ImageView mConversationToggle;
    private ConversationView mConversationView;
    private View mMessageBlock;
    private QuickContactDivot mAvatar;
    private RelativeLayout mPreviewImageContainer;
    private ImageView mPreviewImageView;
    private ImageView mPreviewPlayImageView;
    private ImageView mStatusIndicatorImageView;
    private boolean mIsConversationItem;
    private Path mPath = new Path();
    private Paint mPaint = new Paint();

    private boolean mConversationExpanded;

    /*
     *
     */
    public interface Callbacks {

        public boolean onSingleTapConfirmed(View view, int position);

        public void onLongPress(View view, int position);

        public void onUrlClicked(TwitterStatus status);

        public Activity getActivity();

        public LayoutInflater getLayoutInflater();

        public void onConversationViewToggle(long statusId, boolean show);

        public LazyImageLoader getProfileImageLoader();

        public LazyImageLoader getPreviewImageLoader();
    }

    /*
     *
     */
    public TweetFeedItemView(Context context) {
        super(context);
        init(context);
    }

    public TweetFeedItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TweetFeedItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void init(Context context) {
        mContext = context;
    }

    /*
     *
	 */
    public void configure(TwitterStatus twitterStatus, int position, Callbacks callbacks, boolean loadsTweetSpotlight,
                          boolean showRetweetCount, boolean showConversationView, boolean isConversationItem, boolean resize,
                          final SocialNetConstant.Type socialNetType, final String currentAccountKey) {

        StatusSize statusSize = AppSettings.get().getCurrentStatusSize();

        mTwitterStatus = twitterStatus;
        mIsConversationItem = isConversationItem;
        mPosition = position;
        mCallbacks = callbacks;
        mLoadsTweetSpotlight = loadsTweetSpotlight;

        mAuthorScreenNameTextView = (TextView) findViewById(R.id.authorScreenName);
        if (mAuthorScreenNameTextView != null) {
            mAuthorScreenNameTextView.setText("@"
                    + twitterStatus.getAuthorScreenName());

            if (resize) {
                Integer textSize = null;
                if (statusSize == StatusSize.Small) {
                    textSize = 14;
                }

                if (textSize != null) {
                    mAuthorScreenNameTextView.setTextSize(
                            TypedValue.COMPLEX_UNIT_SP, textSize);
                }
            }
        }
        mAuthorNameTextView = (TextView) findViewById(R.id.authorName);
        if (mAuthorNameTextView != null) {
            mAuthorNameTextView.setText(twitterStatus.getAuthorName());
        }

        mTweetDetailsView = (TextView) findViewById(R.id.tweet_details);
        if (mTweetDetailsView != null) {

            boolean showTweetSource = AppSettings.get().showTweetSource();

            String verb = socialNetType == SocialNetConstant.Type.Twitter ? "Retweeted" : "Reposted";

            if (twitterStatus.mIsRetweet == true) {

                String text = verb + " by " + twitterStatus.mUserName;
                if (showTweetSource) {
                    text += " " + mContext.getString(R.string.via) + " " + mTwitterStatus.mSource;
                }
                mTweetDetailsView.setText(text);
            } else if (showRetweetCount == true && twitterStatus.mRetweetCount > 0) {
                mTweetDetailsView.setText(verb + " " + twitterStatus.mRetweetCount + " times.");
            } else {
                if (showTweetSource) {
                    mTweetDetailsView.setText(mContext.getString(R.string.via) + " " + mTwitterStatus.mSource);
                } else {
                    mTweetDetailsView.setVisibility(GONE);
                }
            }
        }

        mConversationExpanded = showConversationView;
        mConversationToggle = (ImageView) findViewById(R.id.conversationToggle);
        if (mConversationToggle != null) {

            if (twitterStatus.mInReplyToStatusId != null) {
                mConversationToggle.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mConversationExpanded = !mConversationExpanded;
                        configureConversationView(socialNetType, currentAccountKey);
                    }
                });

                if (showConversationView) {
                    insertConversationView();
                    mConversationView.setVisibility(GONE);
                    configureConversationView(socialNetType, currentAccountKey);
                }
            } else {
                mConversationToggle.setVisibility(GONE);
            }
        }

        mStatusTextView = (TextView) findViewById(R.id.status);

        Integer textSize = null;
        switch (statusSize) {
            case ExtraSmall:
                textSize = R.dimen.font_size_extra_small;
                break;
            case Small:
                textSize = R.dimen.font_size_small;
                break;
            case Medium:
                textSize = R.dimen.font_size_medium;
                break;
            case Large:
                textSize = R.dimen.font_size_large;
                break;
            case ExtraLarge:
                textSize = R.dimen.font_size_extra_large;
                break;
        }

        if (textSize != null && resize) {
            int dimensionValue = mContext.getResources().getDimensionPixelSize(
                    textSize);
            mStatusTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    dimensionValue);
        }

        // Spanned statusSpanned = mIsConversationItem ?
        // twitterStatus.getStatusFullSpanned() :
        // twitterStatus.mStatusSlimSpanned;
        Spanned statusSpanned = twitterStatus.mStatusFullSpanned;// .getStatusFullSpanned()
        // :
        // twitterStatus.mStatusSlimSpanned;
        if (statusSpanned != null) {
            mStatusTextView.setText(statusSpanned);
            mStatusTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        TextView prettyDateTextView = (TextView) findViewById(R.id.pretty_date);
        if (prettyDateTextView != null) {
            prettyDateTextView.setText(Util
                    .getPrettyDate(mTwitterStatus.mCreatedAt));
        }

        TextView fullDateTextView = (TextView) findViewById(R.id.full_date);
        if (fullDateTextView != null) {
            fullDateTextView.setText(Util
                    .getFullDate(mTwitterStatus.mCreatedAt));
        }

        mStatusIndicatorImageView = (ImageView) findViewById(R.id.status_indicator);
        if (mStatusIndicatorImageView != null) {
            if (mTwitterStatus.mIsFavorited && mTwitterStatus.mIsRetweetedByMe) {
                mStatusIndicatorImageView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.status_indicator_rt_fav));
                mStatusIndicatorImageView.setVisibility(View.VISIBLE);
            } else if (mTwitterStatus.mIsFavorited) {
                mStatusIndicatorImageView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.status_indicator_fav));
                mStatusIndicatorImageView.setVisibility(View.VISIBLE);
            } else if (mTwitterStatus.mIsRetweetedByMe) {
                mStatusIndicatorImageView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.status_indicator_rt));
                mStatusIndicatorImageView.setVisibility(View.VISIBLE);
            } else {
                mStatusIndicatorImageView.setVisibility(View.GONE);
            }
        }

        /*
         * mCreatedAtTimeTextView =
         * (TextView)findViewById(R.id.created_at_time); if
         * (mCreatedAtTimeTextView != null) { SimpleDateFormat simpleDataFormat
         * = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm a"); String
         * createdAtTime = simpleDataFormat.format(mTwitterStatus.mCreatedAt) +
         * " " + App.getContext().getString(R.string.via) + " " +
         * twitterStatus.mSource; mCreatedAtTimeTextView.setText(createdAtTime);
         * }
         */

        mAvatar = (QuickContactDivot) findViewById(R.id.avatar);
        if (mAvatar != null) {

            if (resize) {
                Integer dimensionId = null;
                switch (AppSettings.get().getCurrentProfileImageSize()) {
                    case Small:
                        dimensionId = R.dimen.avatar_width_height_small;
                        break;
                    case Medium:
                        dimensionId = R.dimen.avatar_width_height_medium;
                        break;
                    case Large:
                        dimensionId = R.dimen.avatar_width_height_large;
                        break;
                    default:
                        break;
                }
                if (dimensionId != null) {
                    int dimensionValue = mContext.getResources()
                            .getDimensionPixelSize(dimensionId);
                    mAvatar.setLayoutParams(new RelativeLayout.LayoutParams(
                            dimensionValue, dimensionValue));
                }
            }

            // dimen/avatar_width_height
            String profileImageUrl = twitterStatus.getProfileImageUrl(ProfileImageSize.BIGGER);
            if (profileImageUrl != null) {

                if (AppSettings.get().downloadFeedImages()) {

                    LazyImageLoader profileImageLoader = callbacks
                            .getProfileImageLoader();
                    if (profileImageLoader != null) {

                        profileImageLoader.displayImage(profileImageUrl, mAvatar);
                    }
                }

                mAvatar.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        onProfileImageClick();
                    }
                });
            }

        }

        mMessageBlock = findViewById(R.id.message_block);

        //
        // Configure the onTouchListeners so that selection/deselection of the
        // TweetFeedItemView instance works
        //
        setOnTouchListener(mOnTouchListener);
        if (mStatusTextView != null) {
            mStatusTextView.setOnTouchListener(mStatusOnTouchListener);
        }

        if (mAuthorScreenNameTextView != null) {
            mAuthorScreenNameTextView.setOnTouchListener(mOnTouchListener);
        }
        if (mAuthorNameTextView != null) {
            mAuthorNameTextView.setOnTouchListener(mOnTouchListener);
        }

        setPreviewImage(twitterStatus.mMediaEntity, twitterStatus.mAdnMedia, callbacks);
    }

    /*
	 *
	 */
    public void insertConversationView() {
        if (mConversationView == null) {
            mConversationView = (ConversationView) mCallbacks
                    .getLayoutInflater().inflate(R.layout.conversation_feed,
                            null);
            addView(mConversationView);
        }
    }

    /*
     *
     */
    public void setPreviewImage(TwitterMediaEntity mediaEntity, AdnMedia adnMedia, Callbacks callbacks) {

        mPreviewImageContainer = (RelativeLayout) findViewById(R.id.preview_image_container);

        if ((mediaEntity == null && adnMedia == null) || AppSettings.get().downloadFeedImages() == false) {
            if (mPreviewImageContainer != null) {
                mPreviewImageContainer.setVisibility(View.GONE);
            }
            return;
        }

        String mediaUrl = adnMedia != null ? adnMedia.mUrl : mediaEntity.getMediaUrl(Size.LARGE);
        String thumbUrl = adnMedia != null ? adnMedia.mThumbnailUrl : mediaEntity.getMediaUrl(Size.THUMB);
        TwitterMediaEntity.Source source = adnMedia != null ? null : mediaEntity.getSource();

        if (mPreviewImageContainer != null) {
            final boolean isVideo = source == TwitterMediaEntity.Source.YOUTUBE;

            mPreviewImageContainer.setVisibility(View.VISIBLE);
            mPreviewImageView = (ImageView) findViewById(R.id.preview_image_view);
            if (mPreviewImageView == null) {
                mPreviewImageView = (ImageView) findViewById(R.id.preview_large_image_view);
                UrlImageViewHelper.setUrlDrawable(mPreviewImageView, mediaUrl, new UrlImageViewCallback() {

                    @Override
                    public void onLoaded(ImageView imageView, Drawable loadedDrawable, String url,
                                         boolean loadedFromCache) {
                        //Chris's code did nothing special in this case, yo I've left it like that for now.
                        if (loadedDrawable == null) {
                        }

                    }
                });

            } else {
                LazyImageLoader previewImageLoader = callbacks.getPreviewImageLoader();
                if (previewImageLoader != null) {
                    previewImageLoader.displayImage(thumbUrl, mPreviewImageView);
                }
            }

            mPreviewImageView.setVisibility(VISIBLE);
            mPreviewImageView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    String url = mTwitterStatus.mAdnMedia != null ? mTwitterStatus.mAdnMedia.mUrl :
                            mTwitterStatus.mMediaEntity.getMediaUrl(Size.LARGE);
                    String expandedUrl = mTwitterStatus.mAdnMedia != null ? mTwitterStatus.mAdnMedia.mExpandedUrl :
                            mTwitterStatus.mMediaEntity.getExpandedUrl();

                    if (mTwitterStatus != null) {
                        if (isVideo) {
                            Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(expandedUrl));
                            mCallbacks.getActivity().startActivity(viewIntent);
                        } else {
                            ImageViewActivity.createAndStartActivity(mCallbacks.getActivity(), url, expandedUrl,
                                    mTwitterStatus.getAuthorScreenName());
                        }
                    }
                }
            });

            mPreviewPlayImageView = (ImageView) findViewById(R.id.preview_image_play_view);
            if (mPreviewPlayImageView != null) {
                mPreviewPlayImageView.setVisibility(isVideo ? View.VISIBLE
                        : View.GONE);
            }

            // Bit of hack, but reduce the status right padding element when
            // displaying an image
            mStatusTextView.setPadding(mStatusTextView.getPaddingLeft(), mStatusTextView.getPaddingTop(),
                    (int) Util.convertDpToPixel(6, mContext), mStatusTextView.getPaddingRight());
        }
    }

    /*
	 *
	 */
    OnTouchListener mStatusOnTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent event) {

            // Code from here: http://stackoverflow.com/a/7327332/328679
            TextView textView = (TextView) view;
            Object text = textView.getText();
            if (text instanceof Spanned) {
                int action = event.getAction();

                if (action == MotionEvent.ACTION_UP
                        || action == MotionEvent.ACTION_DOWN) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    x -= textView.getTotalPaddingLeft();
                    y -= textView.getTotalPaddingTop();

                    x += textView.getScrollX();
                    y += textView.getScrollY();

                    Layout layout = textView.getLayout();
                    int line = layout.getLineForVertical(y);
                    int off = layout.getOffsetForHorizontal(line, x);

                    Spanned buffer = (Spanned) text;
                    ClickableSpan[] link = buffer.getSpans(off, off,
                            ClickableSpan.class);

                    // If this is a link, don't pass the touch back to the
                    // system. TwitterLinkify will handle these links,
                    // and by passing false back we ensure the TweetFeedItemView
                    // instance is not selected int the parent ListView

                    // bug fix (devisnik) for: no switch to action mode when
                    // longpressing on a link
                    // only handle link if touch is not a long press
                    if (action == MotionEvent.ACTION_UP && event.getEventTime() - event.getDownTime() < ViewConfiguration
                            .getLongPressTimeout() && link.length != 0) {
                        if (mCallbacks != null) {
                            mCallbacks.onUrlClicked(mTwitterStatus);
                        }
                        MotionEvent cancelEvent = MotionEvent.obtain(event);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                        mGestureDetector.onTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                        return false;
                    }
                }

            }

            return mGestureDetector.onTouchEvent(event);
        }
    };

    /*
	 *
	 */
    OnTouchListener mOnTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
        }
    };

    /*
	 *
	 */
    GestureDetector mGestureDetector = new GestureDetector(
            new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (mCallbacks != null) {
                        return mCallbacks.onSingleTapConfirmed(
                                TweetFeedItemView.this, mPosition);
                    }
                    return false;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    if (mCallbacks != null) {
                        mCallbacks.onLongPress(TweetFeedItemView.this,
                                mPosition);
                    }
                    // return true;
                }

                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }
            });

    public TwitterStatus getTwitterStatus() {
        return mTwitterStatus;
    }

    /*
	 *
	 */
    public void onProfileImageClick() {
        Intent profileIntent = new Intent(mContext, ProfileActivity.class);
        profileIntent.putExtra("userId", Long.valueOf(mTwitterStatus.mAuthorId)
                .toString());
        profileIntent.putExtra("userScreenName",
                mTwitterStatus.getAuthorScreenName());

        // profileIntent.putExtra("userScreenName", "JossWhedonGeek");
        // profileIntent.putExtra("userScreenName", "Donnicous");
        // profileIntent.putExtra("userScreenName", "TweetLanes");
        // profileIntent.putExtra("userScreenName", "JustinBieber");
        // profileIntent.putExtra("userScreenName", "SteveStreza");
        mContext.startActivity(profileIntent);
    }

    /*
	 *
	 */
    public void configureConversationView(SocialNetConstant.Type socialNetType, String currentAccountKey) {

        insertConversationView();

        if (mConversationExpanded) {
            mConversationView.setVisibility(VISIBLE);
            int drawable = AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark ? R.drawable.ic_action_collapse_dark
                    : R.drawable.ic_action_collapse_light;
            mConversationToggle.setImageDrawable(getResources().getDrawable(
                    drawable));
            mConversationView.configure(mTwitterStatus,
                    mCallbacks.getLayoutInflater(),
                    new ConversationView.Callbacks() {

                        @Override
                        public Activity getActivity() {
                            return mCallbacks.getActivity();
                        }

                        @Override
                        public LazyImageLoader getProfileImageLoader() {
                            return mCallbacks.getProfileImageLoader();
                        }

                        @Override
                        public LazyImageLoader getPreviewImageLoader() {
                            return mCallbacks.getPreviewImageLoader();
                        }
                    }, socialNetType, currentAccountKey);
        } else {
            mConversationView.setVisibility(GONE);
            int drawable = AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark ? R.drawable.ic_action_expand_dark
                    : R.drawable.ic_action_expand_light;
            mConversationToggle.setImageDrawable(getResources().getDrawable(
                    drawable));
        }

        mCallbacks.onConversationViewToggle(mTwitterStatus.mId,
                mConversationExpanded);
    }

    /*
	 *
	 */
    public void onLoadTweetSpotlight() {
        if (mLoadsTweetSpotlight == true) {

            Intent tweetSpotlightIntent = new Intent(mContext,
                    TweetSpotlightActivity.class);
            tweetSpotlightIntent.putExtra("statusId",
                    Long.toString(mTwitterStatus.mId));
            mContext.startActivity(tweetSpotlightIntent);

        }
    }

    /**
     * Override dispatchDraw so that we can put our own background and border
     * in. This is all complexity to support a shared border from one item to
     * the next.
     */

    @Override
    public void dispatchDraw(Canvas c) {
        View v = mMessageBlock;
        if (v != null) {
            float l = v.getX() + getPaddingLeft();
            float t = v.getY();
            float b = v.getY() + v.getHeight();

            super.dispatchDraw(c);

            Path path = mPath;

            // if (mAvatar.getPosition() == Divot.LEFT_UPPER ||
            // mAvatar.getPosition() == Divot.RIGHT_UPPER)

            Paint paint = mPaint;
            // paint.setColor(0xff00ff00);
            paint.setColor(AppSettings.get().getCurrentBorderColor());
            paint.setStrokeWidth(1F);
            paint.setStyle(Paint.Style.STROKE);

            path.reset();
            path.moveTo(l, b);
            path.lineTo(l, t + mAvatar.getFarOffset());
            c.drawPath(path, paint);

            path.reset();
            path.moveTo(l, t);
            path.lineTo(l, t + mAvatar.getCloseOffset());
            c.drawPath(path, paint);

            if (mIsConversationItem) {
                path.reset();
                path.moveTo(l, t);
                path.lineTo(l + v.getWidth(), t);
                c.drawPath(path, paint);
            }

        } else {
            super.dispatchDraw(c);
        }
    }

}

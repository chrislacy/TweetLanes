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
import android.net.Uri;
import android.text.Layout;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.AppSettings.StatusSize;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.util.Util;

import org.appdotnet4j.model.AdnMedia;
import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterManager.ProfileImageSize;
import org.tweetalib.android.model.TwitterMediaEntity;
import org.tweetalib.android.model.TwitterMediaEntity.Size;
import org.tweetalib.android.model.TwitterStatus;

public class TweetFeedItemView extends LinearLayout {

    private Context mContext;
    private int mPosition;
    private TwitterStatus mTwitterStatus;
    private Callbacks mCallbacks;
    private TextView mStatusTextView;
    private ImageView mConversationToggle;
    private ConversationView mConversationView;
    private View mMessageBlock;
    private QuickContactDivot mAvatar;
    private boolean mIsConversationItem;
    private ViewHolder mHolder;
    private final Path mPath = new Path();
    private final Paint mPaint = new Paint();

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

    void init(Context context) {
        mContext = context;
        final int theme = AppSettings.get().getCurrentThemeStyle();
        int background = android.R.color.white;
        if (theme == R.style.Theme_TweetLanes) {
            background = android.R.color.black;
        }
        setBackgroundColor(getResources().getColor(background));
    }

    /*
     *
	 */
    public void configure(TwitterStatus twitterStatus, int position, Callbacks callbacks, boolean tweetFeedItem,
                          boolean showRetweetCount, boolean showConversationView, boolean isConversationItem, boolean resize,
                          final SocialNetConstant.Type socialNetType, final String currentAccountKey) {

        mHolder = (ViewHolder) getTag();
        if (mHolder == null) {
            mHolder = new ViewHolder(this);
            setTag(mHolder);
        }
        StatusSize statusSize = AppSettings.get().getCurrentStatusSize();

        mTwitterStatus = twitterStatus;
        mIsConversationItem = isConversationItem;
        mPosition = position;
        mCallbacks = callbacks;

        AppSettings.DisplayNameFormat nameFormat = AppSettings.get().getCurrentDisplayNameFormat();
        TextView authorScreenNameTextView = mHolder.authorScreenNameTextView;
        if (authorScreenNameTextView != null) {
            if (nameFormat == AppSettings.DisplayNameFormat.Both) {
                authorScreenNameTextView.setText("@" + twitterStatus.getAuthorScreenName());

                if (resize) {
                    Integer textSize = null;
                    if (statusSize == StatusSize.Small) {
                        textSize = 14;
                    }

                    if (textSize != null) {
                        authorScreenNameTextView.setTextSize(
                                TypedValue.COMPLEX_UNIT_SP, textSize);
                    }
                }
            } else {
                authorScreenNameTextView.setVisibility(GONE);
            }
        }
        TextView authorNameTextView = mHolder.authorNameTextView;
        if (authorNameTextView != null) {
            if (nameFormat == AppSettings.DisplayNameFormat.Handle) {
                authorNameTextView.setText("@" + twitterStatus.getAuthorScreenName());
            } else {
                authorNameTextView.setText(twitterStatus.getAuthorName());
            }
        }

        TextView tweetDetailsView = mHolder.tweetDetailsView;
        if (tweetDetailsView != null) {

            boolean showTweetSource = AppSettings.get().showTweetSource();

            String verb = socialNetType == SocialNetConstant.Type.Twitter ? "Retweeted" : "Reposted";

            if (twitterStatus.mIsRetweet) {

                String text = verb + " by " + twitterStatus.mUserName;
                if (twitterStatus.mRetweetCount > 1) {
                    long otherRetweets = (twitterStatus.mRetweetCount - 1);
                    text += " and " + otherRetweets;
                    if (otherRetweets > 1) {
                        text += " others.";
                    } else {
                        text += " other.";
                    }
                }
                if (showTweetSource) {
                    text += " " + mContext.getString(R.string.via) + " " + mTwitterStatus.mSource;
                }
                tweetDetailsView.setText(text);
                tweetDetailsView.setVisibility(VISIBLE);
            } else if (showRetweetCount && twitterStatus.mRetweetCount > 0) {
                tweetDetailsView.setText(verb + " " + twitterStatus.mRetweetCount + " times.");
                tweetDetailsView.setVisibility(VISIBLE);
            } else {
                if (showTweetSource) {
                    tweetDetailsView.setText(mContext.getString(R.string.via) + " " + mTwitterStatus.mSource);
                } else {
                    tweetDetailsView.setVisibility(GONE);
                }
            }
        }

        mConversationExpanded = showConversationView;
        mConversationToggle = mHolder.conversationToggle;

        if (mConversationToggle != null) {
            mConversationToggle.setVisibility(GONE);
            if (mConversationView != null) {
                removeView(mConversationView);
                mConversationView = null;
            }
            int drawable = AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark ? R.drawable.ic_action_expand_dark
                    : R.drawable.ic_action_expand_light;
            mConversationToggle.setImageDrawable(getResources().getDrawable(
                    drawable));

            if (twitterStatus.mInReplyToStatusId != null) {
                mConversationToggle.setVisibility(VISIBLE);
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
            }
        }

        mStatusTextView = mHolder.statusTextView;

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
            case ExtraExtraLarge:
                textSize = R.dimen.font_size_extra_extra_large;
                break;
            case Supersize:
                textSize = R.dimen.font_size_supersize;
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

        TextView prettyDateTextView = mHolder.prettyDateTextView;
        if (prettyDateTextView != null) {
            prettyDateTextView.setText(Util.getDisplayDate(mTwitterStatus.mCreatedAt));
        }

        TextView fullDateTextView = mHolder.fullDateTextView;
        if (fullDateTextView != null) {
            fullDateTextView.setText(Util
                    .getFullDate(mTwitterStatus.mCreatedAt));
        }

        ImageView statusIndicatorImageView = mHolder.statusIndicatorImageView;
        if (statusIndicatorImageView != null) {
            if (mTwitterStatus.mIsFavorited && mTwitterStatus.mIsRetweetedByMe) {
                statusIndicatorImageView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.status_indicator_rt_fav));
                statusIndicatorImageView.setVisibility(View.VISIBLE);
            } else if (mTwitterStatus.mIsFavorited) {
                statusIndicatorImageView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.status_indicator_fav));
                statusIndicatorImageView.setVisibility(View.VISIBLE);
            } else if (mTwitterStatus.mIsRetweetedByMe) {
                statusIndicatorImageView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.status_indicator_rt));
                statusIndicatorImageView.setVisibility(View.VISIBLE);
            } else {
                statusIndicatorImageView.setVisibility(View.GONE);
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

        mAvatar = mHolder.avatar;
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
                } else {
                    mAvatar.setImageResource(R.drawable.ic_contact_picture);
                }

                mAvatar.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        onProfileImageClick();
                    }
                });
            }

        }

        mMessageBlock = mHolder.messageBlock;

        //
        // Configure the onTouchListeners so that selection/deselection of the
        // TweetFeedItemView instance works
        //
        setOnTouchListener(mOnTouchListener);
        if (mStatusTextView != null) {
            mStatusTextView.setOnTouchListener(mStatusOnTouchListener);
        }

        if (authorScreenNameTextView != null) {
            authorScreenNameTextView.setOnTouchListener(mOnTouchListener);
        }
        if (authorNameTextView != null) {
            authorNameTextView.setOnTouchListener(mOnTouchListener);
        }

        setPreviewImage(twitterStatus.mMediaEntity, twitterStatus.mAdnMedia, callbacks, tweetFeedItem);
    }

    /*
     *
	 */
    void insertConversationView() {
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
    void setPreviewImage(TwitterMediaEntity mediaEntity, AdnMedia adnMedia, Callbacks callbacks, boolean tweetFeedItem) {
        if (mHolder != null) {
            RelativeLayout previewImageContainer;
            ImageView previewImageView;
            ImageView previewPlayImageView;

            if(mHolder.previewImageContainer != null) mHolder.previewImageContainer.setVisibility(GONE);
            if(mHolder.previewImageContainerLarge != null) mHolder.previewImageContainerLarge.setVisibility(GONE);
            if(mHolder.previewImageContainerSpotlight != null) mHolder.previewImageContainerSpotlight.setVisibility(GONE);

            AppSettings.MediaImageSize mediaImageSize = AppSettings.get().getCurrentMediaImageSize();

            if ((mediaEntity == null && adnMedia == null) || !AppSettings.get().downloadFeedImages()) {
                return;
            } else if (mediaImageSize == AppSettings.MediaImageSize.Off && tweetFeedItem) {
                return;
            }

            boolean useLarge = true;
            if (tweetFeedItem) {
                useLarge = mediaImageSize == AppSettings.MediaImageSize.Large;
                if (useLarge) {
                    previewImageContainer = mHolder.previewImageContainerLarge;
                    previewImageView = mHolder.previewImageViewLarge;
                    previewPlayImageView = mHolder.previewPlayImageViewLarge;
                } else {
                    previewImageContainer = mHolder.previewImageContainer;
                    previewImageView = mHolder.previewImageView;
                    previewPlayImageView = mHolder.previewPlayImageView;
                }
            } else {
                previewImageContainer = mHolder.previewImageContainerSpotlight;
                previewImageView = mHolder.previewImageViewSpotlight;
                previewPlayImageView = mHolder.previewPlayImageViewSpotlight;
            }

            String thumbUrl;
            if (useLarge) {
                thumbUrl = adnMedia != null ? adnMedia.mUrl : mediaEntity.getMediaUrl(Size.MEDIUM);
            } else {
                thumbUrl = adnMedia != null ? adnMedia.mThumbnailUrl : mediaEntity.getMediaUrl(Size.THUMB);
            }
            TwitterMediaEntity.Source source = adnMedia != null ? null : mediaEntity.getSource();

            if (previewImageContainer != null) {
                final boolean isVideo = source == TwitterMediaEntity.Source.YOUTUBE;

                previewImageContainer.setVisibility(View.VISIBLE);
                LazyImageLoader previewImageLoader = callbacks.getPreviewImageLoader();
                if (previewImageLoader != null) {
                    previewImageLoader.displayImage(thumbUrl, previewImageView);
                }

                previewImageView.setVisibility(VISIBLE);
                previewImageView.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        String url = mTwitterStatus.mAdnMedia != null ? mTwitterStatus.mAdnMedia.mUrl :
                                mTwitterStatus.mMediaEntity.getMediaUrl(Size.LARGE);
                        String expandedUrl = mTwitterStatus.mAdnMedia != null ? mTwitterStatus.mAdnMedia.mExpandedUrl :
                                mTwitterStatus.mMediaEntity.getExpandedUrl();

                        if (mTwitterStatus != null) {
                            if (isVideo) {
                                Intent viewIntent = null;
                                if (expandedUrl != null) {
                                    viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(expandedUrl));
                                } else if (url != null) {
                                    viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                }
                                if (viewIntent != null) {
                                    mCallbacks.getActivity().startActivity(viewIntent);
                                }
                            } else {
                                ImageViewActivity.createAndStartActivity(mCallbacks.getActivity(), url, expandedUrl,
                                        mTwitterStatus.getAuthorScreenName());
                            }
                        }
                    }
                });

                if (previewPlayImageView != null) {
                    previewPlayImageView.setVisibility(isVideo ? View.VISIBLE
                            : View.GONE);
                }

                // Bit of hack, but reduce the status right padding element when
                // displaying an image
                mStatusTextView.setPadding(mStatusTextView.getPaddingLeft(), mStatusTextView.getPaddingTop(),
                        (int) Util.convertDpToPixel(6, mContext), mStatusTextView.getPaddingRight());
            }
        }
    }

    /*
     *
	 */
    private final OnTouchListener mStatusOnTouchListener = new OnTouchListener() {

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
    private final OnTouchListener mOnTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
        }
    };

    /*
     *
	 */
    private final GestureDetector mGestureDetector = new GestureDetector(
            new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    return mCallbacks != null && mCallbacks.onSingleTapConfirmed(TweetFeedItemView.this, mPosition);
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
    void onProfileImageClick() {
        Intent profileIntent = new Intent(mContext, ProfileActivity.class);
        profileIntent.putExtra("userId", Long.valueOf(mTwitterStatus.mAuthorId)
                .toString());
        profileIntent.putExtra("userScreenName",
                mTwitterStatus.getAuthorScreenName());

        profileIntent.putExtra("clearCompose", "true");

        ((Activity) mContext).startActivityForResult(profileIntent, Constant.REQUEST_CODE_PROFILE);
    }

    /*
	 *
	 */
    void configureConversationView(SocialNetConstant.Type socialNetType, String currentAccountKey) {

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

    private static class ViewHolder {
        public QuickContactDivot avatar;
        public TextView authorScreenNameTextView;
        public TextView authorNameTextView;
        public TextView tweetDetailsView;
        public ImageView conversationToggle;
        public TextView statusTextView;
        public TextView prettyDateTextView;
        public TextView fullDateTextView;
        public ImageView statusIndicatorImageView;
        public View messageBlock;
        public RelativeLayout previewImageContainer;
        public ImageView previewImageView;
        public ImageView previewPlayImageView;
        public RelativeLayout previewImageContainerLarge;
        public ImageView previewImageViewLarge;
        public ImageView previewPlayImageViewLarge;
        public RelativeLayout previewImageContainerSpotlight;
        public ImageView previewImageViewSpotlight;
        public ImageView previewPlayImageViewSpotlight;

        public ViewHolder(View v) {
            avatar = (QuickContactDivot) v.findViewById(R.id.avatar);
            authorScreenNameTextView = (TextView) v.findViewById(R.id.authorScreenName);
            authorNameTextView = (TextView) v.findViewById(R.id.authorName);
            tweetDetailsView = (TextView) v.findViewById(R.id.tweet_details);
            conversationToggle = (ImageView) v.findViewById(R.id.conversationToggle);
            statusTextView = (TextView) v.findViewById(R.id.status);
            prettyDateTextView = (TextView) v.findViewById(R.id.pretty_date);
            fullDateTextView = (TextView) v.findViewById(R.id.full_date);
            statusIndicatorImageView = (ImageView) v.findViewById(R.id.status_indicator);
            messageBlock = v.findViewById(R.id.message_block);
            previewImageContainer = (RelativeLayout) v.findViewById(R.id.preview_image_container);
            if (previewImageContainer != null) {
                previewImageView = (ImageView) previewImageContainer.findViewById(R.id.preview_image_view);
                previewPlayImageView = (ImageView) previewImageContainer.findViewById(R.id.preview_image_play_view);
            }
            previewImageContainerLarge = (RelativeLayout) v.findViewById(R.id.preview_image_container_large);
            if (previewImageContainerLarge != null) {
                previewImageViewLarge = (ImageView) previewImageContainerLarge.findViewById(R.id.preview_image_view_large);
                previewPlayImageViewLarge = (ImageView) previewImageContainerLarge.findViewById(R.id.preview_image_play_view_large);
            }
            previewImageContainerSpotlight = (RelativeLayout) v.findViewById(R.id.preview_spotlight_image_container);
            if (previewImageContainerSpotlight != null) {
                previewImageViewSpotlight = (ImageView) previewImageContainerSpotlight.findViewById(R.id.preview_spotlight_image_view);
                previewPlayImageViewSpotlight = (ImageView) previewImageContainerSpotlight.findViewById(R.id.preview_spotlight_image_play_view);
            }
        }
    }

}

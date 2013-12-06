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
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.AppSettings.StatusSize;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.model.AccountDescriptor;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.util.Util;

import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterDirectMessage;
import org.tweetalib.android.model.TwitterDirectMessage.MessageType;
import org.tweetalib.android.widget.URLSpanNoUnderline;

public class DirectMessageItemView extends LinearLayout {

    private final Path mPath = new Path();
    private final Paint mPaint = new Paint();
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
                    return mCallbacks != null && mCallbacks.onSingleTapConfirmed(DirectMessageItemView.this, mPosition);
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    if (mCallbacks != null) {
                        mCallbacks.onLongPress(DirectMessageItemView.this,
                                mPosition);
                    }
                    // return true;
                }

                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }
            });
    private Context mContext;
    private int mPosition;
    private TwitterDirectMessage mDirectMessage;
    private DirectMessageItemViewCallbacks mCallbacks;
    private View mMessageBlock;
    private QuickContactDivot mAvatar;
    private boolean mFullConversation;
    private ViewHolder mHolder;

    /*
     *
     */
    public DirectMessageItemView(Context context) {
        super(context);
        init(context);
    }

    public DirectMessageItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DirectMessageItemView(Context context, AttributeSet attrs,
                                 int defStyle) {
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
    public void configure(String userScreenName, String userName,
                          TwitterDirectMessage directMessage, int position,
                          MessageType messageType, boolean fullConversation,
                          DirectMessageItemViewCallbacks callbacks) {

        mHolder = (ViewHolder) getTag();
        if (mHolder == null) {
            mHolder = new ViewHolder(this);
            setTag(mHolder);
        }
        StatusSize statusSize = AppSettings.get().getCurrentStatusSize();

        mDirectMessage = directMessage;
        mFullConversation = fullConversation;

        mPosition = position;
        mCallbacks = callbacks;

        ImageView replyIcon = mHolder.replyIcon;
        if (replyIcon != null) {
            if (fullConversation) {
                replyIcon.setVisibility(GONE);
            } else {
                if (directMessage.getMessageType() == MessageType.SENT) {
                    replyIcon.setVisibility(VISIBLE);
                } else {
                    replyIcon.setVisibility(GONE);
                }
            }
        }

        AppSettings.DisplayNameFormat nameFormat = AppSettings.get().getCurrentDisplayNameFormat();
        TextView authorScreenNameTextView = mHolder.authorScreenNameTextView;
        if (authorScreenNameTextView != null) {
            if (nameFormat == AppSettings.DisplayNameFormat.Both) {
                authorScreenNameTextView.setText("@"
                        + (messageType == MessageType.SENT ? userScreenName
                        : directMessage.getOtherUserScreenName()));

                Integer textSize = null;
                if (statusSize == StatusSize.Small) {
                    textSize = 14;
                } else if (statusSize == StatusSize.Large) {
                    textSize = 18;
                }
                if (textSize != null) {
                    authorScreenNameTextView.setTextSize(
                            TypedValue.COMPLEX_UNIT_SP, textSize);
                }
            } else {
                authorScreenNameTextView.setVisibility(GONE);
            }
        }

        TextView authorNameTextView = mHolder.authorNameTextView;
        if (authorNameTextView != null) {
            if (nameFormat == AppSettings.DisplayNameFormat.Handle) {
                authorNameTextView.setText("@"
                        + (messageType == MessageType.SENT ? userScreenName
                        : directMessage.getOtherUserScreenName()));
            } else {
                authorNameTextView.setText(messageType == MessageType.SENT ? userName
                        : directMessage.getOtherUserName());
            }
        }

        TextView statusTextView = mHolder.statusTextView;
        String text = directMessage.getText();
        if (text != null) {
            if (mFullConversation) {
                statusTextView.setText(directMessage.mTextSpanned);
                statusTextView.setMovementMethod(LinkMovementMethod
                        .getInstance());
                URLSpanNoUnderline.stripUnderlines(statusTextView);
            } else {
                statusTextView.setText(directMessage.mTextSpanned);
            }

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

            if (textSize != null) {
                int dimensionValue = mContext.getResources()
                        .getDimensionPixelSize(textSize);
                statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        dimensionValue);
            }

        }

        TextView prettyDateTextView = mHolder.prettyDateTextView;
        if (prettyDateTextView != null) {
            prettyDateTextView.setText(Util.getDisplayDate(directMessage.getCreatedAt()));
        }

        mAvatar = mHolder.avatar;
        mAvatar.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onProfileImageClick();
            }
        });

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
            int dimensionValue = mContext.getResources().getDimensionPixelSize(
                    dimensionId);
            mAvatar.setLayoutParams(new RelativeLayout.LayoutParams(
                    dimensionValue, dimensionValue));
        }

        if (AppSettings.get().downloadFeedImages()) {

            String imageUrl = (messageType == MessageType.SENT ?
                    directMessage.getSenderProfileImageUrl(TwitterManager.ProfileImageSize.BIGGER) :
                    directMessage.getOtherUserProfileImageUrl(TwitterManager.ProfileImageSize.BIGGER));

            LazyImageLoader imageLoader = callbacks.getProfileImageLoader();
            if (imageLoader != null) {
                imageLoader.displayImage(imageUrl, mAvatar);
            }
        } else {
            mAvatar.setImageResource(R.drawable.ic_contact_picture);
        }

        mMessageBlock = mHolder.messageBlock;

        setOnTouchListener(mOnTouchListener);
        if (statusTextView != null) {
            statusTextView.setOnTouchListener(mOnTouchListener);
        }

        if (authorScreenNameTextView != null) {
            authorScreenNameTextView.setOnTouchListener(mOnTouchListener);
        }
    }

    public TwitterDirectMessage getDirectMessage() {
        return mDirectMessage;
    }

    /*
     *
	 */
    void onProfileImageClick() {
        Intent profileIntent = new Intent(mContext, ProfileActivity.class);

        if (mDirectMessage != null) {
            if (mFullConversation
                    && mDirectMessage.getMessageType() == MessageType.SENT) {
                AccountDescriptor account = ((App) mCallbacks.getActivity()
                        .getApplication()).getCurrentAccount();
                if (account != null) {
                    profileIntent.putExtra("userId",
                            Long.valueOf(account.getId()));
                    profileIntent.putExtra("userScreenName",
                            account.getScreenName());
                }
            } else {
                profileIntent.putExtra("userId",
                        Long.valueOf(mDirectMessage.getOtherUserId()));
                profileIntent.putExtra("userScreenName",
                        mDirectMessage.getOtherUserScreenName());
            }
        }

        profileIntent.putExtra("clearCompose", "true");
        ((Activity) mContext).startActivityForResult(profileIntent, Constant.REQUEST_CODE_PROFILE);
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

        } else {
            super.dispatchDraw(c);
        }
    }

    /*
     *
     */
    public interface DirectMessageItemViewCallbacks {

        public boolean onSingleTapConfirmed(View view, int position);

        public void onLongPress(View view, int position);

        public Activity getActivity();

        public LazyImageLoader getProfileImageLoader();
    }

    private static class ViewHolder {
        public ImageView replyIcon;
        public TextView authorScreenNameTextView;
        public TextView authorNameTextView;
        public TextView statusTextView;
        public TextView prettyDateTextView;
        public QuickContactDivot avatar;
        public View messageBlock;

        public ViewHolder(View v) {
            replyIcon = (ImageView) v.findViewById(R.id.replyIcon);
            authorScreenNameTextView = (TextView) v.findViewById(R.id.authorScreenName);
            authorNameTextView = (TextView) v.findViewById(R.id.authorName);
            statusTextView = (TextView) v.findViewById(R.id.status);
            prettyDateTextView = (TextView) v.findViewById(R.id.pretty_date);
            avatar = (QuickContactDivot) v.findViewById(R.id.avatar);
            messageBlock = v.findViewById(R.id.message_block);
        }
    }
}

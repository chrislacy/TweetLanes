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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.util.LazyImageLoader;
import com.tweetlanes.android.core.view.UserFeedFragment.UserFeedItemViewCallbacks;

import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterUser;

public class UserFeedItemView extends LinearLayout {

    private final Path mPath = new Path();
    private final Paint mPaint = new Paint();
    private Context mContext;
    private TwitterUser mUser;
    private long mUserId;
    private String mUserScreenName;
    private View mMessageBlock;
    private QuickContactDivot mAvatar;

    public UserFeedItemView(Context context) {
        super(context);
        init(context);
    }

    public UserFeedItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public UserFeedItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    void init(Context context) {
        mContext = context;
    }

    public void configure(TwitterUser user, int position,
                          UserFeedItemViewCallbacks callbacks) {

        mUser = user;

        mUserId = user.getId();
        mUserScreenName = user.getScreenName();
        TextView screenNameTextView = (TextView) findViewById(R.id.screen_name);
        screenNameTextView.setText("@" + user.getScreenName());

        TextView nameTextView = (TextView) findViewById(R.id.name);
        nameTextView.setText(user.getName());

        mAvatar = (QuickContactDivot) findViewById(R.id.avatar);
        if (AppSettings.get().downloadFeedImages()) {
            String imageUrl = user.getProfileImageUrl(TwitterManager.ProfileImageSize.BIGGER);

            LazyImageLoader profileImageLoader = callbacks
                    .getProfileImageLoader();
            if (profileImageLoader != null) {
                profileImageLoader.displayImage(imageUrl, mAvatar);
            }
        } else {
            mAvatar.setImageResource(R.drawable.ic_contact_picture);
        }

        mMessageBlock = findViewById(R.id.message_block);
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
            float l = v.getX();
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
}
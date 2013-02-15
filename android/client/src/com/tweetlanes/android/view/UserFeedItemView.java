/*
 * Copyright (C) 2013 Chris Lacy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tweetlanes.android.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tweetlanes.android.AppSettings;
import com.tweetlanes.android.R;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterUser;

import com.tweetlanes.android.util.LazyImageLoader;
import com.tweetlanes.android.view.UserFeedFragment.UserFeedItemViewCallbacks;

public class UserFeedItemView extends LinearLayout {

	private Context mContext;
	private TwitterUser mUser;
	private TextView mScreenNameTextView;
	private TextView mNameTextView;
	private long mUserId;
	private String mUserScreenName;
	private View mMessageBlock;
	private QuickContactDivot mAvatar;
	private Path mPath = new Path();
    private Paint mPaint = new Paint();
	
	public UserFeedItemView(Context context) {
		super(context);
		init(context);
	}
	public UserFeedItemView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}
	public UserFeedItemView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public void init(Context context) {
		mContext = context;
	}
	
	public void configure(TwitterUser user, int position, UserFeedItemViewCallbacks callbacks) {
		
		mUser = user;
		
		mUserId = user.getId();
		mUserScreenName = user.getScreenName();
		mScreenNameTextView = (TextView)findViewById(R.id.screen_name);
		mScreenNameTextView.setText("@" + user.getScreenName());
		
		mNameTextView = (TextView)findViewById(R.id.name);
		mNameTextView.setText(user.getName());
		
		mAvatar = (QuickContactDivot) findViewById(R.id.avatar);
		if (AppSettings.get().downloadFeedImages()) {
			String imageUrl = TwitterManager.getProfileImageUrl(user.getScreenName(), TwitterManager.ProfileImageSize.BIGGER);
			//UrlImageViewHelper.setUrlDrawable(avatar, imageUrl, R.drawable.ic_contact_picture);
			//avatar.setImageURL(imageUrl);
			LazyImageLoader profileImageLoader = callbacks.getProfileImageLoader(); 
			if (profileImageLoader != null) {
				profileImageLoader.displayImage(imageUrl, mAvatar);
			}
		}
			
		mMessageBlock = findViewById(R.id.message_block);
	}
	
	public void onProfileImageClick() {
		Intent profileIntent = new Intent(mContext, ProfileActivity.class);
		profileIntent.putExtra("userId", Long.valueOf(mUserId).toString());
		profileIntent.putExtra("userScreenName", mUserScreenName);
		mContext.startActivity(profileIntent);
	}
	
	public TwitterUser getTwitterUser() { return mUser; }
	
	
	/**
     * Override dispatchDraw so that we can put our own background and border in.
     * This is all complexity to support a shared border from one item to the next.
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

            //if (mAvatar.getPosition() == Divot.LEFT_UPPER || mAvatar.getPosition() == Divot.RIGHT_UPPER)

            Paint paint = mPaint;
            //paint.setColor(0xff00ff00);
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
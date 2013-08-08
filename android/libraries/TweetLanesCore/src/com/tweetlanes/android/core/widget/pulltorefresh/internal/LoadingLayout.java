package com.tweetlanes.android.core.widget.pulltorefresh.internal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.widget.pulltorefresh.PullToRefreshBase;

public class LoadingLayout extends FrameLayout {

    private static final int DEFAULT_ROTATION_ANIMATION_DURATION = 150;

    private final ImageView mHeaderImage;
    private final ProgressBar mHeaderProgress;
    private final TextView mHeaderText;

    private String mPullLabel;
    private String mRefreshingLabel;
    private String mReleaseLabel;

    private final Animation mRotateAnimation, mResetRotateAnimation;

    public LoadingLayout(Context context, final int mode, String releaseLabel,
                         String pullLabel, String refreshingLabel) {
        super(context);
        ViewGroup header = (ViewGroup) LayoutInflater.from(context).inflate(
                R.layout.load_more, this);
        mHeaderText = (TextView) header.findViewById(R.id.load_more_text);
        mHeaderImage = (ImageView) header.findViewById(R.id.load_more_image);
        mHeaderProgress = (ProgressBar) header
                .findViewById(R.id.load_more_progress);

        final Interpolator interpolator = new LinearInterpolator();
        mRotateAnimation = new RotateAnimation(0, -180,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mRotateAnimation.setInterpolator(interpolator);
        mRotateAnimation.setDuration(DEFAULT_ROTATION_ANIMATION_DURATION);
        mRotateAnimation.setFillAfter(true);

        mResetRotateAnimation = new RotateAnimation(-180, 0,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mResetRotateAnimation.setInterpolator(interpolator);
        mResetRotateAnimation.setDuration(DEFAULT_ROTATION_ANIMATION_DURATION);
        mResetRotateAnimation.setFillAfter(true);

        this.mReleaseLabel = releaseLabel;
        this.mPullLabel = pullLabel;
        this.mRefreshingLabel = refreshingLabel;

        switch (mode) {
            case PullToRefreshBase.MODE_PULL_UP_TO_REFRESH:
                mHeaderImage.setImageResource(R.drawable.pulltorefresh_up_arrow);
                break;
            case PullToRefreshBase.MODE_PULL_DOWN_TO_REFRESH:
            default:
                mHeaderImage.setImageResource(R.drawable.pulltorefresh_down_arrow);
                break;
        }
    }

    public void reset() {
        mHeaderText.setText(mPullLabel);
        mHeaderImage.setVisibility(View.VISIBLE);
        mHeaderProgress.setVisibility(View.GONE);
    }

    public void releaseToRefresh() {
        mHeaderText.setText(mReleaseLabel);
        mHeaderImage.clearAnimation();
        mHeaderImage.startAnimation(mRotateAnimation);
    }

    public void setPullLabel(String pullLabel) {
        this.mPullLabel = pullLabel;
    }

    public void refreshing() {
        mHeaderText.setText(mRefreshingLabel);
        mHeaderImage.clearAnimation();
        mHeaderImage.setVisibility(View.INVISIBLE);
        mHeaderProgress.setVisibility(View.VISIBLE);
    }

    public void setRefreshingLabel(String refreshingLabel) {
        this.mRefreshingLabel = refreshingLabel;
    }

    public void setReleaseLabel(String releaseLabel) {
        this.mReleaseLabel = releaseLabel;
    }

    public void pullToRefresh() {
        mHeaderText.setText(mPullLabel);
        mHeaderImage.clearAnimation();
        mHeaderImage.startAnimation(mResetRotateAnimation);
    }

    public void setTextColor(int color) {
        mHeaderText.setTextColor(color);
    }

}

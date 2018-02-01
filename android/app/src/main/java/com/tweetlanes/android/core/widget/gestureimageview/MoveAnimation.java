/*
 * Copyright (c) 2012 Jason Polites Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.tweetlanes.android.core.widget.gestureimageview;

/**
 * @author Jason Polites
 */
public class MoveAnimation implements Animation {

    private boolean mFirstFrame = true;

    private float mStartX;
    private float mStartY;

    private float mTargetX;
    private float mTargetY;
    private long mAnimationTimeMS = 100;
    private long mTotalTime = 0;

    private MoveAnimationListener mMoveAnimationListener;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.polites.android.Animation#update(com.polites.android.GestureImageView
     * , long)
     */
    @Override
    public boolean update(GestureImageView view, long time) {
        mTotalTime += time;

        if (mFirstFrame) {
            mFirstFrame = false;
            mStartX = view.getImageX();
            mStartY = view.getImageY();
        }

        if (mTotalTime < mAnimationTimeMS) {

            float ratio = (float) mTotalTime / mAnimationTimeMS;

            float newX = ((mTargetX - mStartX) * ratio) + mStartX;
            float newY = ((mTargetY - mStartY) * ratio) + mStartY;

            if (mMoveAnimationListener != null) {
                mMoveAnimationListener.onMove(newX, newY);
            }

            return true;
        } else {
            if (mMoveAnimationListener != null) {
                mMoveAnimationListener.onMove(mTargetX, mTargetY);
            }
        }

        return false;
    }

    public void reset() {
        mFirstFrame = true;
        mTotalTime = 0;
    }

    public float getTargetX() {
        return mTargetX;
    }

    public void setTargetX(float targetX) {
        this.mTargetX = targetX;
    }

    public float getTargetY() {
        return mTargetY;
    }

    public void setTargetY(float targetY) {
        this.mTargetY = targetY;
    }

    public long getAnimationTimeMS() {
        return mAnimationTimeMS;
    }

    public void setAnimationTimeMS(long animationTimeMS) {
        this.mAnimationTimeMS = animationTimeMS;
    }

    public void setMoveAnimationListener(
            MoveAnimationListener moveAnimationListener) {
        this.mMoveAnimationListener = moveAnimationListener;
    }
}

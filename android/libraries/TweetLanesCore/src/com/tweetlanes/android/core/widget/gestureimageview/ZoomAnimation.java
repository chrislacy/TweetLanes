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

import android.graphics.PointF;

/**
 * @author Jason Polites
 */
public class ZoomAnimation implements Animation {

    private boolean mFirstFrame = true;

    private float mTouchX;
    private float mTouchY;

    private float mZoom;

    private float mStartX;
    private float mStartY;
    private float mStartScale;

    private float mXDiff;
    private float mYDiff;
    private float mScaleDiff;

    private long mAnimationLengthMS = 200;
    private long mTotalTime = 0;

    private ZoomAnimationListener zoomAnimationListener;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.polites.android.Animation#update(com.polites.android.GestureImageView
     * , long)
     */
    @Override
    public boolean update(GestureImageView view, long time) {
        if (mFirstFrame) {
            mFirstFrame = false;

            mStartX = view.getImageX();
            mStartY = view.getImageY();
            mStartScale = view.getScale();
            mScaleDiff = (mZoom * mStartScale) - mStartScale;

            if (mScaleDiff > 0) {
                // Calculate destination for midpoint
                VectorF vector = new VectorF();

                // Set the touch point as start because we want to move the end
                vector.setStart(new PointF(mTouchX, mTouchY));
                vector.setEnd(new PointF(mStartX, mStartY));

                vector.calculateAngle();

                // Get the current length
                float length = vector.calculateLength();

                // Multiply length by zoom to get the new length
                vector.length = length * mZoom;

                // Now deduce the new endpoint
                vector.calculateEndPoint();

                mXDiff = vector.end.x - mStartX;
                mYDiff = vector.end.y - mStartY;
            } else {
                // Zoom out to center
                mXDiff = view.getCenterX() - mStartX;
                mYDiff = view.getCenterY() - mStartY;
            }
        }

        mTotalTime += time;

        float ratio = (float) mTotalTime / (float) mAnimationLengthMS;

        if (ratio < 1) {

            if (ratio > 0) {
                // we still have time left
                float newScale = (ratio * mScaleDiff) + mStartScale;
                float newX = (ratio * mXDiff) + mStartX;
                float newY = (ratio * mYDiff) + mStartY;

                if (zoomAnimationListener != null) {
                    zoomAnimationListener.onZoom(newScale, newX, newY);
                }
            }

            return true;
        } else {

            float newScale = mScaleDiff + mStartScale;
            float newX = mXDiff + mStartX;
            float newY = mYDiff + mStartY;

            if (zoomAnimationListener != null) {
                zoomAnimationListener.onZoom(newScale, newX, newY);
                zoomAnimationListener.onComplete();
            }

            return false;
        }
    }

    public void reset() {
        mFirstFrame = true;
        mTotalTime = 0;
    }

    public void setZoom(float zoom) {
        this.mZoom = zoom;
    }

    public void setTouchX(float touchX) {
        this.mTouchX = touchX;
    }

    public void setTouchY(float touchY) {
        this.mTouchY = touchY;
    }

    public void setZoomAnimationListener(
            ZoomAnimationListener zoomAnimationListener) {
        this.zoomAnimationListener = zoomAnimationListener;
    }
}

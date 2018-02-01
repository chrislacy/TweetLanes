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
public class FlingAnimation implements Animation {

    private float mVelocityX;
    private float mVelocityY;

    private float mFactor = 0.85f;

    private FlingAnimationListener listener;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.polites.android.Transformer#update(com.polites.android.GestureImageView
     * , long)
     */
    @Override
    public boolean update(GestureImageView view, long time) {
        float seconds = (float) time / 1000.0f;

        float dx = mVelocityX * seconds;
        float dy = mVelocityY * seconds;

        mVelocityX *= mFactor;
        mVelocityY *= mFactor;

        float threshold = 10;
        boolean active = (Math.abs(mVelocityX) > threshold && Math
                .abs(mVelocityY) > threshold);

        if (listener != null) {
            listener.onMove(dx, dy);

            if (!active) {
                listener.onComplete();
            }
        }

        return active;
    }

    public void setVelocityX(float velocityX) {
        this.mVelocityX = velocityX;
    }

    public void setVelocityY(float velocityY) {
        this.mVelocityY = velocityY;
    }

    public void setFactor(float factor) {
        this.mFactor = factor;
    }

    public void setListener(FlingAnimationListener listener) {
        this.listener = listener;
    }
}

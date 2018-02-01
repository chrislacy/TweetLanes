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

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

/**
 * @author Jason Polites
 */
class FlingListener extends SimpleOnGestureListener {

    private float mVelocityX;
    private float mVelocityY;

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        this.mVelocityX = velocityX;
        this.mVelocityY = velocityY;
        return true;
    }

    public float getVelocityX() {
        return mVelocityX;
    }

    public float getVelocityY() {
        return mVelocityY;
    }
}

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
package com.tweetlanes.android.widget.gestureimageview;

import android.graphics.PointF;
import android.util.FloatMath;
import android.view.MotionEvent;

public class VectorF {

    public float mAngle;
    public float mLength;

    public final PointF mStart = new PointF();
    public final PointF mEnd = new PointF();

    public void calculateEndPoint() {
        mEnd.x = FloatMath.cos(mAngle) * mLength + mStart.x;
        mEnd.y = FloatMath.sin(mAngle) * mLength + mStart.y;
    }

    public void setStart(PointF p) {
        this.mStart.x = p.x;
        this.mStart.y = p.y;
    }

    public void setEnd(PointF p) {
        this.mEnd.x = p.x;
        this.mEnd.y = p.y;
    }

    public void set(MotionEvent event) {
        this.mStart.x = event.getX(0);
        this.mStart.y = event.getY(0);
        this.mEnd.x = event.getX(1);
        this.mEnd.y = event.getY(1);
    }

    public float calculateLength() {
        mLength = MathUtils.distance(mStart, mEnd);
        return mLength;
    }

    public float calculateAngle() {
        mAngle = MathUtils.angle(mStart, mEnd);
        return mAngle;
    }

}

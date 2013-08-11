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
class Animator extends Thread {

    private final GestureImageView mView;
    private Animation mAnimation;
    private boolean mRunning = false;
    private boolean mActive = false;
    private long mLastTime = -1L;

    public Animator(GestureImageView view, String threadName) {
        super(threadName);
        this.mView = view;
    }

    @Override
    public void run() {

        mRunning = true;

        while (mRunning) {

            while (mActive && mAnimation != null) {
                long time = System.currentTimeMillis();
                mActive = mAnimation.update(mView, time - mLastTime);
                mView.redraw();
                mLastTime = time;

                while (mActive) {
                    try {
                        if (mView.waitForDraw(32)) { // 30Htz
                            break;
                        }
                    } catch (InterruptedException ignore) {
                        mActive = false;
                    }
                }
            }

            synchronized (this) {
                if (mRunning) {
                    try {
                        wait();
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }
    }

    public synchronized void finish() {
        mRunning = false;
        mActive = false;
        notifyAll();
    }

    public void play(Animation transformer) {
        if (mActive) {
            cancel();
        }
        this.mAnimation = transformer;

        activate();
    }

    synchronized void activate() {
        mLastTime = System.currentTimeMillis();
        mActive = true;
        notifyAll();
    }

    public void cancel() {
        mActive = false;
    }
}

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

import android.content.res.Configuration;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

class GestureImageViewTouchListener implements OnTouchListener {

    private final GestureImageView mImage;
    private OnClickListener mOnClickListener;

    private final PointF mCurrent = new PointF();
    private final PointF mLast = new PointF();
    private final PointF mNext = new PointF();
    private final PointF mMidpoint = new PointF();

    private final VectorF mScaleVector = new VectorF();
    private final VectorF mPinchVector = new VectorF();

    private boolean mTouched = false;
    private boolean mInZoom = false;

    private float mInitialDistance;
    private float mLastScale = 1.0f;
    private float mCurrentScale = 1.0f;

    private float mBoundaryLeft = 0;
    private float mBoundaryTop = 0;
    private float mBoundaryRight = 0;
    private float mBoundaryBottom = 0;

    private float mMaxScale = 5.0f;
    private float mMinScale = 0.25f;
    private float mFitScaleHorizontal = 1.0f;
    private float mFitScaleVertical = 1.0f;

    private int mCanvasWidth = 0;
    private int mCanvasHeight = 0;

    private float mCenterX = 0;
    private float mCenterY = 0;

    private float mStartingScale = 0;

    private boolean mCanDragX = false;
    private boolean mCanDragY = false;

    private boolean mMultiTouch = false;

    private final int mDisplayWidth;
    private final int mDisplayHeight;

    private final int mImageWidth;
    private final int mImageHeight;

    private final FlingListener mFlingListener;
    private final FlingAnimation mFlingAnimation;
    private final ZoomAnimation mZoomAnimation;
    private final GestureDetector mTapDetector;
    private final GestureDetector mFlingDetector;
    private final GestureImageViewListener mImageListener;

    public GestureImageViewTouchListener(final GestureImageView image,
                                         int displayWidth, int displayHeight) {
        super();

        this.mImage = image;

        this.mDisplayWidth = displayWidth;
        this.mDisplayHeight = displayHeight;

        this.mCenterX = (float) displayWidth / 2.0f;
        this.mCenterY = (float) displayHeight / 2.0f;

        this.mImageWidth = image.getImageWidth();
        this.mImageHeight = image.getImageHeight();

        mStartingScale = image.getScale();

        mCurrentScale = mStartingScale;
        mLastScale = mStartingScale;

        mBoundaryRight = displayWidth;
        mBoundaryBottom = displayHeight;
        mBoundaryLeft = 0;
        mBoundaryTop = 0;

        mNext.x = image.getImageX();
        mNext.y = image.getImageY();

        mFlingListener = new FlingListener();
        mFlingAnimation = new FlingAnimation();
        mZoomAnimation = new ZoomAnimation();
        MoveAnimation moveAnimation = new MoveAnimation();

        mFlingAnimation.setListener(new FlingAnimationListener() {

            @Override
            public void onMove(float x, float y) {
                handleDrag(mCurrent.x + x, mCurrent.y + y);
            }

            @Override
            public void onComplete() {
            }
        });

        mZoomAnimation.setZoom(2.0f);
        mZoomAnimation.setZoomAnimationListener(new ZoomAnimationListener() {

            @Override
            public void onZoom(float scale, float x, float y) {
                if (scale <= mMaxScale && scale >= mMinScale) {
                    handleScale(scale, x, y);
                }
            }

            @Override
            public void onComplete() {
                mInZoom = false;
                handleUp();
            }
        });

        moveAnimation.setMoveAnimationListener(new MoveAnimationListener() {

            @Override
            public void onMove(float x, float y) {
                image.setPosition(x, y);
                image.redraw();
            }
        });

        mTapDetector = new GestureDetector(image.getContext(),
                new SimpleOnGestureListener() {

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        startZoom(e);
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        if (!mInZoom) {
                            if (mOnClickListener != null) {
                                mOnClickListener.onClick(image);
                                return true;
                            }
                        }

                        return false;
                    }
                });

        mFlingDetector = new GestureDetector(image.getContext(), mFlingListener);
        mImageListener = image.getGestureImageViewListener();

        calculateBoundaries();
    }

    private void startFling() {
        mFlingAnimation.setVelocityX(mFlingListener.getVelocityX());
        mFlingAnimation.setVelocityY(mFlingListener.getVelocityY());
        mImage.animationStart(mFlingAnimation);
    }

    private void startZoom(MotionEvent e) {
        mInZoom = true;
        mZoomAnimation.reset();

        float zoomTo;

        if (mImage.isLandscape()) {
            if (mImage.getDeviceOrientation() == Configuration.ORIENTATION_PORTRAIT) {
                int scaledHeight = mImage.getScaledHeight();

                if (scaledHeight < mCanvasHeight) {
                    zoomTo = mFitScaleVertical / mCurrentScale;
                    mZoomAnimation.setTouchX(e.getX());
                    mZoomAnimation.setTouchY(mImage.getCenterY());
                } else {
                    zoomTo = mFitScaleHorizontal / mCurrentScale;
                    mZoomAnimation.setTouchX(mImage.getCenterX());
                    mZoomAnimation.setTouchY(mImage.getCenterY());
                }
            } else {
                int scaledWidth = mImage.getScaledWidth();

                if (scaledWidth == mCanvasWidth) {
                    zoomTo = mCurrentScale * 4.0f;
                    mZoomAnimation.setTouchX(e.getX());
                    mZoomAnimation.setTouchY(e.getY());
                } else if (scaledWidth < mCanvasWidth) {
                    zoomTo = mFitScaleHorizontal / mCurrentScale;
                    mZoomAnimation.setTouchX(mImage.getCenterX());
                    mZoomAnimation.setTouchY(e.getY());
                } else {
                    zoomTo = mFitScaleHorizontal / mCurrentScale;
                    mZoomAnimation.setTouchX(mImage.getCenterX());
                    mZoomAnimation.setTouchY(mImage.getCenterY());
                }
            }
        } else {
            if (mImage.getDeviceOrientation() == Configuration.ORIENTATION_PORTRAIT) {

                int scaledHeight = mImage.getScaledHeight();

                if (scaledHeight == mCanvasHeight) {
                    zoomTo = mCurrentScale * 4.0f;
                    mZoomAnimation.setTouchX(e.getX());
                    mZoomAnimation.setTouchY(e.getY());
                } else if (scaledHeight < mCanvasHeight) {
                    zoomTo = mFitScaleVertical / mCurrentScale;
                    mZoomAnimation.setTouchX(e.getX());
                    mZoomAnimation.setTouchY(mImage.getCenterY());
                } else {
                    zoomTo = mFitScaleVertical / mCurrentScale;
                    mZoomAnimation.setTouchX(mImage.getCenterX());
                    mZoomAnimation.setTouchY(mImage.getCenterY());
                }
            } else {
                int scaledWidth = mImage.getScaledWidth();

                if (scaledWidth < mCanvasWidth) {
                    zoomTo = mFitScaleHorizontal / mCurrentScale;
                    mZoomAnimation.setTouchX(mImage.getCenterX());
                    mZoomAnimation.setTouchY(e.getY());
                } else {
                    zoomTo = mFitScaleVertical / mCurrentScale;
                    mZoomAnimation.setTouchX(mImage.getCenterX());
                    mZoomAnimation.setTouchY(mImage.getCenterY());
                }
            }
        }

        mZoomAnimation.setZoom(zoomTo);
        mImage.animationStart(mZoomAnimation);
    }

    private void stopAnimations() {
        mImage.animationStop();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (!mInZoom) {

            if (!mTapDetector.onTouchEvent(event)) {
                if (event.getPointerCount() == 1
                        && mFlingDetector.onTouchEvent(event)) {
                    startFling();
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    handleUp();
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    stopAnimations();

                    mLast.x = event.getX();
                    mLast.y = event.getY();

                    if (mImageListener != null) {
                        mImageListener.onTouch(mLast.x, mLast.y);
                    }

                    mTouched = true;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (event.getPointerCount() > 1) {
                        mMultiTouch = true;
                        if (mInitialDistance > 0) {

                            mPinchVector.set(event);
                            mPinchVector.calculateLength();

                            float distance = mPinchVector.length;

                            if (mInitialDistance != distance) {

                                float newScale = (distance / mInitialDistance)
                                        * mLastScale;

                                if (newScale <= mMaxScale) {
                                    mScaleVector.length *= newScale;

                                    mScaleVector.calculateEndPoint();

                                    mScaleVector.length /= newScale;

                                    float newX = mScaleVector.end.x;
                                    float newY = mScaleVector.end.y;

                                    handleScale(newScale, newX, newY);
                                }
                            }
                        } else {
                            mInitialDistance = MathUtils.distance(event);

                            MathUtils.midpoint(event, mMidpoint);

                            mScaleVector.setStart(mMidpoint);
                            mScaleVector.setEnd(mNext);

                            mScaleVector.calculateLength();
                            mScaleVector.calculateAngle();

                            mScaleVector.length /= mLastScale;
                        }
                    } else {
                        if (!mTouched) {
                            mTouched = true;
                            mLast.x = event.getX();
                            mLast.y = event.getY();
                            mNext.x = mImage.getImageX();
                            mNext.y = mImage.getImageY();
                        } else if (!mMultiTouch) {
                            if (handleDrag(event.getX(), event.getY())) {
                                mImage.redraw();
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    void handleUp() {

        mMultiTouch = false;

        mInitialDistance = 0;
        mLastScale = mCurrentScale;

        if (!mCanDragX) {
            mNext.x = mCenterX;
        }

        if (!mCanDragY) {
            mNext.y = mCenterY;
        }

        boundCoordinates();

        if (!mCanDragX && !mCanDragY) {

            if (mImage.isLandscape()) {
                mCurrentScale = mFitScaleHorizontal;
                mLastScale = mFitScaleHorizontal;
            } else {
                mCurrentScale = mFitScaleVertical;
                mLastScale = mFitScaleVertical;
            }
        }

        mImage.setScale(mCurrentScale);
        mImage.setPosition(mNext.x, mNext.y);

        if (mImageListener != null) {
            mImageListener.onScale(mCurrentScale);
            mImageListener.onPosition(mNext.x, mNext.y);
        }

        mImage.redraw();
    }

    void handleScale(float scale, float x, float y) {

        mCurrentScale = scale;

        if (mCurrentScale > mMaxScale) {
            mCurrentScale = mMaxScale;
        } else if (mCurrentScale < mMinScale) {
            mCurrentScale = mMinScale;
        } else {
            mNext.x = x;
            mNext.y = y;
        }

        calculateBoundaries();

        mImage.setScale(mCurrentScale);
        mImage.setPosition(mNext.x, mNext.y);

        if (mImageListener != null) {
            mImageListener.onScale(mCurrentScale);
            mImageListener.onPosition(mNext.x, mNext.y);
        }

        mImage.redraw();
    }

    boolean handleDrag(float x, float y) {
        mCurrent.x = x;
        mCurrent.y = y;

        float diffX = (mCurrent.x - mLast.x);
        float diffY = (mCurrent.y - mLast.y);

        if (diffX != 0 || diffY != 0) {

            if (mCanDragX) mNext.x += diffX;
            if (mCanDragY) mNext.y += diffY;

            boundCoordinates();

            mLast.x = mCurrent.x;
            mLast.y = mCurrent.y;

            if (mCanDragX || mCanDragY) {
                mImage.setPosition(mNext.x, mNext.y);

                if (mImageListener != null) {
                    mImageListener.onPosition(mNext.x, mNext.y);
                }

                return true;
            }
        }

        return false;
    }

    public void reset() {
        mCurrentScale = mStartingScale;
        mNext.x = mCenterX;
        mNext.y = mCenterY;
        calculateBoundaries();
        mImage.setScale(mCurrentScale);
        mImage.setPosition(mNext.x, mNext.y);
        mImage.redraw();
    }

    public float getMaxScale() {
        return mMaxScale;
    }

    public void setMaxScale(float maxScale) {
        this.mMaxScale = maxScale;
    }

    public float getMinScale() {
        return mMinScale;
    }

    public void setMinScale(float minScale) {
        this.mMinScale = minScale;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.mOnClickListener = onClickListener;
    }

    void setCanvasWidth(int canvasWidth) {
        this.mCanvasWidth = canvasWidth;
    }

    void setCanvasHeight(int canvasHeight) {
        this.mCanvasHeight = canvasHeight;
    }

    void setFitScaleHorizontal(float fitScale) {
        this.mFitScaleHorizontal = fitScale;
    }

    void setFitScaleVertical(float fitScaleVertical) {
        this.mFitScaleVertical = fitScaleVertical;
    }

    void boundCoordinates() {
        if (mNext.x < mBoundaryLeft) {
            mNext.x = mBoundaryLeft;
        } else if (mNext.x > mBoundaryRight) {
            mNext.x = mBoundaryRight;
        }

        if (mNext.y < mBoundaryTop) {
            mNext.y = mBoundaryTop;
        } else if (mNext.y > mBoundaryBottom) {
            mNext.y = mBoundaryBottom;
        }
    }

    void calculateBoundaries() {

        int effectiveWidth = Math.round((float) mImageWidth * mCurrentScale);
        int effectiveHeight = Math.round((float) mImageHeight * mCurrentScale);

        mCanDragX = effectiveWidth > mDisplayWidth;
        mCanDragY = effectiveHeight > mDisplayHeight;

        if (mCanDragX) {
            float diff = (float) (effectiveWidth - mDisplayWidth) / 2.0f;
            mBoundaryLeft = mCenterX - diff;
            mBoundaryRight = mCenterX + diff;
        }

        if (mCanDragY) {
            float diff = (float) (effectiveHeight - mDisplayHeight) / 2.0f;
            mBoundaryTop = mCenterY - diff;
            mBoundaryBottom = mCenterY + diff;
        }
    }
}

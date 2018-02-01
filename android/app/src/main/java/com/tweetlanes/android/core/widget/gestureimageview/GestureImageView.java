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

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class GestureImageView extends ImageView {

    private static final String GLOBAL_NS = "http://schemas.android.com/apk/res/android";
    private static final String LOCAL_NS = "http://schemas.polites.com/android";

    private final Semaphore mDrawLock = new Semaphore(0);
    private Animator mAnimator;

    private Drawable mDrawable;

    private float mX = 0, mY = 0;

    private boolean mLayout = false;

    private float mScaleAdjust = 1.0f;
    private float mStartingScale = -1.0f;

    private float mMaxScale = 5.0f;
    private float mMinScale = 0.75f;
    private float mFitScaleHorizontal = 1.0f;
    private float mFitScaleVertical = 1.0f;
    private float mRotation = 0.0f;

    private float mCenterX;
    private float mCenterY;

    private Float mStartX, mStartY;

    private int mResId = -1;
    private boolean mRecycle = false;
    private boolean mStrict = false;

    private int mDisplayHeight;
    private int mDisplayWidth;

    private int mAlpha = 255;
    private ColorFilter mColorFilter;

    private int mDeviceOrientation = -1;
    private int mImageOrientation;

    private GestureImageViewListener mGestureImageViewListener;
    private GestureImageViewTouchListener mGestureImageViewTouchListener;

    private OnTouchListener mCustomOnTouchListener;
    private OnClickListener mOnClickListener;

    public GestureImageView(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs);
    }

    public GestureImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        String scaleType = attrs.getAttributeValue(GLOBAL_NS, "scaleType");

        if (scaleType == null || scaleType.trim().length() == 0) {
            setScaleType(ScaleType.CENTER_INSIDE);
        }

        String strStartX = attrs.getAttributeValue(LOCAL_NS, "start-x");
        String strStartY = attrs.getAttributeValue(LOCAL_NS, "start-y");

        if (strStartX != null && strStartX.trim().length() > 0) {
            mStartX = Float.parseFloat(strStartX);
        }

        if (strStartY != null && strStartY.trim().length() > 0) {
            mStartY = Float.parseFloat(strStartY);
        }

        setStartingScale(attrs.getAttributeFloatValue(LOCAL_NS, "start-scale",
                mStartingScale));
        setMinScale(attrs.getAttributeFloatValue(LOCAL_NS, "min-scale",
                mMinScale));
        setMaxScale(attrs.getAttributeFloatValue(LOCAL_NS, "max-scale",
                mMaxScale));
        setStrict(attrs.getAttributeBooleanValue(LOCAL_NS, "strict", mStrict));
        setRecycle(attrs
                .getAttributeBooleanValue(LOCAL_NS, "recycle", mRecycle));

        initImage();
    }

    public GestureImageView(Context context) {
        super(context);
        setScaleType(ScaleType.CENTER_INSIDE);
        initImage();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (mDrawable != null) {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mDisplayHeight = MeasureSpec.getSize(heightMeasureSpec);

                if (getLayoutParams().width == LayoutParams.WRAP_CONTENT) {
                    float ratio = (float) getImageWidth()
                            / (float) getImageHeight();
                    mDisplayWidth = Math.round((float) mDisplayHeight * ratio);
                } else {
                    mDisplayWidth = MeasureSpec.getSize(widthMeasureSpec);
                }
            } else {
                mDisplayWidth = MeasureSpec.getSize(widthMeasureSpec);
                if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
                    float ratio = (float) getImageHeight()
                            / (float) getImageWidth();
                    mDisplayHeight = Math.round((float) mDisplayWidth * ratio);
                } else {
                    mDisplayHeight = MeasureSpec.getSize(heightMeasureSpec);
                }
            }
        } else {
            mDisplayHeight = MeasureSpec.getSize(heightMeasureSpec);
            mDisplayWidth = MeasureSpec.getSize(widthMeasureSpec);
        }

        setMeasuredDimension(mDisplayWidth, mDisplayHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed || !mLayout) {
            setupCanvas(mDisplayWidth, mDisplayHeight, getResources()
                    .getConfiguration().orientation);
        }
    }

    void setupCanvas(int measuredWidth, int measuredHeight,
                     int orientation) {

        if (mDeviceOrientation != orientation) {
            mLayout = false;
            mDeviceOrientation = orientation;
        }

        if (mDrawable != null && !mLayout) {
            int imageWidth = getImageWidth();
            int imageHeight = getImageHeight();

            int HWidth = Math.round(((float) imageWidth / 2.0f));
            int HHeight = Math.round(((float) imageHeight / 2.0f));

            measuredWidth -= (getPaddingLeft() + getPaddingRight());
            measuredHeight -= (getPaddingTop() + getPaddingBottom());

            computeCropScale(imageWidth, imageHeight, measuredWidth,
                    measuredHeight);

            if (mStartingScale <= 0.0f) {
                computeStartingScale(imageWidth, imageHeight, measuredWidth,
                        measuredHeight);
            }

            mScaleAdjust = mStartingScale;

            this.mCenterX = (float) measuredWidth / 2.0f;
            this.mCenterY = (float) measuredHeight / 2.0f;

            if (mStartX == null) {
                mX = mCenterX;
            } else {
                mX = mStartX;
            }

            if (mStartY == null) {
                mY = mCenterY;
            } else {
                mY = mStartY;
            }

            mGestureImageViewTouchListener = new GestureImageViewTouchListener(
                    this, measuredWidth, measuredHeight);

            if (isLandscape()) {
                mGestureImageViewTouchListener.setMinScale(mMinScale
                        * mFitScaleHorizontal);
            } else {
                mGestureImageViewTouchListener.setMinScale(mMinScale
                        * mFitScaleVertical);
            }

            mGestureImageViewTouchListener.setMaxScale(mMaxScale
                    * mStartingScale);

            mGestureImageViewTouchListener
                    .setFitScaleHorizontal(mFitScaleHorizontal);
            mGestureImageViewTouchListener
                    .setFitScaleVertical(mFitScaleVertical);
            mGestureImageViewTouchListener.setCanvasWidth(measuredWidth);
            mGestureImageViewTouchListener.setCanvasHeight(measuredHeight);
            mGestureImageViewTouchListener.setOnClickListener(mOnClickListener);

            mDrawable.setBounds(-HWidth, -HHeight, HWidth, HHeight);

            super.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (mCustomOnTouchListener != null) {
                        mCustomOnTouchListener.onTouch(v, event);
                    }
                    return mGestureImageViewTouchListener.onTouch(v, event);
                }
            });

            mLayout = true;
        }
    }

    void computeCropScale(int imageWidth, int imageHeight,
                          int measuredWidth, int measuredHeight) {
        mFitScaleHorizontal = (float) measuredWidth / (float) imageWidth;
        mFitScaleVertical = (float) measuredHeight / (float) imageHeight;
    }

    void computeStartingScale(int imageWidth, int imageHeight,
                              int measuredWidth, int measuredHeight) {
        switch (getScaleType()) {
            case CENTER:
                // Center the image in the view, but perform no scaling.
                mStartingScale = 1.0f;
                break;

            case CENTER_CROP:
                mStartingScale = Math.max((float) measuredHeight
                        / (float) imageHeight, (float) measuredWidth
                        / (float) imageWidth);
                break;

            case CENTER_INSIDE:
                if (isLandscape()) {
                    mStartingScale = mFitScaleHorizontal;
                } else {
                    mStartingScale = mFitScaleVertical;
                }
                break;
        }
    }

    boolean isNotRecycled() {
        if (mDrawable != null && mDrawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) mDrawable).getBitmap();
            if (bitmap != null) {
                return !bitmap.isRecycled();
            }
        }
        return true;
    }

    void recycle() {
        if (mRecycle && mDrawable != null
                && mDrawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) mDrawable).getBitmap();
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mLayout) {
            if (mDrawable != null && isNotRecycled()) {
                canvas.save();

                float scale = 1.0f;
                float adjustedScale = scale * mScaleAdjust;

                canvas.translate(mX, mY);

                if (mRotation != 0.0f) {
                    canvas.rotate(mRotation);
                }

                if (adjustedScale != 1.0f) {
                    canvas.scale(adjustedScale, adjustedScale);
                }

                mDrawable.draw(canvas);

                canvas.restore();
            }

            if (mDrawLock.availablePermits() <= 0) {
                mDrawLock.release();
            }
        }
    }

    /**
     * Waits for a draw
     *
     * @param max time to wait for draw (ms)
     * @throws InterruptedException
     */
    public boolean waitForDraw(long timeout) throws InterruptedException {
        return mDrawLock.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onAttachedToWindow() {
        mAnimator = new Animator(this, "GestureImageViewAnimator");
        mAnimator.start();

        if (mResId >= 0 && mDrawable == null) {
            setImageResource(mResId);
        }

        super.onAttachedToWindow();
    }

    public void animationStart(Animation animation) {
        if (mAnimator != null) {
            mAnimator.play(animation);
        }
    }

    public void animationStop() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAnimator != null) {
            mAnimator.finish();
        }
        if (mRecycle && mDrawable != null && isNotRecycled()) {
            recycle();
            mDrawable = null;
        }
        super.onDetachedFromWindow();
    }

    void initImage() {
        if (this.mDrawable != null) {
            this.mDrawable.setAlpha(mAlpha);
            this.mDrawable.setFilterBitmap(true);
            if (mColorFilter != null) {
                this.mDrawable.setColorFilter(mColorFilter);
            }
        }

        if (!mLayout) {
            requestLayout();
            redraw();
        }
    }

    public void setImageBitmap(Bitmap image) {
        this.mDrawable = new BitmapDrawable(getResources(), image);
        initImage();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        this.mDrawable = drawable;
        initImage();
    }

    public void setImageResource(int id) {
        if (this.mDrawable != null) {
            this.recycle();
        }
        if (id >= 0) {
            this.mResId = id;
            setImageDrawable(getContext().getResources().getDrawable(id));
        }
    }

    public int getScaledWidth() {
        return Math.round(getImageWidth() * getScale());
    }

    public int getScaledHeight() {
        return Math.round(getImageHeight() * getScale());
    }

    public int getImageWidth() {
        if (mDrawable != null) {
            return mDrawable.getIntrinsicWidth();
        }
        return 0;
    }

    public int getImageHeight() {
        if (mDrawable != null) {
            return mDrawable.getIntrinsicHeight();
        }
        return 0;
    }

    public void moveBy(float x, float y) {
        this.mX += x;
        this.mY += y;
    }

    public void setPosition(float x, float y) {
        this.mX = x;
        this.mY = y;
    }

    public void redraw() {
        postInvalidate();
    }

    void setMinScale(float min) {
        this.mMinScale = min;
        if (mGestureImageViewTouchListener != null) {
            mGestureImageViewTouchListener.setMinScale(min
                    * mFitScaleHorizontal);
        }
    }

    void setMaxScale(float max) {
        this.mMaxScale = max;
        if (mGestureImageViewTouchListener != null) {
            mGestureImageViewTouchListener.setMaxScale(max * mStartingScale);
        }
    }

    public void setScale(float scale) {
        mScaleAdjust = scale;
    }

    public float getScale() {
        return mScaleAdjust;
    }

    public float getImageX() {
        return mX;
    }

    public float getImageY() {
        return mY;
    }

    public boolean isStrict() {
        return mStrict;
    }

    void setStrict(boolean strict) {
        this.mStrict = strict;
    }

    public boolean isRecycle() {
        return mRecycle;
    }

    void setRecycle(boolean recycle) {
        this.mRecycle = recycle;
    }

    public void reset() {
        mX = mCenterX;
        mY = mCenterY;
        mScaleAdjust = mStartingScale;
        redraw();
    }

    public void setRotation(float rotation) {
        this.mRotation = rotation;
    }

    public void setGestureImageViewListener(
            GestureImageViewListener pinchImageViewListener) {
        this.mGestureImageViewListener = pinchImageViewListener;
    }

    public GestureImageViewListener getGestureImageViewListener() {
        return mGestureImageViewListener;
    }

    @Override
    public Drawable getDrawable() {
        return mDrawable;
    }

    @Override
    public void setAlpha(int alpha) {
        this.mAlpha = alpha;
        if (mDrawable != null) {
            mDrawable.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        this.mColorFilter = cf;
        if (mDrawable != null) {
            mDrawable.setColorFilter(cf);
        }
    }

    @Override
    public void setImageURI(Uri mUri) {
        if ("content".equals(mUri.getScheme())) {
            try {
                String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};

                Cursor cur = getContext().getContentResolver().query(mUri,
                        orientationColumn, null, null, null);

                if (cur != null && cur.moveToFirst()) {
                    mImageOrientation = cur.getInt(cur
                            .getColumnIndex(orientationColumn[0]));
                }

                InputStream in = null;

                try {
                    in = getContext().getContentResolver()
                            .openInputStream(mUri);
                    Bitmap bmp = BitmapFactory.decodeStream(in);

                    if (mImageOrientation != 0) {
                        Matrix m = new Matrix();
                        m.postRotate(mImageOrientation);
                        Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0,
                                bmp.getWidth(), bmp.getHeight(), m, true);
                        bmp.recycle();
                        setImageDrawable(new BitmapDrawable(getResources(),
                                rotated));
                    } else {
                        setImageDrawable(new BitmapDrawable(getResources(), bmp));
                    }
                } finally {
                    if (in != null) {
                        in.close();
                    }

                    if (cur != null) {
                        cur.close();
                    }
                }
            } catch (Exception e) {
                Log.w("GestureImageView", "Unable to open content: " + mUri, e);
            }
        } else {
            setImageDrawable(Drawable.createFromPath(mUri.toString()));
        }

        if (mDrawable == null) {
            Log.e("GestureImageView", "resolveUri failed on bad bitmap uri: "
                    + mUri);
            // Don't try again.
            mUri = null;
        }
    }

    @Override
    public Matrix getImageMatrix() {
        if (mStrict) {
            throw new UnsupportedOperationException("Not supported");
        }
        return super.getImageMatrix();
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (scaleType == ScaleType.CENTER || scaleType == ScaleType.CENTER_CROP
                || scaleType == ScaleType.CENTER_INSIDE) {

            super.setScaleType(scaleType);
        } else if (mStrict) {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    @Override
    public void invalidateDrawable(Drawable dr) {
        if (mStrict) {
            throw new UnsupportedOperationException("Not supported");
        }
        super.invalidateDrawable(dr);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (mStrict) {
            throw new UnsupportedOperationException("Not supported");
        }
        return super.onCreateDrawableState(extraSpace);
    }

    @Override
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (mStrict) {
            throw new UnsupportedOperationException("Not supported");
        }
        super.setAdjustViewBounds(adjustViewBounds);
    }

    @Override
    public void setImageLevel(int level) {
        if (mStrict) {
            throw new UnsupportedOperationException("Not supported");
        }
        super.setImageLevel(level);
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        if (mStrict) {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    @Override
    public void setImageState(int[] state, boolean merge) {
        if (mStrict) {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    @Override
    public void setSelected(boolean selected) {
        if (mStrict) {
            throw new UnsupportedOperationException("Not supported");
        }
        super.setSelected(selected);
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        this.mCustomOnTouchListener = l;
    }

    public float getCenterX() {
        return mCenterX;
    }

    public float getCenterY() {
        return mCenterY;
    }

    public boolean isLandscape() {
        return getImageWidth() >= getImageHeight();
    }

    boolean isPortrait() {
        return getImageWidth() <= getImageHeight();
    }

    void setStartingScale(float startingScale) {
        this.mStartingScale = startingScale;
    }

    public void setStartingPosition(float x, float y) {
        this.mStartX = x;
        this.mStartY = y;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        this.mOnClickListener = l;

        if (mGestureImageViewTouchListener != null) {
            mGestureImageViewTouchListener.setOnClickListener(l);
        }
    }

    /**
     * Returns true if the image dimensions are aligned with the orientation of
     * the device.
     *
     * @return
     */
    public boolean isOrientationAligned() {
        if (mDeviceOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            return isLandscape();
        } else if (mDeviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
            return isPortrait();
        }
        return true;
    }

    public int getDeviceOrientation() {
        return mDeviceOrientation;
    }
}

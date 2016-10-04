/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.largeimageview;

/*
 * Created by Hippo on 9/26/2016.
 */

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.Interpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

// TODO Add Gesture listener to override
public class LargeImageView extends View implements ImageSource.Callback, GestureRecognizer.Listener {

    @IntDef({ORIENTATION_0, ORIENTATION_90, ORIENTATION_180, ORIENTATION_270})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Orientation {}

    public static final int ORIENTATION_0 = 0;
    public static final int ORIENTATION_90 = 1;
    public static final int ORIENTATION_180 = 2;
    public static final int ORIENTATION_270 = 3;

    private static final float MAX_SCALE = 8.0f;
    private static final float MIN_SCALE = 1.0f / 8.0f;

    private static final Interpolator FAST_SLOW_INTERPOLATOR = new LinearOutSlowInInterpolator();

    @Orientation
    private int mOrientation = ORIENTATION_0;
    private float mScale;

    private ImageSource mImage;
    private int mImageWidth;
    private int mImageHeight;

    // Window width of Image
    private int mWindowWidth = 0;
    // Window height of Image
    private int mWindowHeight = 0;
    // Max bitmap width and height
    private int mMaxBitmapSize = 0;

    // True to update mSrcActual and mDstActual
    private boolean mRectDirty;

    // The area in view for whole Image
    private final RectF mDst = new RectF();
    // The area in image to draw
    private final RectF mSrcActual = new RectF();
    // The area in view to draw
    private final RectF mDstActual = new RectF();

    // The scale to fit view.
    private float mFitScale;
    // The max value that scale can be.
    private float mMaxScale;
    // The min value that scale can be.
    private float mMinScale;
    // fit width, fit height, 1.0f
    private final float[] mScaleArray = new float[3];

    // Current running animator count
    private int mAnimating;

    private GestureRecognizer mGestureRecognizer;

    @Nullable
    private SmoothScaler mSmoothScaler;
    @Nullable
    private ImageFling mImageFling;

    private ImageInitListener mImageInitListener;

    private final PointF mTempPointF = new PointF();
    // The dump drawable to call
    // scheduleDrawable and unscheduleDrawable.
    private Drawable mDumpDrawable;

    public LargeImageView(Context context) {
        super(context);
        init(context);
    }

    public LargeImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LargeImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mGestureRecognizer = new GestureRecognizer(context, this);
    }

    public void setImageInitListener(ImageInitListener imageInitListener) {
        mImageInitListener = imageInitListener;
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        // We use dump drawable to call scheduleDrawable and unscheduleDrawable,
        // so verify dump drawable here.
        return super.verifyDrawable(who) || (mDumpDrawable != null && who == mDumpDrawable);
    }

    private void updateScale() {
        final int wWidth = mWindowWidth;
        final int wHeight = mWindowHeight;
        if (wWidth <= 0 || wHeight <= 0) {
            return;
        }
        final int iWidth = mImageWidth;
        final int iHeight = mImageHeight;
        if (iWidth <= 0 || iHeight <= 0) {
            return;
        }

        final float widthScale = (float) wWidth / iWidth;
        final float heightScale = (float) wHeight / iHeight;
        final float fitScale = Math.min(widthScale, heightScale);

        mFitScale = fitScale;
        mMaxScale = Math.max(MAX_SCALE, fitScale);
        mMinScale = Math.min(MIN_SCALE, fitScale);

        final float[] scaleArray = mScaleArray;
        scaleArray[0] = clamp(widthScale, mMinScale, mMaxScale);
        scaleArray[1] = clamp(heightScale, mMinScale, mMaxScale);
        scaleArray[2] = 1.0f;
        Arrays.sort(scaleArray);
    }

    // Fit windows center
    private void initPosition() {
        final int wWidth = mWindowWidth;
        final int wHeight = mWindowHeight;
        if (wWidth <= 0 || wHeight <= 0) {
            return;
        }
        final int iWidth = mImageWidth;
        final int iHeight = mImageHeight;
        if (iWidth <= 0 || iHeight <= 0) {
            return;
        }

        // Set scale
        mScale = mFitScale;
        final float dWidth = iWidth * mScale;
        final float dHeight = iHeight * mScale;

        // Set mDst.left and mDst.right
        final RectF dst = mDst;
        dst.left = (wWidth - dWidth) / 2;
        dst.top = (wHeight - dHeight) / 2;

        // Set mDst.right and mDst.bottom
        dst.right = dst.left + dWidth;
        dst.bottom = dst.top + dHeight;

        // Adjust position
        adjustPosition();

        mRectDirty = true;
    }

    // Make sure scale is in [mMinScale, mMaxScale]
    private void adjustScale() {
        final int iWidth = mImageWidth;
        final int iHeight = mImageHeight;
        if (iWidth <= 0 || iHeight <= 0) {
            return;
        }
        final RectF dst = mDst;
        if (dst.isEmpty()) {
            return;
        }

        final float oldScale = mScale;
        mScale = clamp(oldScale, mMinScale, mMaxScale);
        if (oldScale == mScale) {
            return;
        }

        dst.right = dst.left + mScale * iWidth;
        dst.bottom = dst.top + mScale * iHeight;
        mRectDirty = true;
    }

    // If target is smaller then view, make it in screen center.
    // If target is larger then view, make it fill screen.
    private void adjustPosition() {
        final int wWidth = mWindowWidth;
        final int wHeight = mWindowHeight;
        if (wWidth <= 0 || wHeight <= 0) {
            return;
        }
        final RectF dst = mDst;
        final float dWidth = dst.width();
        final float dHeight = dst.height();
        if (dWidth <= 0 || dHeight <= 0) {
            return;
        }

        if (dWidth > wWidth) {
            float fixXOffset = dst.left;
            if (fixXOffset > 0) {
                dst.left -= fixXOffset;
                dst.right -= fixXOffset;
                mRectDirty = true;
            } else if ((fixXOffset = wWidth - dst.right) > 0) {
                dst.left += fixXOffset;
                dst.right += fixXOffset;
                mRectDirty = true;
            }
        } else {
            final float left = (wWidth - dWidth) / 2;
            dst.offsetTo(left, dst.top);
            mRectDirty = true;
        }
        if (dHeight > wHeight) {
            float fixYOffset = dst.top;
            if (fixYOffset > 0) {
                dst.top -= fixYOffset;
                dst.bottom -= fixYOffset;
                mRectDirty = true;
            } else if ((fixYOffset = wHeight - dst.bottom) > 0) {
                dst.top += fixYOffset;
                dst.bottom += fixYOffset;
                mRectDirty = true;
            }
        } else {
            final float top = (wHeight - dHeight) / 2;
            dst.offsetTo(dst.left, top);
            mRectDirty = true;
        }
    }

    // Update mWindowWidth and mWindowHeight
    private void setWindowSize(int w, int h) {
        switch (mOrientation) {
            case ORIENTATION_0:
            case ORIENTATION_180:
                mWindowWidth = w;
                mWindowHeight = h;
                break;
            case ORIENTATION_90:
            case ORIENTATION_270:
                mWindowWidth = h;
                mWindowHeight = w;
                break;
            default:
                throw new IllegalStateException("Unknown orientation: " + mOrientation);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cancelAllAnimator();

        // Assign window width and height
        setWindowSize(w, h);

        if (mImage != null) {
            mImage.setWindowSize(w, h);
            updateScale();
            if (mDst.isEmpty()) {
                initPosition();
            } else {
                adjustScale();
                adjustPosition();
            }
        }
    }

    /**
     * Return current orientation.
     */
    @Orientation
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * Set orientation for {@code ImageSource}.
     * Must be one of {@link #ORIENTATION_0}, {@link #ORIENTATION_90},
     * {@link #ORIENTATION_180} and {@link #ORIENTATION_270}.
     */
    public void setOrientation(@Orientation int orientation) {
        if (mOrientation == orientation) {
            return;
        }
        mOrientation = orientation;

        cancelAllAnimator();

        // Window size might be still the same, check it
        final int oldWWidth = mWindowWidth;
        final int oldWHeight = mWindowHeight;
        setWindowSize(getWidth(), getHeight());
        if (oldWWidth != mWindowWidth || oldWHeight != mWindowHeight) {
            mImage.setWindowSize(mWindowWidth, mWindowHeight);
            updateScale();
            initPosition();
        }
        invalidate();
    }

    private void translate(float dx, float dy) {
        final int wWidth = mWindowWidth;
        final int wHeight = mWindowHeight;
        if (wWidth <= 0 || wHeight <= 0) {
            return;
        }
        final RectF dst = mDst;
        final float dWidth = dst.width();
        final float dHeight = dst.height();
        if (dWidth <= 0 || dHeight <= 0) {
            return;
        }

        final float remainX;
        final float remainY;

        if (dWidth > wWidth) {
            dst.left -= dx;
            dst.right -= dx;

            float fixXOffset = dst.left;
            if (fixXOffset > 0) {
                dst.left -= fixXOffset;
                dst.right -= fixXOffset;
                remainX = -fixXOffset;
            } else if ((fixXOffset = wWidth - dst.right) > 0) {
                dst.left += fixXOffset;
                dst.right += fixXOffset;
                remainX = fixXOffset;
            } else {
                remainX = 0;
            }
        } else {
            remainX = dx;
        }
        if (dHeight > wHeight) {
            dst.top -= dy;
            dst.bottom -= dy;

            float fixYOffset = dst.top;
            if (fixYOffset > 0) {
                dst.top -= fixYOffset;
                dst.bottom -= fixYOffset;
                remainY = -fixYOffset;
            } else if ((fixYOffset = wHeight - dst.bottom) > 0) {
                dst.top += fixYOffset;
                dst.bottom += fixYOffset;
                remainY = fixYOffset;
            } else {
                remainY = 0;
            }
        } else {
            remainY = dy;
        }

        // Check requestDisallowInterceptTouchEvent
        // Don't call requestDisallowInterceptTouchEvent when animated
        // Only call requestDisallowInterceptTouchEvent when on room for scroll left or right
        if (mAnimating == 0 && dx == remainX) {
            final ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(false);
            }
        }

        if (dx != remainX || dy != remainY) {
            mRectDirty = true;
            invalidate();
        }
    }

    private void setScale(float x, float y, float scale) {
        final int iWidth = mImageWidth;
        final int iHeight = mImageHeight;
        if (iWidth <= 0 || iHeight <= 0) {
            return;
        }
        final RectF dst = mDst;
        final float dWidth = dst.width();
        final float dHeight = dst.height();
        if (dWidth <= 0 || dHeight <= 0) {
            return;
        }

        scale = clamp(scale, mMinScale, mMaxScale);
        if (mScale == scale) {
            return;
        }

        final float sScale = scale / mScale;
        mScale = scale;
        dst.left = (x - ((x - dst.left) * sScale));
        dst.top = (y - ((y - dst.top) * sScale));
        dst.right = dst.left + (iWidth * scale);
        dst.bottom = dst.top + (iHeight * scale);

        // Adjust position
        adjustPosition();

        mRectDirty = true;
        invalidate();
    }

    private void scale(float x, float y, float scale) {
        setScale(x, y, mScale * scale);
    }

    private void cancelAllAnimator() {
        if (mSmoothScaler != null) {
            mSmoothScaler.cancel();
        }
        if (mImageFling != null) {
            mImageFling.cancel();
        }
    }

    private void scaleToNextLevel(float x, float y) {
        if (!isReady()) {
            return;
        }

        final float scale = mScale;
        float endScale = mScaleArray[0];
        for (final float value: mScaleArray) {
            if (scale < value - 0.01f) {
                endScale = value;
                break;
            }
        }

        if (mSmoothScaler == null) {
            mSmoothScaler = new SmoothScaler(this);
        }
        mSmoothScaler.startSmoothScaler(x, y, endScale);
    }

    private void fling(float velocityX, float velocityY) {
        final int wWidth = mWindowWidth;
        final int wHeight = mWindowHeight;
        if (wWidth <= 0 || wHeight <= 0) {
            return;
        }
        final RectF dst = mDst;
        if (dst.isEmpty()) {
            return;
        }

        final float minX, maxX;
        final float minY, maxY;

        minX = (dst.right > wWidth) ? (wWidth - dst.right) : 0;
        maxX = dst.left < 0 ? -dst.left : 0;
        minY = (dst.bottom > wHeight) ? (wHeight - dst.bottom) : 0;
        maxY = dst.top < 0 ? -dst.top : 0;

        if (mImageFling == null) {
            mImageFling = new ImageFling(this);
        }
        mImageFling.startFling(velocityX, minX, maxX, velocityY, minY, maxY);
    }

    // Return true if image is shown
    private boolean isReady() {
        return !mDst.isEmpty();
    }

    /**
     * Set the {@code ImageSource} to show.
     */
    public void setImage(ImageSource image) {
        if (mImage != null) {
            cancelAllAnimator();
            mImage.setCallback(null);
            unscheduleImage(mImage);
            if (ViewCompat.isAttachedToWindow(this)) {
                mImage.setVisible(false);
            }
            mImage.recycle();
            mDst.setEmpty();
            mSrcActual.setEmpty();
            mDstActual.setEmpty();
        }

        final int oldIWidth = mImageWidth;
        final int oldIHeight = mImageHeight;

        mImage = image;

        if (image != null) {
            image.setCallback(this);
            if (ViewCompat.isAttachedToWindow(this)) {
                image.setVisible(getVisibility() == VISIBLE);
            }
            image.setWindowSize(mWindowWidth, mWindowHeight);
            if (mMaxBitmapSize != 0) {
                image.setMaxBitmapSize(mMaxBitmapSize);
            }
            if (image.isReady()) {
                onImageReady(image);
                // Let onImageReady handle next process
                return;
            } else {
                image.init();
            }
        }

        // ImageSource is null or not ready
        mImageWidth = 0;
        mImageHeight = 0;
        if (oldIWidth != mImageWidth || oldIHeight != mImageHeight) {
            requestLayout();
        }
        invalidate();
    }

    /**
     * Return {@code ImageSource}.
     */
    public ImageSource getImage() {
        return mImage;
    }

    @Override
    public void onImageReady(@NonNull ImageSource who) {
        if (who != mImage) {
            return;
        }

        final int oldIWidth = mImageWidth;
        final int oldIHeight = mImageHeight;

        mImageWidth = who.getWidth();
        mImageHeight = who.getHeight();

        updateScale();
        initPosition();

        if (oldIWidth != mImageWidth || oldIHeight != mImageHeight) {
            requestLayout();
        }
        invalidate();

        if (mImageInitListener != null) {
            mImageInitListener.onImageInitSuccessful();
        }
    }

    @Override
    public void onImageFailed(@NonNull ImageSource who) {
        if (who != mImage) {
            return;
        }

        if (mImageInitListener != null) {
            mImageInitListener.onImageInitFailed();
        }
    }

    @Override
    public void invalidateImage(@NonNull ImageSource who) {
        if (who == mImage) {
            invalidate();
        }
    }

    private void ensureDumpDrawable() {
        if (mDumpDrawable == null) {
            mDumpDrawable = new DumpDrawable();
            mDumpDrawable.setCallback(this);
        }
    }

    @Override
    public void scheduleImage(@NonNull ImageSource who, @NonNull Runnable what, long when) {
        if (who == mImage) {
            // Use dump drawable to call scheduleDrawable
            ensureDumpDrawable();
            scheduleDrawable(mDumpDrawable, what, when);
        }
    }

    @Override
    public void unscheduleImage(@NonNull ImageSource who, @NonNull Runnable what) {
        if (who == mImage) {
            // Use dump drawable to call unscheduleDrawable
            ensureDumpDrawable();
            unscheduleDrawable(mDumpDrawable, what);
        }
    }

    public void unscheduleImage(@NonNull ImageSource who) {
        if (who == mImage) {
            // Use dump drawable to call unscheduleDrawable
            ensureDumpDrawable();
            unscheduleDrawable(mDumpDrawable);
        }
    }

    private void applyRectInWindow() {
        final RectF dst = mDst;
        final RectF dstActual = mDstActual;
        final RectF srcActual = mSrcActual;

        dstActual.set(dst);
        if (dstActual.intersect(0, 0, mWindowWidth, mWindowHeight)) {
            if (dst.equals(dstActual)) {
                // Still dst
                srcActual.set(0, 0, mImageWidth, mImageHeight);
            } else {
                srcActual.left = lerp(0.0f, mImageWidth,
                        norm(dst.left, dst.right, dstActual.left));
                srcActual.right = lerp(0.0f, mImageWidth,
                        norm(dst.left, dst.right, dstActual.right));
                srcActual.top = lerp(0.0f, mImageHeight,
                        norm(dst.top, dst.bottom, dstActual.top));
                srcActual.bottom = lerp(0.0f, mImageHeight,
                        norm(dst.top, dst.bottom, dstActual.bottom));
            }
        } else {
            // Can't be seen, set src and dst empty
            srcActual.setEmpty();
            dstActual.setEmpty();
        }

        mRectDirty = false;
    }

    private int getMaxBitmapSize(Canvas canvas) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            int maxSize = Math.min(canvas.getMaximumBitmapWidth(), canvas.getMaximumBitmapHeight());
            // If hardware acceleration is not enabled,
            // getMaximumBitmapWidth() and getMaximumBitmapHeight()
            // will return Integer.MAX_VALUE.
            // In that case, use 2048 as default.
            if (maxSize == Integer.MAX_VALUE) {
                maxSize = 2048;
            }
            return maxSize;
        } else {
            // Before ICE_CREAM_SANDWICH, hardware acceleration is not supported,
            // bitmap max size is not limited.
            // Use 2048 as default.
            return 2048;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Set max bitmap size
        if (mMaxBitmapSize == 0) {
            mMaxBitmapSize = getMaxBitmapSize(canvas);
            if (mImage != null) {
                mImage.setMaxBitmapSize(mMaxBitmapSize);
            }
        }

        if (mImage == null || mDst.isEmpty()) {
            return;
        }
        if (mRectDirty) {
            applyRectInWindow();
        }
        if (!mSrcActual.isEmpty()) {
            final int saved = transformCanvas(canvas);
            mImage.draw(canvas, mSrcActual, mDstActual);
            if (saved != 0) {
                canvas.restoreToCount(saved);
            }
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mImage != null) {
            mImage.setVisible(ViewCompat.isAttachedToWindow(this) && visibility == VISIBLE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mImage != null) {
            mImage.setVisible(getVisibility() == VISIBLE);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mImage != null) {
            mImage.setVisible(false);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Always call parent.requestDisallowInterceptTouchEvent(true)
        // When get edge, translate() will call parent.requestDisallowInterceptTouchEvent(false)
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }

        mGestureRecognizer.onTouchEvent(event);
        return true;
    }

    @Override
    public void onDown() {
        cancelAllAnimator();

        if (mImage != null) {
            mImage.onTouchStart();
        }
    }

    @Override
    public void onUp() {
        if (mImage != null) {
            mImage.onTouchEnd();
        }
    }

    @Override
    public void onCancel() {
        if (mImage != null) {
            mImage.onTouchEnd();
        }
    }

    @Override
    public void onSingleTap(float x, float y) {}

    @Override
    public void onDoubleTap(float x, float y) {
        mTempPointF.set(x, y);
        transformPoint(mTempPointF);
        scaleToNextLevel(mTempPointF.x, mTempPointF.y);
    }

    @Override
    public void onLongPress(float x, float y) {}

    @Override
    public void onScroll(float x, float y, float dx, float dy, float totalX, float totalY) {
        mTempPointF.set(dx, dy);
        transformDistance(mTempPointF);
        translate(mTempPointF.x, mTempPointF.y);
    }

    @Override
    public void onFling(float x, float y, float velocityX, float velocityY) {
        mTempPointF.set(velocityX, velocityY);
        transformDistance(mTempPointF);
        fling(mTempPointF.x, mTempPointF.y);
    }

    @Override
    public void onScale(float x, float y, float scale) {
        mTempPointF.set(x, y);
        transformPoint(mTempPointF);
        scale(mTempPointF.x, mTempPointF.y, scale);
    }

    public void onAnimatorStart() {
        ++mAnimating;

        if (mImage != null) {
            mImage.onAnimatorStart();
        }
    }

    public void onAnimatorEnd() {
        --mAnimating;

        if (mImage != null) {
            mImage.onAnimatorEnd();
        }
    }


    ////////////////////
    // Animator
    ////////////////////

    private abstract static class BaseAnimator extends ValueAnimator
            implements Animator.AnimatorListener,
            ValueAnimator.AnimatorUpdateListener {

        private final LargeImageView mView;

        public BaseAnimator(LargeImageView view) {
            mView = view;
            addListener(this);
            addUpdateListener(this);
        }

        @Override
        public void onAnimationStart(Animator animator) {
            mView.onAnimatorStart();
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            mView.onAnimatorEnd();
        }

        @Override
        public void onAnimationCancel(Animator animator) {}

        @Override
        public void onAnimationRepeat(Animator animation) {}
    }

    private static class SmoothScaler extends BaseAnimator {

        private final LargeImageView mView;
        private float mX;
        private float mY;
        private float mStartScale;
        private float mEndScale;

        public SmoothScaler(LargeImageView view) {
            super(view);
            mView = view;
            setDuration(300);
            setInterpolator(FAST_SLOW_INTERPOLATOR);
            setFloatValues(0.0f, 1.0f);
        }

        public void startSmoothScaler(float x, float y, float scale) {
            if (!mView.isReady()) {
                return;
            }
            mView.cancelAllAnimator();
            mX = x;
            mY = y;
            mStartScale = mView.mScale;
            mEndScale = scale;
            start();
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
            final float value = (Float) getAnimatedValue();
            mView.setScale(mX, mY, lerp(mStartScale, mEndScale, value));
        }
    }

    private static class ImageFling extends BaseAnimator {

        private final LargeImageView mView;
        private final Fling mFling;

        private float mDx;
        private float mDy;
        private float mLastX;
        private float mLastY;

        public ImageFling(LargeImageView view) {
            super(view);
            mView = view;
            mFling = new Fling(view.getContext());
            setInterpolator(Fling.FLING_INTERPOLATOR);
            setFloatValues(0.0f, 1.0f);
        }

        public void startFling(float velocityX, float minX, float maxX,
                float velocityY, float minY, float maxY) {
            final Fling fling = mFling;
            mDx = (float) (fling.getSplineFlingDistance(velocityX) * Math.signum(velocityX));
            mDy = (float) (fling.getSplineFlingDistance(velocityY) * Math.signum(velocityY));
            mLastX = 0;
            mLastY = 0;
            int durationX = fling.getSplineFlingDuration(velocityX);
            int durationY = fling.getSplineFlingDuration(velocityY);

            if (mDx < minX) {
                durationX = fling.adjustDuration(0, mDx, minX, durationX);
                mDx = minX;
            }
            if (mDx > maxX) {
                durationX = fling.adjustDuration(0, mDx, maxX, durationX);
                mDx = maxX;
            }
            if (mDy < minY) {
                durationY = fling.adjustDuration(0, mDy, minY, durationY);
                mDy = minY;
            }
            if (mDy > maxY) {
                durationY = fling.adjustDuration(0, mDy, maxY, durationY);
                mDy = maxY;
            }

            if (mDx == 0 && mDy == 0) {
                return;
            }

            setDuration(Math.max(durationX, durationY));
            start();
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
            final float value = (Float) getAnimatedValue();
            final float x = mDx * value;
            final float y = mDy * value;
            final float offsetX = x - mLastX;
            final float offsetY = y - mLastY;
            mView.translate(-offsetX, -offsetY);
            mLastX = x;
            mLastY = y;
        }
    }


    ////////////////////
    // Transform for orientation
    ////////////////////

    private int transformCanvas(Canvas canvas) {
        final int saved;
        switch (mOrientation) {
            case ORIENTATION_0:
                saved = 0;
                break;
            case ORIENTATION_90:
                saved = canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.translate(getWidth(), 0);
                canvas.rotate(90);
                break;
            case ORIENTATION_180:
                saved = canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.translate(getWidth(), getHeight());
                canvas.rotate(180);
                break;
            case ORIENTATION_270:
                saved = canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.translate(0, getHeight());
                canvas.rotate(-90);
                break;
            default:
                throw new IllegalStateException("Unknown orientation: " + mOrientation);
        }
        return saved;
    }

    private void transformPoint(PointF p) {
        switch (mOrientation) {
            case ORIENTATION_0:
                break;
            case ORIENTATION_90:
                p.set(p.y, getWidth() - p.x);
                break;
            case ORIENTATION_180:
                p.set(getWidth() - p.x, getHeight() - p.y);
                break;
            case ORIENTATION_270:
                p.set(getHeight() - p.y, p.x);
                break;
            default:
                throw new IllegalStateException("Unknown orientation: " + mOrientation);
        }
    }

    private void transformDistance(PointF p) {
        switch (mOrientation) {
            case ORIENTATION_0:
                break;
            case ORIENTATION_90:
                p.set(p.y, -p.x);
                break;
            case ORIENTATION_180:
                p.set(-p.x, -p.y);
                break;
            case ORIENTATION_270:
                p.set(-p.y, p.x);
                break;
            default:
                throw new IllegalStateException("Unknown orientation: " + mOrientation);
        }
    }


    ////////////////////
    // Math function
    ////////////////////

    @CheckResult
    private static float lerp(float start, float stop, float amount) {
        return start + (stop - start) * amount;
    }

    @CheckResult
    private static float norm(float start, float stop, float value) {
        if (stop == start) {
            if (stop == value) {
                return 1.0f;
            } else {
                return Float.NaN;
            }
        } else {
            return (value - start) / (stop - start);
        }
    }

    @CheckResult
    private static float clamp(float x, float min, float max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    // The dump drawable to call scheduleDrawable and unscheduleDrawable.
    private class DumpDrawable extends Drawable {
        @Override
        public void draw(@NonNull Canvas canvas) {}
        @Override
        public void setAlpha(int alpha) {}
        @Override
        public void setColorFilter(ColorFilter colorFilter) {}
        @Override
        public int getOpacity() { return PixelFormat.OPAQUE; }
    }

    /**
     * Listener for init {@code ImageSource}.
     */
    public interface ImageInitListener {

        void onImageInitSuccessful();

        void onImageInitFailed();
    }
}

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

import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * This class just like {@link android.graphics.drawable.Drawable}.
 * {@code LargeImageView} use it to render.
 */
public abstract class ImageSource {

    private WeakReference<ImageSource.Callback> mCallback;

    /**
     * Init this ImageSource. If init action takes a long time,
     * it should be done in background thread.
     *
     * When init action is done, call {@link #callSelfReady()}
     * or {@link #callSelfFailed()} in UI thread.
     *
     * @see #callSelfReady()
     * @see #callSelfFailed()
     */
    public abstract void init();

    /**
     * Return {@code true} if this {@code ImageSource} is ready.
     * If it return {@code false}, {@link #init()} will be called soon.
     *
     * @see #init()
     */
    public abstract boolean isReady();

    /**
     * Set callback for this {@code ImageSource}.
     * The callback will be referenced as {@code WeakReference}.
     *
     * Usually it is done by {@code LargeImageView}.
     */
    public final void setCallback(@Nullable ImageSource.Callback cb) {
        mCallback = cb != null ? new WeakReference<>(cb) : null;
    }

    /**
     * Return the callback of this {@code ImageSource}.
     */
    public ImageSource.Callback getCallback() {
        return mCallback != null ? mCallback.get() : null;
    }

    /**
     * Call {@code onImageReady} of its callback.
     */
    public void callSelfReady() {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.onImageReady(this);
        }
    }

    /**
     * Call {@code onImageFailed} of its callback.
     */
    public void callSelfFailed() {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.onImageFailed(this);
        }
    }

    /**
     * Call {@code invalidateImage} of its callback.
     */
    public void invalidateSelf() {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateImage(this);
        }
    }

    /**
     * Call {@code scheduleImage} of its callback.
     */
    public void scheduleSelf(@NonNull Runnable what, long when) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.scheduleImage(this, what, when);
        }
    }

    /**
     * Call {@code unscheduleImage} of its callback.
     */
    public void unscheduleSelf(@NonNull Runnable what) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.unscheduleImage(this, what);
        }
    }

    /**
     * A window is the rect the {@code ImageSource}
     * to show in {@code Canvas}.
     */
    public abstract void setWindowSize(int w, int h);

    /**
     * From {@link Canvas#getMaximumBitmapWidth()}
     * and {@link Canvas#getMaximumBitmapHeight()}.
     */
    public abstract void setMaxBitmapSize(int maxSize);

    /**
     * Indicate a animator on this {@code ImageSource} start.
     */
    public abstract void onAnimatorStart();

    /**
     * Indicate a animator on this {@code ImageSource} end.
     */
    public abstract void onAnimatorEnd();

    /**
     * Return the width of this {@code ImageSource}.
     * If not ready, do what you wanna do.
     */
    public abstract int getWidth();

    /**
     * Return the height of this {@code ImageSource}.
     * If not ready, do what you wanna do.
     */
    public abstract int getHeight();

    /**
     * Render this {@code ImageSource}.
     * If not ready, do what you wanna do.
     */
    public abstract void draw(Canvas canvas, RectF src, RectF dst);

    /**
     * Recycle this {@code ImageSource}.
     */
    public abstract void recycle();

    /**
     * Just like {@link android.graphics.drawable.Drawable.Callback}.
     */
    interface Callback {

        /**
         * Indicate this {@code ImageSource} init done.
         */
        void onImageReady(@NonNull ImageSource who);

        /**
         * Indicate this {@code ImageSource} init failed.
         */
        void onImageFailed(@NonNull ImageSource who);

        /**
         * Request for rendering this {@code ImageSource}.
         */
        void invalidateImage(@NonNull ImageSource who);

        /**
         * Request for posting the {@code Runnable}.
         */
        void scheduleImage(@NonNull ImageSource who, @NonNull Runnable what, long when);

        /**
         * Request for canceling posting the {@code Runnable}.
         */
        void unscheduleImage(@NonNull ImageSource who, @NonNull Runnable what);
    }
}

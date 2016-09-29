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
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;

public abstract class ImageSource {

    private WeakReference<ImageSource.Callback> mCallback;

    public abstract void init();

    public abstract boolean isReady();

    public final void setCallback(@Nullable ImageSource.Callback cb) {
        mCallback = cb != null ? new WeakReference<>(cb) : null;
    }

    public ImageSource.Callback getCallback() {
        return mCallback != null ? mCallback.get() : null;
    }

    public void invalidateSelf() {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateImage(this);
        }
    }

    public void scheduleSelf(@NonNull Runnable what, long when) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.scheduleImage(this, what, when);
        }
    }

    public void unscheduleSelf(@NonNull Runnable what) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.unscheduleImage(this, what);
        }
    }

    public abstract void setWindowSize(int w, int h);

    public abstract void setMaxBitmapSize(int maxSize);

    public abstract void onAnimatorStart();

    public abstract void onAnimatorEnd();

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract void draw(Canvas canvas, Rect src, RectF dst);

    public abstract void recycle();

    interface Callback {

        void onImageReady(@NonNull ImageSource who);

        void invalidateImage(@NonNull ImageSource who);

        void scheduleImage(@NonNull ImageSource who, @NonNull Runnable what, long when);

        void unscheduleImage(@NonNull ImageSource who, @NonNull Runnable what);
    }
}

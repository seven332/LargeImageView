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
 * Created by Hippo on 9/28/2016.
 */

import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.annotation.NonNull;

public abstract class WrapperSource extends ImageSource implements ImageSource.Callback{

    private ImageSource mBase;
    private int mWindowWidth;
    private int mWindowHeight;
    private int mMaxBitmapSize;

    /**
     * Set base ImageSource.
     * Only call it once!
     */
    protected void setImageSource(@NonNull ImageSource base) {
        if (mBase != null) {
            throw new IllegalStateException("Can't set ImageSource twice");
        }
        mBase = base;
        base.setCallback(this);
        base.setWindowSize(mWindowWidth, mWindowHeight);
        base.setMaxBitmapSize(mMaxBitmapSize);
        // Ready
        callSelfReady();
    }

    @Override
    public void init() {
        onInit();
    }

    /**
     * Start point to init base ImageSource.
     */
    protected abstract void onInit();

    @Override
    public boolean isReady() {
        return mBase != null;
    }

    @Override
    public void setWindowSize(int w, int h) {
        if (mBase != null) {
            mBase.setWindowSize(w, h);
        } else {
            mWindowWidth = w;
            mWindowHeight = h;
        }
    }

    @Override
    public void setMaxBitmapSize(int maxSize) {
        if (mBase != null) {
            mBase.setMaxBitmapSize(maxSize);
        } else {
            mMaxBitmapSize = maxSize;
        }
    }

    @Override
    public void onAnimatorStart() {
        if (mBase != null) {
            mBase.onAnimatorStart();
        }
    }

    @Override
    public void onAnimatorEnd() {
        if (mBase != null) {
            mBase.onAnimatorEnd();
        }
    }

    @Override
    public int getWidth() {
        if (mBase != null) {
            return mBase.getWidth();
        } else {
            return 0;
        }
    }

    @Override
    public int getHeight() {
        if (mBase != null) {
            return mBase.getHeight();
        } else {
            return 0;
        }
    }

    @Override
    public void draw(Canvas canvas, RectF src, RectF dst) {
        if (mBase != null) {
            mBase.draw(canvas, src, dst);
        }
    }

    @Override
    public void recycle() {
        if (mBase != null) {
            mBase.recycle();
        }
    }


    ////////////////////
    // Callback for base image source
    ////////////////////

    @Override
    public void onImageReady(@NonNull ImageSource who) {
        callSelfReady();
    }

    @Override
    public void onImageFailed(@NonNull ImageSource who) {
        callSelfFailed();
    }

    @Override
    public void invalidateImage(@NonNull ImageSource who) {
        invalidateSelf();
    }

    @Override
    public void scheduleImage(@NonNull ImageSource who, @NonNull Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleImage(@NonNull ImageSource who, @NonNull Runnable what) {
        unscheduleSelf(what);
    }
}

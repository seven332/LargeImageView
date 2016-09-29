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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;

public class BitmapSource extends ImageSource {

    private Bitmap mBitmap;
    private Paint mPaint;
    private final boolean mIsOwner;

    public BitmapSource(@NonNull Bitmap bitmap) {
        this(bitmap, false);
    }

    public BitmapSource(@NonNull Bitmap bitmap, boolean isOwner) {
        mBitmap = bitmap;
        mIsOwner = isOwner;
        mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    }

    @Override
    public void init() {}

    @Override
    public boolean isReady() {
        // Always ready
        return true;
    }

    @Override
    public void setWindowSize(int w, int h) {}

    @Override
    public void setMaxBitmapSize(int maxSize) {}

    @Override
    public void onAnimatorStart() {}

    @Override
    public void onAnimatorEnd() {}

    @Override
    public int getWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int getHeight() {
        return mBitmap.getHeight();
    }

    @Override
    public void draw(Canvas canvas, Rect src, RectF dst) {
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, src, dst, mPaint);
        }
    }

    @Override
    public void recycle() {
        if (mBitmap != null) {
            if (mIsOwner) {
                mBitmap.recycle();
            }
            mBitmap = null;
            mPaint = null;
        }
    }
}

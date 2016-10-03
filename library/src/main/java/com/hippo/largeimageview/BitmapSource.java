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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;

/**
 * A ImageSource to show single {@link Bitmap}.
 */
public class BitmapSource extends ImageSource {

    private Bitmap mBitmap;
    private Paint mPaint;
    private Matrix mMatrix;
    private final boolean mIsOwner;

    public BitmapSource(@NonNull Bitmap bitmap) {
        this(bitmap, false);
    }

    public BitmapSource(@NonNull Bitmap bitmap, boolean isOwner) {
        mBitmap = bitmap;
        mIsOwner = isOwner;
        mMatrix = new Matrix();
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
    public int getWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int getHeight() {
        return mBitmap.getHeight();
    }

    @Override
    public void draw(Canvas canvas, RectF src, RectF dst) {
        if (mBitmap != null) {
            mMatrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
            canvas.drawBitmap(mBitmap, mMatrix, mPaint);
        }
    }

    @Override
    public void recycle() {
        if (mBitmap != null) {
            if (mIsOwner) {
                mBitmap.recycle();
            }
            mBitmap = null;
            mMatrix = null;
            mPaint = null;
        }
    }
}

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
 * Created by Hippo on 9/27/2016.
 */

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

class GestureRecognizer implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener{

    public interface Listener {
        void onDown();
        void onUp();
        void onCancel();
        void onSingleTap(float x, float y);
        void onDoubleTap(float x, float y);
        void onLongPress(float x, float y);
        void onScroll(float x, float y, float dx, float dy, float totalX, float totalY);
        void onFling(float x, float y, float velocityX, float velocityY);
        void onScale(float x, float y, float scale);
    }

    private final GestureDetectorCompat mGestureDetector;
    private final ScaleGestureDetector mScaleDetector;
    private final Listener mListener;

    private boolean mScale;

    public GestureRecognizer(Context context, Listener listener) {
        mListener = listener;
        final Handler handler = new Handler(Looper.getMainLooper());
        mGestureDetector = new GestureDetectorCompat(context, this, handler);
        mGestureDetector.setOnDoubleTapListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mScaleDetector = new ScaleGestureDetector(context, this, handler);
        } else {
            mScaleDetector = new ScaleGestureDetector(context, this);
        }
    }

    public void onTouchEvent(MotionEvent event) {
        // If pointer count is more than 1, must be scale action
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                mListener.onUp();
                break;
            case MotionEvent.ACTION_CANCEL:
                mListener.onCancel();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mScale = event.getPointerCount() > 1;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mScale = (event.getPointerCount() - 1) > 1;
                break;
        }

        mGestureDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        mListener.onSingleTap(e.getX(), e.getY());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) { return true; }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            mListener.onDoubleTap(e.getX(), e.getY());
        }
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mScale = false;
        mListener.onDown();
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapUp(MotionEvent e) { return true; }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (!mScale) {
            mListener.onScroll(e2.getX(), e2.getY(), distanceX, distanceY,
                    e2.getX() - e1.getX(), e2.getY() - e1.getY());
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (!mScale) {
            mListener.onLongPress(e.getX(), e.getY());
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (!mScale) {
            mListener.onFling(e2.getX(), e2.getY(), velocityX, velocityY);
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mScale = true;
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        mScale = true;
        mListener.onScale(detector.getFocusX(),
                detector.getFocusY(), detector.getScaleFactor());
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {}
}

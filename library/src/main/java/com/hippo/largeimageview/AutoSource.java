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
 * Created by Hippo on 9/30/2016.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.hippo.streampipe.InputStreamPipe;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Choice {@link BitmapSource} or {@link TiledBitmapSource} automatically.
 */
public class AutoSource extends WrapperSource {

    private static final String LOG_TAG = AutoSource.class.getSimpleName();

    private boolean mInit;
    private int mMaxBitmapSize;
    private int mBitmapLimit;
    private InitTask mTask;
    private InputStreamPipe mPipe;

    public AutoSource(@NonNull InputStreamPipe pipe) {
        mPipe = pipe;
    }

    @Override
    protected void onInit() {
        mInit = true;
        initSource();
    }

    @Override
    public void setMaxBitmapSize(int maxSize) {
        super.setMaxBitmapSize(maxSize);
        mMaxBitmapSize = maxSize;
        mBitmapLimit = maxSize / 2;
        initSource();
    }

    protected int getMaxBitmapSize() {
        return mMaxBitmapSize;
    }

    protected int getBitmapLimit() {
        return mBitmapLimit;
    }

    protected InputStreamPipe getInputStreamPipe() {
        return mPipe;
    }

    protected void clearInputStreamPipe() {
        mPipe = null;
    }

    private void initSource() {
        if (!mInit || mBitmapLimit == 0 || mTask != null || isReady()) {
            return;
        }
        mTask = new InitTask(this);
        mTask.execute();
    }

    private void onInitDone(ImageSource imageSource) {
        mTask = null;
        if (imageSource != null) {
            setImageSource(imageSource);
        } else {
            callSelfFailed();
        }
    }

    @Override
    public void recycle() {
        super.recycle();
        if (mTask != null) {
            mTask.cancel(false);
            mTask = null;
        }
        mPipe = null;
    }

    /**
     * Decode InputStreamPipe to ImageSource.
     * Called in non-UI thread.
     */
    protected ImageSource decode() {
        final InputStreamPipe pipe = mPipe;
        if (pipe == null) {
            return null;
        }

        try {
            pipe.obtain();

            // Decode image info
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(pipe.open(), null, options);
            pipe.close();
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                // It is not a image
                return null;
            }

            if (options.outWidth <= mBitmapLimit && options.outHeight <= mBitmapLimit) {
                // BitmapSource
                options.inJustDecodeBounds = false;
                try {
                    final Bitmap bitmap = BitmapFactory.decodeStream(pipe.open(), null, options);
                    return new BitmapSource(bitmap);
                } catch (OutOfMemoryError e) {
                    return null;
                }
            } else {
                // TiledBitmapSource
                final BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(pipe.open(), false);
                if (decoder != null) {
                    return new TiledBitmapSource(new SkiaRegionDecoder(decoder, Bitmap.Config.ARGB_8888));
                } else {
                    // Can't decode
                    return null;
                }
            }
        } catch (IOException e) {
            return null;
        } finally {
            pipe.close();
            pipe.release();
            mPipe = null;
        }
    }

    private static class InitTask extends AsyncTask<Void, Void, ImageSource> {

        private final WeakReference<AutoSource> mAutoSource;

        public InitTask(AutoSource autoSource) {
            mAutoSource = new WeakReference<>(autoSource);
        }

        @Override
        protected ImageSource doInBackground(Void... params) {
            final AutoSource autoSource = mAutoSource.get();
            if (autoSource != null) {
                return autoSource.decode();
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(ImageSource imageSource) {
            final AutoSource autoSource = mAutoSource.get();
            if (autoSource == null) {
                Log.w(LOG_TAG, "Should call cancel() on InitTask");
                if (imageSource != null) {
                    imageSource.recycle();
                }
            } else {
                autoSource.onInitDone(imageSource);
            }
        }

        @Override
        protected void onCancelled(ImageSource imageSource) {
            if (imageSource != null) {
                imageSource.recycle();
            }
        }
    }
}

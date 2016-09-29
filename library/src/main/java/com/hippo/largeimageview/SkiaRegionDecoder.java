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
 * Created by Hippo on 9/29/2016.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;

public class SkiaRegionDecoder extends RegionDecoder {

    private BitmapRegionDecoder mDecoder;
    private final Bitmap.Config mConfig;

    public SkiaRegionDecoder(BitmapRegionDecoder decoder, Bitmap.Config config) {
        mDecoder = decoder;
        mConfig = config;
    }

    @Override
    public int getWidth() {
        return mDecoder.getWidth();
    }

    @Override
    public int getHeight() {
        return mDecoder.getHeight();
    }

    @Override
    protected Bitmap decodeRegionInternal(Rect rect, int sample) {
        final BitmapRegionDecoder decoder = mDecoder;
        if (decoder != null) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            options.inPreferredConfig = mConfig;
            return decoder.decodeRegion(rect, options);
        } else {
            return null;
        }
    }

    @Override
    public void recycle() {
        if (mDecoder != null) {
            mDecoder.recycle();
            mDecoder = null;
        }
    }
}

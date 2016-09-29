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
import android.graphics.Rect;
import android.graphics.RectF;

public abstract class WrapperSource extends ImageSource {

    private ImageSource mBase;

    @Override
    public boolean isReady() {
        return mBase != null;
    }

    @Override
    public void setWindowSize(int w, int h) {

    }

    @Override
    public void setMaxBitmapSize(int maxSize) {

    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(Canvas canvas, Rect src, RectF dst) {

    }

    @Override
    public void recycle() {

    }
}

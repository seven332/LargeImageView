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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TiledBitmapSource extends ImageSource {

    private static final String LOG_TAG = TiledBitmapSource.class.getSimpleName();

    private static class Tile {
        public Rect rect;
        public Bitmap bitmap;
        public boolean loading;
        public boolean visible;
    }

    private BitmapRegionDecoder mDecoder;
    // The width of parent view
    private int mWindowWidth;
    // The height of parent view
    private int mWindowHeight;
    private int mMaxTileSize;
    private boolean mAnimating;

    private Paint mPaint;

    private int mCurrentSample;
    private int mFullSample;
    // StrongTile
    private List<Tile> mFullTiles;
    // WeakTile
    private final SparseArray<List<Tile>> mTilesMap = new SparseArray<>();

    private FullTileTask mFullTileTask;
    private final List<LoadTileTask> mLoadTileTaskList = new ArrayList<>();

    private final Rect mTempRect = new Rect();
    private final Rect mTempRect2 = new Rect();
    private final RectF mTempRectF = new RectF();
    private final RectF mTempRectF2 = new RectF();
    private final List<Tile> mTempTileList = new ArrayList<>();

    public TiledBitmapSource(BitmapRegionDecoder decoder) {
        mDecoder = decoder;
        mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    }

    @Override
    public void init() {}

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWindowSize(int w, int h) {
        mWindowWidth = w;
        mWindowHeight = h;
        ensureFullTiles();
    }

    @Override
    public void setMaxBitmapSize(int maxSize) {
        // I think MaxBitmapSize / 4 is suitable for max tile size
        mMaxTileSize = maxSize / 4;
        ensureFullTiles();
    }

    @Override
    public void onAnimatorStart() {
        mAnimating = true;
    }

    @Override
    public void onAnimatorEnd() {
        mAnimating = false;
        // Trigger loading missing tiles
        invalidateSelf();
    }

    @Override
    public int getWidth() {
        return mDecoder.getWidth();
    }

    @Override
    public int getHeight() {
        return mDecoder.getHeight();
    }

    private static int calculateSample(int scaleX, int scaleY) {
        int sample = Math.max(scaleX, scaleY);
        sample = Math.max(1, sample);
        return prevPow2(sample);
    }

    private static void recycleTiles(List<Tile> tiles) {
        for (Tile tile : tiles) {
            final Bitmap bitmap = tile.bitmap;
            if (bitmap != null) {
                bitmap.recycle();
                tile.bitmap = null;
            }
        }
    }

    private void ensureFullTiles() {
        if (mWindowWidth == 0 || mWindowHeight == 0 || mMaxTileSize == 0) {
            return;
        }
        // Get full sample
        final int fullSample = calculateSample(mDecoder.getWidth() / mWindowWidth,
                mDecoder.getHeight() / mWindowHeight);
        if (mFullSample == fullSample) {
            // full sample is still the same
            return;
        }

        mFullSample = fullSample;

        // Recycle all tiles
        if (mFullTiles != null) {
            recycleTiles(mFullTiles);
            mFullTiles = null;
        }
        for (int i = 0, len = mTilesMap.size(); i < len; i++) {
            recycleTiles(mTilesMap.valueAt(i));
        }
        mTilesMap.clear();

        // Cancel all tasks
        if (mFullTileTask != null) {
            mFullTileTask.cancel(false);
            mFullTileTask = null;
        }
        for (LoadTileTask task : mLoadTileTaskList) {
            task.cancel(false);
        }
        mLoadTileTaskList.clear();

        mFullTileTask = new FullTileTask(this);
        mFullTileTask.execute();
    }

    private void onFullTileDone(List<Tile> tiles) {
        mFullTileTask = null;
        mFullTiles = tiles;
        invalidateSelf();
    }

    private void onLoadTileDone(LoadTileTask task, int sample) {
        mLoadTileTaskList.remove(task);
        if (sample == mCurrentSample) {
            invalidateSelf();
        }
    }

    private static void mapRect(Rect src, RectF dst, Rect s, RectF d) {
        final float sX = src.left;
        final float sY = src.top;
        final float dX = dst.left;
        final float dY = dst.top;
        final float scaleX = dst.width() / src.width();
        final float scaleY = dst.height() / src.height();
        d.set(dX + (s.left - sX) * scaleX,
                dY + (s.top - sY) * scaleY,
                dX + (s.right - sX) * scaleX,
                dY + (s.bottom - sY) * scaleY);
    }

    private static void mapRect(Rect rect, int sample) {
        rect.left /= sample;
        rect.top /= sample;
        rect.right /= sample;
        rect.bottom /= sample;
    }

    // For full tiles
    private void drawFullTiles(Canvas canvas, Rect src, RectF dst, List<Tile> tiles, int sample) {
        final Rect s = mTempRect;
        final RectF d = mTempRectF;
        final Paint paint = mPaint;

        for (final Tile t : tiles) {
            s.set(t.rect);
            if (!s.intersect(src)) { continue; }
            final Bitmap bitmap = t.bitmap;
            if (bitmap == null) { continue; }
            mapRect(src, dst, s, d);
            s.offset(-t.rect.left, -t.rect.top);
            mapRect(s, sample);
            canvas.drawBitmap(bitmap, s, d, paint);
        }
    }

    // For not full tiles
    private void drawMapTiles(Canvas canvas, Rect src, RectF dst, List<Tile> tiles, int sample) {
        final List<Tile> list = mTempTileList;
        final Rect s = mTempRect;
        final Rect src2 = mTempRect2;

        boolean firstMiss = true;
        // First get missing tile
        for (Tile t : tiles) {
            s.set(t.rect);
            if (s.intersect(src)) {
                t.visible = true;
                // Check bitmap in this tile
                if (t.bitmap == null) {
                    // Missing bitmap, add to miss rect
                    if (firstMiss) {
                        firstMiss = false;
                        src2.set(s);
                    } else {
                        src2.union(s);
                    }

                    if (!t.loading && !mAnimating) {
                        // It is not animating now and
                        // the tile is not loading, start load tile task
                        final LoadTileTask task = new LoadTileTask(this, t, sample);
                        mLoadTileTaskList.add(task);
                        task.execute();
                    }
                } else {
                    // Add this tile to render list
                    list.add(t);
                }
            } else {
                t.visible = false;
            }
        }

        final RectF d = mTempRectF;
        final Paint paint = mPaint;

        // Draw full tiles to fill missing rect
        // TODO The image flicker when scrolling if scale is large. How to fix it?
        if (!firstMiss) {
            final RectF dst2 = mTempRectF2;
            mapRect(src, dst, src2, dst2);
            for (final Tile t : mFullTiles) {
                s.set(t.rect);
                if (!s.intersect(src2)) { continue; }
                final Bitmap bitmap = t.bitmap;
                if (bitmap == null) { continue; }
                mapRect(src2, dst2, s, d);
                s.offset(-t.rect.left, -t.rect.top);
                mapRect(s, mFullSample);
                canvas.drawBitmap(bitmap, s, d, paint);
            }
        }

        // Draw tile in list
        for (final Tile t : list) {
            s.set(t.rect);
            if (!s.intersect(src)) { continue; }
            final Bitmap bitmap = t.bitmap;
            if (bitmap == null) { continue; }
            mapRect(src, dst, s, d);
            s.offset(-t.rect.left, -t.rect.top);
            mapRect(s, sample);
            canvas.drawBitmap(bitmap, s, d, paint);
        }
        list.clear();
    }

    // Gen tile list for the sample
    private List<Tile> genTileList(int sample) {
        final int width = mDecoder.getWidth();
        final int height = mDecoder.getHeight();
        final int step = mMaxTileSize * sample;
        final List<Tile> list = new ArrayList<>(ceilDiv(width, step) * ceilDiv(height, step));

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                final int w = Math.min(step, width - x);
                final int h = Math.min(step, height - y);
                final Rect rect = new Rect(x, y, x + w, y + h);
                final Tile tile = new Tile();
                tile.rect = rect;
                list.add(tile);
            }
        }

        return list;
    }

    private void gc() {
        for (int i = 0, len = mTilesMap.size(); i < len; i++) {
            final int sample = mTilesMap.keyAt(i);
            final List<Tile> list = mTilesMap.valueAt(i);
            if (sample != mCurrentSample) {
                // Recycle all tiles for non current sample
                recycleTiles(list);
            } else {
                // Only recycle invisible tile for current sample
                for (Tile tile : list) {
                    if (!tile.visible) {
                        final Bitmap bitmap = tile.bitmap;
                        if (bitmap != null) {
                            bitmap.recycle();
                            tile.bitmap = null;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas, Rect src, RectF dst) {
        if (mFullTiles == null) {
            // If mFullTiles is null, means first decode step
            // is not done. Wait for it.
            return;
        }

        int sample = calculateSample(src.width() / (int) dst.width(),
                src.height() / (int) dst.height());
        // Full sample must be the biggest sample
        sample = Math.min(mFullSample, sample);
        // Update current sample
        mCurrentSample = sample;

        if (sample == mFullSample) {
            drawFullTiles(canvas, src, dst, mFullTiles, mFullSample);
        } else {
            List<Tile> list = mTilesMap.get(sample);
            if (list == null) {
                list = genTileList(sample);
                mTilesMap.put(sample, list);
            }
            drawMapTiles(canvas, src, dst, list, sample);
        }

        // Always gc after draw tiles
        gc();
    }

    @Override
    public void recycle() {
        mPaint = null;

        // Recycle all tiles
        if (mFullTiles != null) {
            recycleTiles(mFullTiles);
            mFullTiles = null;
        }
        for (int i = 0, len = mTilesMap.size(); i < len; i++) {
            recycleTiles(mTilesMap.valueAt(i));
        }
        mTilesMap.clear();

        boolean recycled = false;

        // Cancel all tasks
        if (mFullTileTask != null) {
            recycled = true;
            mFullTileTask.recycle();
            mFullTileTask.cancel(false);
        }
        for (LoadTileTask task : mLoadTileTaskList) {
            task.recycle();
            task.cancel(false);
        }
        mLoadTileTaskList.clear();

        if (!recycled) {
            mDecoder.recycle();
        }
        mDecoder = null;
    }

    /**
     * The BaseTask for all the other tasks.
     * Handle mImageSource recycle.
     */
    private static abstract class BaseTask<Params, Progress, Result>
            extends AsyncTask<Params, Progress, Result> {

        protected BitmapRegionDecoder mDecoder;
        private boolean mRecycle;

        public BaseTask(BitmapRegionDecoder decoder) {
            mDecoder = decoder;
        }

        /**
         * Call it before {@link #cancel(boolean)} if you want to
         * recycle the ImageSource.
         */
        public void recycle() {
            mRecycle = true;
        }

        @Override
        protected void onCancelled(Result result) {
            if (mRecycle) {
                mDecoder.recycle();
            }
        }
    }

    private static class FullTileTask extends BaseTask<Void, Void, List<Tile>> {

        private final WeakReference<TiledBitmapSource> mSource;
        private final BitmapRegionDecoder mDecoder;
        private final int mFullSample;
        private final int mMaxTileSize;

        public FullTileTask(TiledBitmapSource source) {
            super(source.mDecoder);
            mSource = new WeakReference<>(source);
            mDecoder = source.mDecoder;
            mFullSample = source.mFullSample;
            mMaxTileSize = source.mMaxTileSize;
        }

        @Override
        protected List<Tile> doInBackground(Void... params) {
            final int width = mDecoder.getWidth();
            final int height = mDecoder.getHeight();
            final int step = mMaxTileSize * mFullSample;
            final List<Tile> list = new ArrayList<>(ceilDiv(width, step) * ceilDiv(height, step));

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = mFullSample;

            for (int y = 0; y < height; y += step) {
                for (int x = 0; x < width; x += step) {
                    if (isCancelled()) {
                        break;
                    }
                    final int w = Math.min(step, width - x);
                    final int h = Math.min(step, height - y);
                    final Rect rect = new Rect(x, y, x + w, y + h);
                    // TODO Catch OOM
                    final Bitmap bitmap = mDecoder.decodeRegion(rect, options);
                    final Tile tile = new Tile();
                    tile.rect = rect;
                    tile.bitmap = bitmap;
                    list.add(tile);
                }
            }

            return list;
        }

        @Override
        protected void onPostExecute(List<Tile> tiles) {
            final TiledBitmapSource source = mSource.get();
            if (source == null) {
                Log.w(LOG_TAG, "Should call cancel() on FullTileTask");
                recycleTiles(tiles);
            } else {
                // Callback
                source.onFullTileDone(tiles);
            }
        }

        @Override
        protected void onCancelled(List<Tile> tiles) {
            super.onCancelled(tiles);
            recycleTiles(tiles);
        }
    }

    private static class LoadTileTask extends BaseTask<Void, Void, Bitmap> {

        private final WeakReference<TiledBitmapSource> mSource;
        private final WeakReference<Tile> mTile;
        private final BitmapRegionDecoder mDecoder;
        private final int mSample;

        public LoadTileTask(TiledBitmapSource source, Tile tile, int sample) {
            super(source.mDecoder);
            mSource = new WeakReference<>(source);
            mTile = new WeakReference<>(tile);
            mDecoder = source.mDecoder;
            mSample = sample;
            tile.loading = true;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            final Tile tile = mTile.get();
            if (tile == null) { return null; }

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = mSample;
            // TODO catch OOM
            final Bitmap bitmap = mDecoder.decodeRegion(tile.rect, options);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            final TiledBitmapSource source = mSource.get();
            final Tile tile = mTile.get();
            if (source == null || tile == null) {
                Log.w(LOG_TAG, "Should call cancel() on LoadTileTask");
                if (bitmap != null) { bitmap.recycle(); }
            } else {
                // TODO What if bitmap is null ? Add a flag ?
                tile.loading = false;
                tile.bitmap = bitmap;
                // Callback
                source.onLoadTileDone(this, mSample);
            }
        }

        @Override
        protected void onCancelled(Bitmap bitmap) {
            super.onCancelled(bitmap);
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }

    private static int prevPow2(int n) {
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n - (n >> 1);
    }
}

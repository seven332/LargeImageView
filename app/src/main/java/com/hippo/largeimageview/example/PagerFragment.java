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

package com.hippo.largeimageview.example;

/*
 * Created by Hippo on 10/3/2016.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.largeimageview.AutoSource;
import com.hippo.largeimageview.LargeImageView;
import com.hippo.streampipe.InputStreamPipe;

import java.io.IOException;
import java.io.InputStream;

public class PagerFragment extends Fragment {

    private int mImageId;
    private LargeImageView mLargeImageView;

    public void setImageId(int imageId) {
        mImageId = imageId;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_pager, container, false);
        mLargeImageView = (LargeImageView) rootView.findViewById(R.id.large_image_view);
        //final Button button = (Button) rootView.findViewById(R.id.orientation);

        mLargeImageView.setImage(new AutoSource(new ResourceInputStreamPipe(mImageId)));
        /*
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int o  = mLargeImageView.getOrientation() + 1;
                if (o > LargeImageView.ORIENTATION_270) {
                    o = LargeImageView.ORIENTATION_0;
                }
                mLargeImageView.setOrientation(o);
            }
        });
        */

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLargeImageView.setImage(null);
        mLargeImageView = null;
    }

    private class ResourceInputStreamPipe implements InputStreamPipe {

        private final int mId;
        private InputStream mStream;

        public ResourceInputStreamPipe(int id) {
            mId = id;
        }

        @Override
        public void obtain() {}

        @Override
        public void release() {}

        @NonNull
        @Override
        public InputStream open() throws IOException {
            if (mStream != null) {
                throw new IOException("Can't open twice");
            }
            return getResources().openRawResource(mId);
        }

        @Override
        public void close() {
            if (mStream != null) {
                try {
                    mStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mStream = null;
            }
        }
    }
}

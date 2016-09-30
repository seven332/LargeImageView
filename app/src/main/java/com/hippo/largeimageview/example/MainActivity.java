package com.hippo.largeimageview.example;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.hippo.largeimageview.AutoSource;
import com.hippo.largeimageview.LargeImageView;
import com.hippo.streampipe.InputStreamPipe;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final LargeImageView view = (LargeImageView) findViewById(R.id.large_image_view);
        view.setImage(new AutoSource(new ResourceInputStreamPipe(R.raw.jpeg_large)));
        view.setBackgroundColor(Color.BLUE);

        Button button = (Button) findViewById(R.id.change_size);
        button.setOnClickListener(new View.OnClickListener() {
            private boolean i;
            @Override
            public void onClick(View v) {
                i = !i;
                if (i) {
                    view.setBottom(view.getBottom() - 500);
                } else {
                    view.setBottom(view.getBottom() + 500);
                }
            }
        });

        Button button2 = (Button) findViewById(R.id.orientation);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int o  = view.getOrientation() + 1;
                if (o > LargeImageView.ORIENTATION_270) {
                    o = LargeImageView.ORIENTATION_0;
                }
                view.setOrientation(o);
            }
        });
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

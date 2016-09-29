package com.hippo.largeimageview.example;

import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.hippo.largeimageview.BitmapSource;
import com.hippo.largeimageview.ImageSource;
import com.hippo.largeimageview.LargeImageView;
import com.hippo.largeimageview.TiledBitmapSource;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final LargeImageView view = (LargeImageView) findViewById(R.id.large_image_view);

        ImageSource source = null;
        try {
            source = new TiledBitmapSource(BitmapRegionDecoder.newInstance(getResources().openRawResource(R.raw.jpeg_large), false));
        } catch (IOException e) {

        }



        view.setImage(source);

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
}

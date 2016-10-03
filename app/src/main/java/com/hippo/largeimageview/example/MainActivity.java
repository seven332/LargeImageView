package com.hippo.largeimageview.example;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new Adapter(getSupportFragmentManager()));
    }

    private class Adapter extends FragmentStatePagerAdapter {

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            PagerFragment fragment = new PagerFragment();

            switch (position % 6) {
                case 0:
                case 1:
                    fragment.setImageId(R.raw.h_line);
                    break;
                case 2:
                case 3:
                    fragment.setImageId(R.raw.jpeg_large);
                    break;
                case 4:
                case 5:
                    fragment.setImageId(R.raw.v_line);
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 100;
        }
    }
}

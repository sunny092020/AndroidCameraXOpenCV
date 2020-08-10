package com.ami.icamdocscanner.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.adapters.ViewPagerEditAdapter;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.io.File;
import java.util.List;

public class ImageDisplayActivity extends AppCompatActivity {
    ViewPager2 viewPagerEdit;
    ViewPagerEditAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        viewPagerEdit = findViewById(R.id.viewPagerEdit);

        File directory = (RecyclerImageFile) getIntent().getSerializableExtra("directory");

        List<RecyclerImageFile> images = FileUtils.listFiles(directory);
        adapter = new ViewPagerEditAdapter(this, images);
        viewPagerEdit.setAdapter(adapter);

        int currentPos = getIntent().getIntExtra("position", 0);
        TextView pager = findViewById(R.id.pager);
        pager.setText(currentPos+1 + "/" + images.size());

        viewPagerEdit.setCurrentItem(currentPos, false);

        viewPagerEdit.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                TextView pager = findViewById(R.id.pager);
                pager.setText(position+1 + "/" + images.size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
    }
}

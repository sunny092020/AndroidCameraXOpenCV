package com.ami.icamdocscanner.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.adapters.ViewPagerEditAdapter;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import android.os.Bundle;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
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
        List<RecyclerImageFile> images = new ArrayList<>();
        for(File f: directory.listFiles(File::isFile)) {
            RecyclerImageFile imageFile = new RecyclerImageFile(f);
            imageFile.setSaved(true);
            images.add(imageFile);
        }

        adapter = new ViewPagerEditAdapter(this, images);
        viewPagerEdit.setAdapter(adapter);

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

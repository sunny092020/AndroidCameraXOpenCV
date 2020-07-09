package com.ami.icamdocscanner;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.Preferences;
import com.ami.icamdocscanner.helpers.ScannerConstants;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.core.MatOfPoint2f;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ImageCropActivity extends AppCompatActivity {
    ViewPager2 viewPager2;

    private void toEditImage() {
        Intent cropIntent = new Intent(this, ImageEditActivity.class);
        startActivity(cropIntent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        initView();
        viewPager2 = findViewById(R.id.viewPager2);
        File directory = new File(FileUtils.tempDir(this));

        List<RecyclerImageFile> files = FileUtils.listFilesByName(directory);

        viewPager2.setAdapter(new ViewPagerAdapter(this, setContours(files), viewPager2));

        int currentImagePosition =  getIntent().getIntExtra("currentImagePosition", files.size());
        viewPager2.setCurrentItem(currentImagePosition, false);
    }

    private List<RecyclerImageFile> setContours(List<RecyclerImageFile> files) {
        for (RecyclerImageFile file : files) {
            Bitmap rotated90croppedBmp = FileUtils.readBitmap(file.getAbsolutePath());
            MatOfPoint2f contour = VisionUtils.findContours(rotated90croppedBmp, this);
            file.setCroppedPolygon(contour);
        }
        return files;
    }


    private void initView() {
        Button btnImageCrop = findViewById(R.id.btnImageCrop);
        Button btnClose = findViewById(R.id.btnClose);
        Button btnAdd = findViewById(R.id.btnAdd);

        btnImageCrop.setOnClickListener(btnImageEnhanceClick);
        btnClose.setOnClickListener(btnCloseClick);

        btnAdd.setOnClickListener(btnAddClick);
    }

    private OnClickListener btnCloseClick = v -> {
        ScannerConstants.resetCaptureState();
        Intent intent = new Intent(this, MainActivity.class);
        int currentImagePosition = viewPager2.getCurrentItem();
        intent.putExtra("currentImagePosition", currentImagePosition);
        startActivity(intent);
        finish();
    };

    private OnClickListener btnAddClick = v -> {
        ScannerConstants.resetCaptureState();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("add", true);
        startActivity(intent);
        finish();
    };

    private OnClickListener btnImageEnhanceClick = v -> toEditImage();

}

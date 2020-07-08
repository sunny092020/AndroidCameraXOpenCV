package com.ami.icamdocscanner;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.helpers.FileUtils;
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

    private OnClickListener btnImageEnhanceClick = v -> toEditImage();

    private void toEditImage() {
        Intent cropIntent = new Intent(this, ImageEditActivity.class);
        startActivity(cropIntent);
        finish();
    }


    private OnClickListener btnCloseClick = v -> {
        ScannerConstants.resetCaptureState();
        Intent intent = new Intent(this, MainActivity.class);
        int currentImagePosition = viewPager2.getCurrentItem();
        intent.putExtra("currentImagePosition", currentImagePosition);
        startActivity(intent);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        initView();
        viewPager2 = findViewById(R.id.viewPager2);
        File directory = new File(FileUtils.tempDir(this));
        viewPager2.setAdapter(new ViewPagerAdapter(this, listFiles(directory), viewPager2));

        int currentImagePosition =  getIntent().getIntExtra("currentImagePosition", -1);
        viewPager2.setCurrentItem(currentImagePosition, false);
    }

    private List<RecyclerImageFile> listFiles(File directory) {
        File[] files = directory.listFiles(File::isFile);
        assert files != null;
        Arrays.sort( files, (Comparator<File>) (o1, o2) -> {
            String name1 = FileUtils.fileNameWithoutExtension(o1.getName());
            String name2 = FileUtils.fileNameWithoutExtension(o2.getName());
            return Integer.compare(Integer.parseInt(name1), Integer.parseInt(name2));
        });

        List<RecyclerImageFile> recyclerImageFiles = new ArrayList<>();

        for (File file : files) {
            RecyclerImageFile returnFile = new RecyclerImageFile(file);
            Bitmap rotated90croppedBmp = FileUtils.readBitmap(returnFile.getAbsolutePath());
            MatOfPoint2f contour = VisionUtils.findContours(rotated90croppedBmp, this);
            returnFile.setCroppedPolygon(contour);
            recyclerImageFiles.add(returnFile);
        }

        return recyclerImageFiles;
    }

    private void initView() {
        Button btnImageCrop = findViewById(R.id.btnImageCrop);
        Button btnClose = findViewById(R.id.btnClose);
        btnImageCrop.setText(ScannerConstants.cropText);
        btnClose.setText(ScannerConstants.backText);
        btnImageCrop.setBackgroundColor(Color.parseColor(ScannerConstants.cropColor));
        btnClose.setBackgroundColor(Color.parseColor(ScannerConstants.backColor));
        btnImageCrop.setOnClickListener(btnImageEnhanceClick);
        btnClose.setOnClickListener(btnCloseClick);
    }
}

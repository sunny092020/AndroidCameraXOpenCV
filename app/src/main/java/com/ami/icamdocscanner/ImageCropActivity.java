package com.ami.icamdocscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.core.MatOfPoint2f;

import java.util.List;

public class ImageCropActivity extends AppCompatActivity {
    ViewPager2 viewPager2;
    ViewPagerCropAdapter adapter;

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

        adapter = new ViewPagerCropAdapter(this, viewPager2);

        viewPager2.setAdapter(adapter);

        int currentImagePosition =  getIntent().getIntExtra("currentImagePosition", ScannerState.capturedImages.size());
        viewPager2.setCurrentItem(currentImagePosition, false);
    }

    private List<RecyclerImageFile> setContours(List<RecyclerImageFile> files) {
        for (RecyclerImageFile file : files) {
            // already set contours
            if(file.getCroppedPolygon() != null) continue;

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
        ScannerState.resetScannerState();
        Intent intent = new Intent(this, MainActivity.class);
        int currentImagePosition = viewPager2.getCurrentItem();
        intent.putExtra("currentImagePosition", currentImagePosition);
        startActivity(intent);
        finish();
    };

    private OnClickListener btnAddClick = v -> {
        ScannerState.resetScannerState();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("add", true);
        startActivity(intent);
        finish();
    };

    private OnClickListener btnImageEnhanceClick = v -> toEditImage();

}

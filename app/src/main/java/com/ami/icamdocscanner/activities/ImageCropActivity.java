package com.ami.icamdocscanner.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.adapters.ViewPagerCropAdapter;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.List;
import java.util.Objects;

public class ImageCropActivity extends AppCompatActivity {
    ViewPager2 viewPager2;
    ViewPagerCropAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        initView();
        viewPager2 = findViewById(R.id.viewPagerCrop);

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

    private void toEditImage() {
        for(RecyclerImageFile file: ScannerState.capturedImages) {
            Bitmap croppedBitmap = getCroppedImage(file);

            String editImageFilePath =  FileUtils.tempDir(this) + FileUtils.fileNameWithoutExtension(file.getName()) + "_edit.jpg";
            String doneImageFilePath =  FileUtils.tempDir(this) + FileUtils.fileNameWithoutExtension(file.getName()) + "_done.jpg";
            FileUtils.writeBitmap(croppedBitmap, editImageFilePath);
            FileUtils.writeBitmap(croppedBitmap, doneImageFilePath);
            ScannerState.croppedImages.add(new RecyclerImageFile(editImageFilePath));
            ScannerState.editedImages.add(new RecyclerImageFile(doneImageFilePath));
        }

        Intent cropIntent = new Intent(this, ImageEditActivity.class);
        startActivity(cropIntent);
        finish();
    }

    private Bitmap getCroppedImage(RecyclerImageFile imageFile) {
        try {
            Bitmap imageFileBitmap = FileUtils.readBitmap(imageFile.getAbsolutePath());

            if(imageFile.getCroppedPolygon() == null) return imageFileBitmap;

            List<Point> points = imageFile.getCroppedPolygon().toList();

            ImageView imageView = findViewById(R.id.imageView);

            float xRatio = (float) imageFileBitmap.getWidth() / imageView.getWidth();
            float yRatio = (float) imageFileBitmap.getHeight() / imageView.getHeight();

            float x1 = (float) ((Objects.requireNonNull(points.get(0)).x) * xRatio);
            float x2 = (float) ((Objects.requireNonNull(points.get(1)).x) * xRatio);
            float x3 = (float) ((Objects.requireNonNull(points.get(2)).x) * xRatio);
            float x4 = (float) ((Objects.requireNonNull(points.get(3)).x) * xRatio);
            float y1 = (float) ((Objects.requireNonNull(points.get(0)).y) * yRatio);
            float y2 = (float) ((Objects.requireNonNull(points.get(1)).y) * yRatio);
            float y3 = (float) ((Objects.requireNonNull(points.get(2)).y) * yRatio);
            float y4 = (float) ((Objects.requireNonNull(points.get(3)).y) * yRatio);
            return VisionUtils.getScannedBitmap(imageFileBitmap, x1, y1, x2, y2, x3, y3, x4, y4);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

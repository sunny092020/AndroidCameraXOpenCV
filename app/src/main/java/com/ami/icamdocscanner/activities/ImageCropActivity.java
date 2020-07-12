package com.ami.icamdocscanner.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.adapters.ViewPagerCropAdapter;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.ScannerConstant;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ImageCropActivity extends AppCompatActivity {
    ViewPager2 viewPager2;
    ViewPagerCropAdapter adapter;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);

        context = this;
        initView();
        viewPager2 = findViewById(R.id.viewPagerCrop);

        adapter = new ViewPagerCropAdapter(this, viewPager2);
        viewPager2.setAdapter(adapter);
        viewPager2.setCurrentItem(ScannerState.getCropImages().size(), false);
    }

    // user go back to re-crop
    protected void onStart() {
        super.onStart();
        int currentImagePosition =  getIntent().getIntExtra("currentImagePosition", -1);
        viewPager2.setCurrentItem(currentImagePosition, false);
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
        Intent intent = new Intent(this, MainActivity.class);
        int currentImagePosition = viewPager2.getCurrentItem();
        intent.putExtra("currentImagePosition", currentImagePosition);
        startActivityForResult(intent, ScannerConstant.RETAKE_PHOTO);
    };

    private OnClickListener btnAddClick = v -> {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("add", true);
        startActivity(intent);
        finish();
    };

    //onActivityResult
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ScannerConstant.RETAKE_PHOTO) {
            if(resultCode == Activity.RESULT_OK){
                int currentImagePosition =  data.getIntExtra("currentImagePosition", ScannerState.getCropImages().size());
                viewPager2.setCurrentItem(currentImagePosition);
                adapter.notifyItemChanged(currentImagePosition);
            }
            if (resultCode == Activity.RESULT_CANCELED) {}
        }
    }

    private OnClickListener btnImageEnhanceClick = v -> {
        int currentImagePosition =  getIntent().getIntExtra("currentImagePosition", -1);
        if(currentImagePosition == -1) {
            toEditImage();
        } else {
            RecyclerImageFile file = ScannerState.getCropImages().get(currentImagePosition);
            Bitmap croppedBitmap = getCroppedImage(file);

            String editImageFilePath =  FileUtils.editImagePath(context, file.getName());
            String doneImageFilePath =  FileUtils.doneImagePath(context, file.getName());

            FileUtils.writeBitmap(croppedBitmap, editImageFilePath);
            FileUtils.writeBitmap(croppedBitmap, doneImageFilePath);
            Intent cropIntent = new Intent(this, ImageEditActivity.class);
            cropIntent.putExtra("currentImagePosition", currentImagePosition);
            setResult(Activity.RESULT_OK,cropIntent);
            finish();
        }
    };

    private void toEditImage() {
        new Thread(() -> {
            for(int i=ScannerState.getCropImages().size()-1; i>=0; i--) {
                RecyclerImageFile file = ScannerState.getCropImages().get(i);
                Bitmap croppedBitmap = getCroppedImage(file);

                String editImageFilePath =  FileUtils.editImagePath(context, file.getName());
                String doneImageFilePath =  FileUtils.doneImagePath(context, file.getName());

                FileUtils.writeBitmap(croppedBitmap, editImageFilePath);
                FileUtils.writeBitmap(croppedBitmap, doneImageFilePath);
            }

        }).start();

        for(RecyclerImageFile file: ScannerState.getCropImages()) {
            String editImageFilePath =  FileUtils.editImagePath(context, file.getName());
            String doneImageFilePath =  FileUtils.doneImagePath(context, file.getName());
            ScannerState.getEditImages().add(new RecyclerImageFile(editImageFilePath));
            ScannerState.getDoneImages().add(new RecyclerImageFile(doneImageFilePath));
        }

        Intent cropIntent = new Intent(this, ImageEditActivity.class);
        startActivity(cropIntent);
        finish();
    }

    private Bitmap getCroppedImage(RecyclerImageFile imageFile) {
        try {
            Bitmap imageFileBitmap = FileUtils.readBitmap(imageFile.getAbsolutePath());
            if(imageFile.getCroppedPolygon() == null) return imageFileBitmap;
            List<Point> cropPolygonPoints = imageFile.getCroppedPolygon().toList();
            FrameLayout holderImageCrop = findViewById(R.id.holderImageCrop);
            float kx = (float) holderImageCrop.getWidth()/imageFileBitmap.getWidth();
            float ky = (float) holderImageCrop.getHeight()/imageFileBitmap.getHeight();
            float k = (Math.min(kx, ky));

            List<Point> points = new ArrayList<>();
            points.add(new Point(cropPolygonPoints.get(0).x*k, cropPolygonPoints.get(0).y*k));
            points.add(new Point(cropPolygonPoints.get(1).x*k, cropPolygonPoints.get(1).y*k));
            points.add(new Point(cropPolygonPoints.get(2).x*k, cropPolygonPoints.get(2).y*k));
            points.add(new Point(cropPolygonPoints.get(3).x*k, cropPolygonPoints.get(3).y*k));

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

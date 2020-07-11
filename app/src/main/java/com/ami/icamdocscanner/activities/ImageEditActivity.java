package com.ami.icamdocscanner.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.adapters.ViewPagerEditAdapter;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ImageEditActivity extends AppCompatActivity {
    ViewPager2 viewPagerEdit;
    ViewPagerEditAdapter adapter;

    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.0f;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);
        context = this;

        viewPagerEdit = findViewById(R.id.viewPagerEdit);
        adapter = new ViewPagerEditAdapter(this);
        viewPagerEdit.setAdapter(adapter);
        displayFilterThumbnails();
        setupFilterButtonEvent();
        setupBottomButtonEvent();

        int currentImagePosition =  getIntent().getIntExtra("currentImagePosition", ScannerState.croppedImages.size());
        viewPagerEdit.setCurrentItem(currentImagePosition, false);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    private void displayFilterThumbnails() {
        int currentImagePosition = viewPagerEdit.getCurrentItem();
        RecyclerImageFile currentImage = ScannerState.croppedImages.get(currentImagePosition);
        Bitmap currentBitmap = FileUtils.readBitmap(currentImage.getAbsolutePath());

        Mat origin = new Mat();
        Utils.bitmapToMat(currentBitmap, origin);

        double DOWNSCALE_IMAGE_SIZE = 80f;

        // Downscale image for better performance.
        double ratio = DOWNSCALE_IMAGE_SIZE / Math.max(origin.width(), origin.height());

        Mat smallOrigin = VisionUtils.downscaleMat(origin, ratio);
        Bitmap smallOriginBitmap = VisionUtils.matToBitmap(smallOrigin);

        Mat grayThumbnail = new Mat();
        VisionUtils.toGray(smallOrigin, grayThumbnail);
        Bitmap grayThumbnailBitmap = VisionUtils.matToBitmap(grayThumbnail);

        Mat enhanceThumbnail = new Mat();
        VisionUtils.enhance(smallOrigin, enhanceThumbnail);
        Bitmap enhanceBitmap = VisionUtils.matToBitmap(enhanceThumbnail);

        Mat bwThumbnail = new Mat();
        VisionUtils.toBw(smallOrigin, bwThumbnail);
        Bitmap bwBitmap = VisionUtils.matToBitmap(bwThumbnail);

        ImageView imgOrigin = findViewById(R.id.imgOrigin);
        ImageView imgGray = findViewById(R.id.imgGray);
        ImageView imgEnhance = findViewById(R.id.imgEnhance);
        ImageView imgBw = findViewById(R.id.imgBw);

        imgOrigin.setImageBitmap(smallOriginBitmap);
        imgGray.setImageBitmap(grayThumbnailBitmap);
        imgEnhance.setImageBitmap(enhanceBitmap);
        imgBw.setImageBitmap(bwBitmap);
    }

    private void setupFilterButtonEvent() {
        int currentImagePosition = viewPagerEdit.getCurrentItem();

        ImageView imgOrigin = findViewById(R.id.imgOrigin);
        ImageView imgGray = findViewById(R.id.imgGray);
        ImageView imgEnhance = findViewById(R.id.imgEnhance);
        ImageView imgBw = findViewById(R.id.imgBw);

        RecyclerImageFile currentImage = ScannerState.croppedImages.get(currentImagePosition);
        Bitmap currentBitmap = FileUtils.readBitmap(currentImage.getAbsolutePath());

        imgOrigin.setOnClickListener(v -> {
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(currentBitmap);
        });

        imgGray.setOnClickListener(v -> {
            Mat gray = new Mat();
            Mat origin = new Mat();
            Utils.bitmapToMat(currentBitmap, origin);
            VisionUtils.toGray(origin, gray);
            Bitmap grayBitmap = VisionUtils.matToBitmap(gray);
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(grayBitmap);
            String doneImageFilePath =  FileUtils.tempDir(context) + FileUtils.fileNameWithoutExtension(currentImage.getName()) + "_done.jpg";
            FileUtils.writeBitmap(grayBitmap, doneImageFilePath);
        });

        imgEnhance.setOnClickListener(v -> {
            Mat enhance = new Mat();
            Mat origin = new Mat();
            Utils.bitmapToMat(currentBitmap, origin);
            VisionUtils.enhance(origin, enhance);
            Bitmap enhanceBitmap = VisionUtils.matToBitmap(enhance);
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(enhanceBitmap);
            String doneImageFilePath =  FileUtils.tempDir(context) + FileUtils.fileNameWithoutExtension(currentImage.getName()) + "_done.jpg";
            FileUtils.writeBitmap(enhanceBitmap, doneImageFilePath);

        });

        imgBw.setOnClickListener(v -> {
            Mat bw = new Mat();
            Mat origin = new Mat();
            Utils.bitmapToMat(currentBitmap, origin);
            VisionUtils.toBw(origin, bw);
            Bitmap bwBitmap = VisionUtils.matToBitmap(bw);
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(bwBitmap);
            String doneImageFilePath =  FileUtils.tempDir(context) + FileUtils.fileNameWithoutExtension(currentImage.getName()) + "_done.jpg";
            FileUtils.writeBitmap(bwBitmap, doneImageFilePath);

        });
    }

    private void setupBottomButtonEvent() {
        int currentImagePosition = viewPagerEdit.getCurrentItem();

        RecyclerImageFile currentImage = ScannerState.croppedImages.get(currentImagePosition);

        LinearLayout cropBtn = findViewById(R.id.cropBtn);
        if(cropBtn==null) return;

        cropBtn.setOnClickListener(v -> {
            Intent cropIntent = new Intent(context, ImageCropActivity.class);
            context.startActivity(cropIntent);
        });

        LinearLayout rotateBtn = findViewById(R.id.rotateBtn);
        if(rotateBtn==null) return;

        rotateBtn.setOnClickListener(v -> {
            ImageView imageView = findViewById(R.id.imageView);

            Bitmap currentFilteredImg = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            currentFilteredImg = VisionUtils.rotateBitmap(currentFilteredImg, 90);
            imageView.setImageBitmap(currentFilteredImg);

            String doneImageFilePath =  FileUtils.tempDir(this) + FileUtils.fileNameWithoutExtension(currentImage.getName()) + "_done.jpg";
            FileUtils.writeBitmap(currentFilteredImg, doneImageFilePath);
        });

        LinearLayout checkBtn = findViewById(R.id.checkBtn);
        if(checkBtn==null) return;
        checkBtn.setOnClickListener(v -> {
            // Get a Calendar and set it to the current time.
            Calendar cal = Calendar.getInstance();

            for(RecyclerImageFile file: ScannerState.editedImages) {
                cal.setTime(Date.from(Instant.now()));

                // Create a filename from a format string.
                // ... Apply date formatting codes.
                String filename = String.format(Locale.US, "AMI_ICAMDOC_SCANNER-%1$tY-%1$tm-%1$td-%1$tk-%1$tS-%1$tp", cal);

                Bitmap currentFilteredImg = FileUtils.readBitmap(file.getAbsolutePath());

                try (FileOutputStream out = context.openFileOutput(filename + "_" + FileUtils.fileNameWithoutExtension(file.getName()) + ".jpg", Context.MODE_PRIVATE)) {
                    currentFilteredImg.compress(Bitmap.CompressFormat.JPEG, 99, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Intent cropIntent = new Intent(context, ImageDoneActivity.class);
            context.startActivity(cropIntent);
        });
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));
            ImageView imageView = findViewById(R.id.imageView);

            imageView.setScaleX(mScaleFactor);
            imageView.setScaleY(mScaleFactor);
            return true;
        }
    }
}

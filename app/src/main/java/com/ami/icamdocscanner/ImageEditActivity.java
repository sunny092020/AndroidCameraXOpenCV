package com.ami.icamdocscanner;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.helpers.VisionUtils;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

public class ImageEditActivity extends AppCompatActivity {
    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private ImageView imageView;

    private ImageView imgOrigin, imgGray, imgEnhance, imgBw;
    private Bitmap currentImg;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);

        context = this;

        imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(ScannerState.cropImageBitmap);
        currentImg = ScannerState.cropImageBitmap;
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        setFrameLayoutRatio();

        displayFilterThumbnails();

        setupFilterButtonEvent();

        setupBottomButtonEvent();
    }

    private void displayFilterThumbnails() {
        Mat origin = new Mat();
        Utils.bitmapToMat(ScannerState.cropImageBitmap, origin);

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

        imgOrigin = findViewById(R.id.imgOrigin);
        imgGray = findViewById(R.id.imgGray);
        imgEnhance = findViewById(R.id.imgEnhance);
        imgBw = findViewById(R.id.imgBw);

        imgOrigin.setImageBitmap(smallOriginBitmap);
        imgGray.setImageBitmap(grayThumbnailBitmap);
        imgEnhance.setImageBitmap(enhanceBitmap);
        imgBw.setImageBitmap(bwBitmap);
    }

    private void setupFilterButtonEvent() {
        imgOrigin.setOnClickListener(v -> imageView.setImageBitmap(currentImg));

        imgGray.setOnClickListener(v -> {
            Mat gray = new Mat();
            Mat origin = new Mat();
            Utils.bitmapToMat(currentImg, origin);
            VisionUtils.toGray(origin, gray);
            Bitmap grayBitmap = VisionUtils.matToBitmap(gray);
            imageView.setImageBitmap(grayBitmap);
        });

        imgEnhance.setOnClickListener(v -> {
            Mat enhance = new Mat();
            Mat origin = new Mat();
            Utils.bitmapToMat(currentImg, origin);
            VisionUtils.enhance(origin, enhance);
            Bitmap enhanceBitmap = VisionUtils.matToBitmap(enhance);
            imageView.setImageBitmap(enhanceBitmap);
        });

        imgBw.setOnClickListener(v -> {
            Mat bw = new Mat();
            Mat origin = new Mat();
            Utils.bitmapToMat(currentImg, origin);
            VisionUtils.toBw(origin, bw);
            Bitmap bwBitmap = VisionUtils.matToBitmap(bw);
            imageView.setImageBitmap(bwBitmap);
        });
    }

    private void setupBottomButtonEvent() {
        LinearLayout cropBtn = findViewById(R.id.cropBtn);
        cropBtn.setOnClickListener(v -> {
            Intent cropIntent = new Intent(this, ImageCropActivity.class);
            startActivity(cropIntent);
            finish();
        });

        LinearLayout rotateBtn = findViewById(R.id.rotateBtn);
        rotateBtn.setOnClickListener(v -> {
            Bitmap currentFilteredImg = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            currentFilteredImg = VisionUtils.rotateBitmap(currentFilteredImg, 90);
            currentImg = VisionUtils.rotateBitmap(currentImg, 90);
            imageView.setImageBitmap(currentFilteredImg);
        });

        LinearLayout checkBtn = findViewById(R.id.checkBtn);
        checkBtn.setOnClickListener(v -> {
            // Get a Calendar and set it to the current time.
            Calendar cal = Calendar.getInstance();
            cal.setTime(Date.from(Instant.now()));

            // Create a filename from a format string.
            // ... Apply date formatting codes.
            String filename = String.format("AMI_ICAMDOC_SCANNER-%1$tY-%1$tm-%1$td-%1$tk-%1$tS-%1$tp.jpg", cal);

            Bitmap currentFilteredImg = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            try (FileOutputStream out = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
                currentFilteredImg.compress(Bitmap.CompressFormat.JPEG, 99, out);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Intent cropIntent = new Intent(this, ImageDoneActivity.class);
            startActivity(cropIntent);
            finish();
        });
    }

    private void setFrameLayoutRatio() {
        FrameLayout frameLayout = findViewById(R.id.frameLayout);

        // Gets the layout params that will allow you to resize the layout
        ViewGroup.LayoutParams params = frameLayout.getLayoutParams();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        // Changes the height and width to the specified *pixels*
        params.width = width;
        params.height = width*4/3;

        frameLayout.setLayoutParams(params);
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
            imageView.setScaleX(mScaleFactor);
            imageView.setScaleY(mScaleFactor);
            return true;
        }
    }
}
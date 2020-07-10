package com.ami.icamdocscanner.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
    private ImageView imageView;

    private ImageView imgOrigin, imgGray, imgEnhance, imgBw;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);

        context = this;

        viewPagerEdit = findViewById(R.id.viewPagerEdit);
        adapter = new ViewPagerEditAdapter(this);
        viewPagerEdit.setAdapter(adapter);

        viewPagerEdit.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Log.e("Selected_Page", String.valueOf(position));
                viewPagerEdit.post(() -> {
                    displayFilterThumbnails(position);
                    setFrameLayoutRatio();
                    setupFilterButtonEvent(position);
                    setupBottomButtonEvent(position);
                });
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });



        int currentImagePosition =  getIntent().getIntExtra("currentImagePosition", ScannerState.croppedImages.size());
        viewPagerEdit.setCurrentItem(currentImagePosition, false);

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

    }

    private void displayFilterThumbnails(int currentImagePosition) {
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

        imgOrigin = findViewById(R.id.imgOrigin);
        imgGray = findViewById(R.id.imgGray);
        imgEnhance = findViewById(R.id.imgEnhance);
        imgBw = findViewById(R.id.imgBw);

        imgOrigin.setImageBitmap(smallOriginBitmap);
        imgGray.setImageBitmap(grayThumbnailBitmap);
        imgEnhance.setImageBitmap(enhanceBitmap);
        imgBw.setImageBitmap(bwBitmap);
    }

    private void setupFilterButtonEvent(int currentImagePosition) {
        ImageView imgOrigin = findViewById(R.id.imgOrigin);
        ImageView imgGray = findViewById(R.id.imgGray);
        ImageView imgEnhance = findViewById(R.id.imgEnhance);
        ImageView imgBw = findViewById(R.id.imgBw);

        if(imgOrigin==null) return;
        if(imgGray==null) return;
        if(imgEnhance==null) return;
        if(imgBw==null) return;

        RecyclerImageFile currentImage = ScannerState.croppedImages.get(currentImagePosition);
        Bitmap currentBitmap = FileUtils.readBitmap(currentImage.getAbsolutePath());

        imageView = findViewById(R.id.imageView);
        imgOrigin.setOnClickListener(v -> imageView.setImageBitmap(currentBitmap));

        imgOrigin.setOnClickListener(null);

        imgGray.setOnClickListener(v -> {
            Mat gray = new Mat();
            Mat origin = new Mat();
            Utils.bitmapToMat(currentBitmap, origin);
            VisionUtils.toGray(origin, gray);
            Bitmap grayBitmap = VisionUtils.matToBitmap(gray);
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
            imageView.setImageBitmap(bwBitmap);

            String doneImageFilePath =  FileUtils.tempDir(context) + FileUtils.fileNameWithoutExtension(currentImage.getName()) + "_done.jpg";
            FileUtils.writeBitmap(bwBitmap, doneImageFilePath);

        });
    }

    private void setupBottomButtonEvent(int currentImagePosition) {
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

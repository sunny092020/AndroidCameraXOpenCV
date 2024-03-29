package com.ami.icamdocscanner.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.adapters.ViewPagerEditAdapter;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.ScannerConstant;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

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
        adapter = new ViewPagerEditAdapter(this, ScannerState.getEditImages(), viewPagerEdit);
        viewPagerEdit.setAdapter(adapter);

        setupFilterButtonEvent();
        setupBottomButtonEvent();

        viewPagerEdit.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                new Thread(() -> displayFilterThumbnails(position)).start();
                TextView pager = findViewById(R.id.pager);
                pager.setText(position+1 + "/" + ScannerState.getEditImages().size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        viewPagerEdit.setCurrentItem(0, false);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onResume() {
        super.onResume();
    }

    private void displayFilterThumbnails(int currentImagePosition) {
        RecyclerImageFile currentImage = ScannerState.getEditImages().get(currentImagePosition);
        Bitmap currentBitmap = FileUtils.readBitmap(currentImage);
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

        runOnUiThread(() -> {
            ImageView imgOrigin = findViewById(R.id.imgOrigin);
            ImageView imgGray = findViewById(R.id.imgGray);
            ImageView imgEnhance = findViewById(R.id.imgEnhance);
            ImageView imgBw = findViewById(R.id.imgBw);
            imgOrigin.setImageBitmap(smallOriginBitmap);
            imgGray.setImageBitmap(grayThumbnailBitmap);
            imgEnhance.setImageBitmap(enhanceBitmap);
            imgBw.setImageBitmap(bwBitmap);
        });
    }

    private void setupFilterButtonEvent() {
        ImageView imgOrigin = findViewById(R.id.imgOrigin);
        ImageView imgGray = findViewById(R.id.imgGray);
        ImageView imgEnhance = findViewById(R.id.imgEnhance);
        ImageView imgBw = findViewById(R.id.imgBw);

        imgOrigin.setOnClickListener(v -> {
            ImageView imageView = findViewById(R.id.imageView);

            int currentImagePosition = viewPagerEdit.getCurrentItem();
            RecyclerImageFile currentImageEdit = ScannerState.getEditImages().get(currentImagePosition);
            RecyclerImageFile currentImageDone = ScannerState.getDoneImages().get(currentImagePosition);

            Bitmap currentImageEditBitmap = FileUtils.readBitmap(currentImageEdit.getAbsolutePath());
            imageView.setImageBitmap(currentImageEditBitmap);

            FileUtils.copyFileUsingChannel(currentImageEdit, currentImageDone);
        });

        imgGray.setOnClickListener(v -> {
            Mat gray = new Mat();
            Mat origin = new Mat();

            int currentImagePosition = viewPagerEdit.getCurrentItem();
            RecyclerImageFile currentImageEdit = ScannerState.getEditImages().get(currentImagePosition);
            Bitmap currentImageEditBitmap = FileUtils.readBitmap(currentImageEdit.getAbsolutePath());

            Utils.bitmapToMat(currentImageEditBitmap, origin);
            VisionUtils.toGray(origin, gray);
            Bitmap grayBitmap = VisionUtils.matToBitmap(gray);
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(grayBitmap);

            RecyclerImageFile currentImageDone = ScannerState.getDoneImages().get(currentImagePosition);
            FileUtils.writeBitmap(grayBitmap, currentImageDone.getAbsolutePath());
        });

        imgEnhance.setOnClickListener(v -> {
            Mat enhance = new Mat();
            Mat origin = new Mat();

            int currentImagePosition = viewPagerEdit.getCurrentItem();
            RecyclerImageFile currentImageEdit = ScannerState.getEditImages().get(currentImagePosition);
            Bitmap currentImageEditBitmap = FileUtils.readBitmap(currentImageEdit.getAbsolutePath());

            Utils.bitmapToMat(currentImageEditBitmap, origin);
            VisionUtils.enhance(origin, enhance);
            Bitmap enhanceBitmap = VisionUtils.matToBitmap(enhance);
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(enhanceBitmap);

            RecyclerImageFile currentImageDone = ScannerState.getDoneImages().get(currentImagePosition);
            FileUtils.writeBitmap(enhanceBitmap, currentImageDone.getAbsolutePath());
        });

        imgBw.setOnClickListener(v -> {
            Mat bw = new Mat();
            Mat origin = new Mat();

            int currentImagePosition = viewPagerEdit.getCurrentItem();
            RecyclerImageFile currentImageEdit = ScannerState.getEditImages().get(currentImagePosition);
            Bitmap currentImageEditBitmap = FileUtils.readBitmap(currentImageEdit.getAbsolutePath());

            Utils.bitmapToMat(currentImageEditBitmap, origin);
            VisionUtils.toBw(origin, bw);
            Bitmap bwBitmap = VisionUtils.matToBitmap(bw);
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(bwBitmap);

            RecyclerImageFile currentImageDone = ScannerState.getDoneImages().get(currentImagePosition);
            FileUtils.writeBitmap(bwBitmap, currentImageDone.getAbsolutePath());
        });
    }

    private void setupBottomButtonEvent() {
        LinearLayout cropBtn = findViewById(R.id.cropBtn);
        cropBtn.setOnClickListener(v -> {
            Intent cropIntent = new Intent(context, ImageCropActivity.class);
            int currentImagePosition = viewPagerEdit.getCurrentItem();
            cropIntent.putExtra("currentImagePosition", currentImagePosition);
            startActivityForResult(cropIntent, ScannerConstant.RECROP_PHOTO);
        });

        LinearLayout rotateBtn = findViewById(R.id.rotateBtn);
        rotateBtn.setOnClickListener(v -> {
            ImageView imageView = findViewById(R.id.imageView);
            Bitmap currentFilteredImg = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            currentFilteredImg = VisionUtils.rotateBitmap(currentFilteredImg, 90);
            imageView.setImageBitmap(currentFilteredImg);

            int currentImagePosition = viewPagerEdit.getCurrentItem();
            RecyclerImageFile currentImageDone = ScannerState.getDoneImages().get(currentImagePosition);
            RecyclerImageFile currentImageEdit = ScannerState.getEditImages().get(currentImagePosition);
            Bitmap currentImageEditBitmap = FileUtils.readBitmap(currentImageEdit.getAbsolutePath());
            currentImageEditBitmap = VisionUtils.rotateBitmap(currentImageEditBitmap, 90);

            FileUtils.writeBitmap(currentImageEditBitmap, currentImageEdit.getAbsolutePath());
            FileUtils.writeBitmap(currentFilteredImg, currentImageDone.getAbsolutePath());
        });

        LinearLayout checkBtn = findViewById(R.id.checkBtn);
        if(checkBtn==null) return;
        checkBtn.setOnClickListener(v -> {
            Intent doneIntent = new Intent(context, ImageDoneActivity.class);
            context.startActivity(doneIntent);
            finish();
        });
    }

    //onActivityResult
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ScannerConstant.RECROP_PHOTO) {
            if(resultCode == Activity.RESULT_OK){
                int currentImagePosition =  data.getIntExtra("currentImagePosition", ScannerState.getEditImages().size());

                viewPagerEdit.setCurrentItem(currentImagePosition, false);
                adapter.notifyItemChanged(currentImagePosition);
            }
        }
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

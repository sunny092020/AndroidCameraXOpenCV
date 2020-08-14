package com.ami.icamdocscanner.activities;

import android.app.Activity;
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
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        new Thread(() -> {
            for(int position=0; position<ScannerState.getCropImages().size(); position++) {
                RecyclerImageFile croppedFile = ScannerState.getCropImages().get(position);
                MatOfPoint2f croppedPolygon = croppedFile.getCroppedPolygon();
                while (croppedPolygon== null) {
                    try {
                        Thread.sleep(100);
                        croppedPolygon = croppedFile.getCroppedPolygon();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Bitmap croppedBitmap = getCroppedImage(croppedFile);
                String editImageFilePath =  FileUtils.editImagePath(context, FileUtils.getOriginFileName(croppedFile.getName()));
                String doneImageFilePath =  FileUtils.doneImagePath(context, FileUtils.getOriginFileName(croppedFile.getName()));
                
                FileUtils.writeBitmap(croppedBitmap, editImageFilePath);
                FileUtils.writeBitmap(croppedBitmap, doneImageFilePath);

                ScannerState.getFileByName(editImageFilePath, ScannerState.getEditImages()).setSaved(true);
                ScannerState.getFileByName(doneImageFilePath, ScannerState.getDoneImages()).setSaved(true);

                int finalPosition = position;
                runOnUiThread(() -> {
                    adapter.notifyItemChanged(finalPosition);
                });
            }
        }).start();

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

    private Bitmap getCroppedImage(RecyclerImageFile imageFile) {
        try {
            Bitmap imageFileBitmap = FileUtils.readBitmap(imageFile.getAbsolutePath());
            List<Point> cropPolygonPoints = imageFile.getCroppedPolygon().toList();
            float kx = (float) ScannerState.holderCropWidth/imageFileBitmap.getWidth();
            float ky = (float) ScannerState.holderCropHeight/imageFileBitmap.getHeight();
            float k = (Math.min(kx, ky));

            List<Point> points = new ArrayList<>();
            points.add(new Point(cropPolygonPoints.get(0).x*k, cropPolygonPoints.get(0).y*k));
            points.add(new Point(cropPolygonPoints.get(1).x*k, cropPolygonPoints.get(1).y*k));
            points.add(new Point(cropPolygonPoints.get(2).x*k, cropPolygonPoints.get(2).y*k));
            points.add(new Point(cropPolygonPoints.get(3).x*k, cropPolygonPoints.get(3).y*k));

            int imageViewWidth =  getIntent().getIntExtra("imageViewWidth", -1);
            int imageViewHeight =  getIntent().getIntExtra("imageViewHeight", -1);

            float xRatio = (float) imageFileBitmap.getWidth() / imageViewWidth;
            float yRatio = (float) imageFileBitmap.getHeight() / imageViewHeight;

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

    protected void onStart() {
        super.onStart();
    }

    protected void onResume() {
        super.onResume();
    }

    private void displayFilterThumbnails(int currentImagePosition) {
        RecyclerImageFile currentImage = ScannerState.getEditImages().get(currentImagePosition);
        currentImage.waitUntilSaved();
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
        });
    }

    //onActivityResult
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ScannerConstant.RECROP_PHOTO) {
            if(resultCode == Activity.RESULT_OK){
                int currentImagePosition =  data.getIntExtra("currentImagePosition", ScannerState.getEditImages().size());

                RecyclerImageFile croppedFile = ScannerState.getCropImages().get(currentImagePosition);
                Bitmap croppedBitmap = getCroppedImage(croppedFile);

                String editImageFilePath =  FileUtils.editImagePath(context, FileUtils.getOriginFileName(croppedFile.getName()));
                String doneImageFilePath =  FileUtils.doneImagePath(context, FileUtils.getOriginFileName(croppedFile.getName()));

                FileUtils.writeBitmap(croppedBitmap, editImageFilePath);
                FileUtils.writeBitmap(croppedBitmap, doneImageFilePath);

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

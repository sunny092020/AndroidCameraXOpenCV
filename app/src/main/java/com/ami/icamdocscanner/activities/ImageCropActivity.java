package com.ami.icamdocscanner.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.adapters.ViewPagerCropAdapter;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.ScannerConstant;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ImageCropActivity extends AppCompatActivity {
    ViewPager2 viewPagerCrop;

    // we need this field to handle 2 cases : from main screen and from edit screen
    // when back from main screen, we have to store the currentImagePosition
    int currentImagePosition = -1;

    public ViewPagerCropAdapter getAdapter() {
        return adapter;
    }

    ViewPagerCropAdapter adapter;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);

        context = this;
        initView();
        viewPagerCrop = findViewById(R.id.viewPagerCrop);

        adapter = new ViewPagerCropAdapter(this, viewPagerCrop);
        viewPagerCrop.setAdapter(adapter);

        viewPagerCrop.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                TextView pager = findViewById(R.id.pager);
                pager.setText(position+1 + "/" + ScannerState.getCropImages().size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        viewPagerCrop.setCurrentItem(0, false);
    }

    // user go back to re-crop
    protected void onStart() {
        super.onStart();
        viewPagerCrop.setCurrentItem(getIntent().getIntExtra("currentImagePosition", currentImagePosition), false);
    }

    private void initView() {
        Button btnImageCrop = findViewById(R.id.btnImageCrop);
        Button btnClose = findViewById(R.id.btnClose);
        Button btnAdd = findViewById(R.id.btnAdd);

        LinearLayout progressBarHolder = findViewById(R.id.progressBarHolder);
        progressBarHolder.setVisibility(View.GONE);

        btnImageCrop.setOnClickListener(btnImageEnhanceClick);
        btnClose.setOnClickListener(btnCloseClick);

        btnAdd.setOnClickListener(btnAddClick);
    }

    private OnClickListener btnCloseClick = v -> {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("currentImagePosition", viewPagerCrop.getCurrentItem());
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
                currentImagePosition =  data.getIntExtra("currentImagePosition", ScannerState.getCropImages().size());
                adapter.notifyItemChanged(currentImagePosition);
                viewPagerCrop.setCurrentItem(currentImagePosition, false);
            }
            if (resultCode == Activity.RESULT_CANCELED) {}
        }
    }

    private OnClickListener btnImageEnhanceClick = v -> {
        int currentImagePosition =  getIntent().getIntExtra("currentImagePosition", -1);
        LinearLayout progressBarHolder = findViewById(R.id.progressBarHolder);
        progressBarHolder.setVisibility(View.VISIBLE);
        ProgressBar progressBar = ((Activity)context).findViewById(R.id.progressBar);
        TextView pbText = ((Activity)context).findViewById(R.id.pbText);

        new Thread(() -> {
            for(int position=0; position<ScannerState.getCropImages().size(); position++) {
                RecyclerImageFile croppedFile = ScannerState.getCropImages().get(position);
                if(!croppedFile.isChanged()) continue;
                croppedFile.setChanged(false);

                Bitmap croppedBitmap = getCroppedImage(croppedFile);
                String editImageFilePath =  FileUtils.editImagePath(context, FileUtils.getOriginFileName(croppedFile.getName()));
                String doneImageFilePath =  FileUtils.doneImagePath(context, FileUtils.getOriginFileName(croppedFile.getName()));
                RecyclerImageFile editFile = ScannerState.getFileByName(editImageFilePath, ScannerState.getEditImages());
                RecyclerImageFile doneFile = ScannerState.getFileByName(doneImageFilePath, ScannerState.getDoneImages());
                if(editFile==null) {
                    ScannerState.getEditImages().add(new RecyclerImageFile(editImageFilePath));
                }
                if(doneFile==null) {
                    ScannerState.getDoneImages().add(new RecyclerImageFile(doneImageFilePath));
                }

                Log.d("write done", position + "" + doneImageFilePath);

                FileUtils.writeBitmap(croppedBitmap, editImageFilePath);
                FileUtils.writeBitmap(croppedBitmap, doneImageFilePath);

                int percent = (position+1) *100/ScannerState.getCropImages().size();
                progressBar.setProgress(percent);
                int finalPosition = position;
                ((Activity)context).runOnUiThread(() -> {
                    pbText.setText("Processed: " + (finalPosition +1) + "/" + ScannerState.getCropImages().size() + " files");
                });
            }

            runOnUiThread(() -> progressBarHolder.setVisibility(View.GONE));

            Intent intent = new Intent(this, ImageEditActivity.class);
            intent.putExtra("currentImagePosition", viewPagerCrop.getCurrentItem());

            if(currentImagePosition == -1) {
                startActivity(intent);
            } else {
                setResult(Activity.RESULT_OK,intent);
            }
            finish();
        }).start();
    };

    private Bitmap getCroppedImage(RecyclerImageFile imageFile) {
        try {
            Bitmap imageFileBitmap = FileUtils.readBitmap(imageFile.getAbsolutePath());
            List<Point> cropPolygonPoints = imageFile.getCroppedPolygon().toList();

            FrameLayout holderImageCrop = findViewById(R.id.holderImageCrop);

            Log.d("holderImageCrop.getWidth()", "" + holderImageCrop.getWidth());
            Log.d("holderImageCrop.getHeight()", "" + holderImageCrop.getHeight());

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

            Log.d("imageView.getWidth()", "" + imageView.getWidth());
            Log.d("imageView.getHeight()", "" + imageView.getHeight());

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

package com.ami.icamdocscanner.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.adapters.ViewPagerCropAdapter;
import com.ami.icamdocscanner.helpers.ActivityUtils;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.ScannerConstant;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.models.RecyclerImageFile;

public class ImageCropActivity extends AppCompatActivity {
    ViewPager2 viewPagerCrop;

    // we need this field to handle 2 cases : from main screen and from edit screen
    // when back from main screen, we 
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
        ActivityUtils.processImageFromUri(this);

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
        if(currentImagePosition == -1) {
            toEditImage();
        } else {
            Intent cropIntent = new Intent(this, ImageEditActivity.class);
            cropIntent.putExtra("currentImagePosition", currentImagePosition);

            new Thread(() -> {
                waitImageLoad();
                ImageView imageView = findViewById(R.id.imageView);
                cropIntent.putExtra("imageViewWidth", imageView.getWidth());
                cropIntent.putExtra("imageViewHeight", imageView.getHeight());
                setResult(Activity.RESULT_OK,cropIntent);
                finish();

            }).start();
        }
    };

    private void toEditImage() {
        ScannerState.getEditImages().clear();
        ScannerState.getDoneImages().clear();

        for(RecyclerImageFile croppedFile: ScannerState.getCropImages()) {
            String doneImageFilePath =  FileUtils.doneImagePath(context, croppedFile.getName());
            String editImageFilePath =  FileUtils.editImagePath(context, croppedFile.getName());
            ScannerState.getEditImages().add(new RecyclerImageFile(editImageFilePath));
            ScannerState.getDoneImages().add(new RecyclerImageFile(doneImageFilePath));
        }

        Intent cropIntent = new Intent(this, ImageEditActivity.class);

        new Thread(() -> {
            waitImageLoad();
            ImageView imageView = findViewById(R.id.imageView);
            cropIntent.putExtra("imageViewWidth", imageView.getWidth());
            cropIntent.putExtra("imageViewHeight", imageView.getHeight());
            startActivity(cropIntent);
            finish();
        }).start();
    }

    private void waitImageLoad() {
        ImageView imageView = findViewById(R.id.imageView);
        while (imageView == null || imageView.getWidth() < 5 || imageView.getHeight() < 5) {
            try {
                Thread.sleep(100);
                imageView = findViewById(R.id.imageView);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

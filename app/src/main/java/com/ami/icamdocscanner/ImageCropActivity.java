package com.ami.icamdocscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ami.icamdocscanner.base.CropperErrorType;
import com.ami.icamdocscanner.base.DocumentScanActivity;
import com.ami.icamdocscanner.helpers.ScannerConstants;
import com.ami.icamdocscanner.libraries.PolygonView;

public class ImageCropActivity extends DocumentScanActivity {

    private FrameLayout holderImageCrop;
    private ImageView imageView;
    private PolygonView polygonView;
    private ProgressBar progressBar;
    private Bitmap cropImage;
    private OnClickListener btnImageEnhanceClick = v -> {
        showProgressBar();

        toEditImage();

//            disposable.add(
//                    Observable.fromCallable(() -> {
//                        cropImage = getCroppedImage();
//                        if (cropImage == null)
//                            return false;
//                        if (ScannerConstants.saveStorage) {
//                            Log.d("save cropImage", cropImage.toString());
//                            saveToInternalStorage(cropImage);
//                        }
//                        return false;
//                    })
//                            .subscribeOn(Schedulers.io())
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .subscribe((result) -> {
//                                hideProgressBar();
//                                if (cropImage != null) {
//                                    ScannerConstants.selectedImageBitmap = cropImage;
//                                    setResult(RESULT_OK);
//                                    finish();
//                                }
//                            })
//            );
    };

    private  void toEditImage() {
        ScannerConstants.cropImageBitmap = getCroppedImage();
        Intent cropIntent = new Intent(this, ImageEditActivity.class);
        startActivityForResult(cropIntent, 1234);
        finish();
    }

    private OnClickListener btnCloseClick = v -> {
        ScannerConstants.resetCaptureState();
        Intent intent = new Intent(this, MainActivity.class);
        startActivityForResult(intent, 1234);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        cropImage = ScannerConstants.selectedImageBitmap;
        initView();
    }

    @Override
    protected FrameLayout getHolderImageCrop() {
        return holderImageCrop;
    }

    @Override
    protected ImageView getImageView() {
        return imageView;
    }

    @Override
    protected PolygonView getPolygonView() {
        return polygonView;
    }

    @Override
    protected void showProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, false);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void hideProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, true);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void showError() {
        Toast.makeText(this, ScannerConstants.cropError, Toast.LENGTH_LONG).show();
    }

    @Override
    protected Bitmap getBitmapImage() {
        return cropImage;
    }

    private void setViewInteract(View view, boolean canDo) {
        view.setEnabled(canDo);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setViewInteract(((ViewGroup) view).getChildAt(i), canDo);
            }
        }
    }

    private void initView() {
        Button btnImageCrop = findViewById(R.id.btnImageCrop);
        Button btnClose = findViewById(R.id.btnClose);
        holderImageCrop = findViewById(R.id.holderImageCrop);
        imageView = findViewById(R.id.imageView);
        btnImageCrop.setText(ScannerConstants.cropText);
        btnClose.setText(ScannerConstants.backText);
        polygonView = findViewById(R.id.polygonView);
        progressBar = findViewById(R.id.progressBar);
        if (progressBar.getIndeterminateDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        else if (progressBar.getProgressDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getProgressDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        btnImageCrop.setBackgroundColor(Color.parseColor(ScannerConstants.cropColor));
        btnClose.setBackgroundColor(Color.parseColor(ScannerConstants.backColor));
        btnImageCrop.setOnClickListener(btnImageEnhanceClick);
        btnClose.setOnClickListener(btnCloseClick);
        startCropping();
    }

}

package com.ami.icamdocscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ami.icamdocscanner.helpers.ScannerConstants;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.libraries.NativeClass;
import com.ami.icamdocscanner.libraries.PolygonView;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ImageCropActivity extends AppCompatActivity {

    private FrameLayout holderImageCrop;
    private ImageView imageView;
    private PolygonView polygonView;
    private ProgressBar progressBar;

    protected CompositeDisposable disposable = new CompositeDisposable();
    private Bitmap selectedImage;
    private NativeClass nativeClass = new NativeClass();

    private OnClickListener btnImageEnhanceClick = v -> {
        showProgressBar();
        toEditImage();
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
        selectedImage = ScannerConstants.selectedImageBitmap;
        initView();
    }

    private void showProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, false);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, true);
        progressBar.setVisibility(View.GONE);
    }

    private void showError() {
        Toast.makeText(this, ScannerConstants.cropError, Toast.LENGTH_LONG).show();
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

    private void setImageRotation() {
        Bitmap tempBitmap = selectedImage.copy(selectedImage.getConfig(), true);
        for (int i = 1; i <= 4; i++) {
            MatOfPoint2f point2f = nativeClass.getPoint(tempBitmap);
            if (point2f == null) {
                tempBitmap = rotateBitmap(tempBitmap);
            } else {
                selectedImage = tempBitmap.copy(selectedImage.getConfig(), true);
                break;
            }
        }
    }

    private Bitmap rotateBitmap(Bitmap source) {
        return  source;
//        Matrix matrix = new Matrix();
//        matrix.postRotate(angle);
//        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void setProgressBar(boolean isShow) {
        if (isShow)
            showProgressBar();
        else
            hideProgressBar();
    }

    private void startCropping() {
        setProgressBar(true);
        disposable.add(Observable.fromCallable(() -> {
                    setImageRotation();
                    return false;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((result) -> {
                            initializeCropping();
                            setProgressBar(false);
                        })
        );
    }


    private void initializeCropping() {
        Bitmap scaledBitmap = VisionUtils.scaledBitmap(selectedImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
        imageView.setImageBitmap(scaledBitmap);

        Bitmap tempBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

        Map<Integer, PointF> pointFs;
        try {
            pointFs = getEdgePoints(tempBitmap);

            polygonView.setPoints(pointFs);
            polygonView.setVisibility(View.VISIBLE);

            int padding = (int) getResources().getDimension(R.dimen.scanPadding);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
            layoutParams.gravity = Gravity.CENTER;

            polygonView.setLayoutParams(layoutParams);
            polygonView.setPointColor(getResources().getColor(R.color.orange));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap getCroppedImage() {
        try {
            Map<Integer, PointF> points = polygonView.getPoints();

            float xRatio = (float) selectedImage.getWidth() / imageView.getWidth();
            float yRatio = (float) selectedImage.getHeight() / imageView.getHeight();

            float x1 = (Objects.requireNonNull(points.get(0)).x) * xRatio;
            float x2 = (Objects.requireNonNull(points.get(1)).x) * xRatio;
            float x3 = (Objects.requireNonNull(points.get(2)).x) * xRatio;
            float x4 = (Objects.requireNonNull(points.get(3)).x) * xRatio;
            float y1 = (Objects.requireNonNull(points.get(0)).y) * yRatio;
            float y2 = (Objects.requireNonNull(points.get(1)).y) * yRatio;
            float y3 = (Objects.requireNonNull(points.get(2)).y) * yRatio;
            float y4 = (Objects.requireNonNull(points.get(3)).y) * yRatio;
            return nativeClass.getScannedBitmap(selectedImage, x1, y1, x2, y2, x3, y3, x4, y4);
        } catch (Exception e) {
            showError();
            return null;
        }
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        List<PointF> pointFs = getContourEdgePoints();
        return orderedValidEdgePoints(tempBitmap, pointFs);
    }

    private List<PointF> getContourEdgePoints() {
        MatOfPoint2f point2f = ScannerConstants.croppedPolygon;
        if (point2f == null)
            point2f = new MatOfPoint2f();
        List<Point> points = Arrays.asList(point2f.toArray());
        List<PointF> result = new ArrayList<>();

        float kx = (float) holderImageCrop.getWidth()/selectedImage.getWidth();
        float ky = (float) holderImageCrop.getHeight()/selectedImage.getHeight();
        float k = (Math.min(kx, ky));

        for (int i = 0; i < points.size(); i++) {
            result.add(new PointF(((float) points.get(i).x*k), ((float) points.get(i).y*k)));
        }
        return result;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);
        if (!polygonView.isValidShape(orderedPoints)) {
            orderedPoints = VisionUtils.getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposable.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }

}

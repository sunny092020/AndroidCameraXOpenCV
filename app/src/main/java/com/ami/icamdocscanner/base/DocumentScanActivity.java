package com.ami.icamdocscanner.base;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.helpers.ScannerConstants;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.libraries.NativeClass;
import com.ami.icamdocscanner.libraries.PolygonView;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public abstract class DocumentScanActivity extends AppCompatActivity {

    protected CompositeDisposable disposable = new CompositeDisposable();
    private Bitmap selectedImage;
    private NativeClass nativeClass = new NativeClass();

    protected abstract FrameLayout getHolderImageCrop();

    protected abstract ImageView getImageView();

    protected abstract PolygonView getPolygonView();

    protected abstract void showProgressBar();

    protected abstract void hideProgressBar();

    protected abstract void showError();

    protected abstract Bitmap getBitmapImage();

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

    protected Bitmap rotateBitmap(Bitmap source) {
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

    protected void startCropping() {
        selectedImage = getBitmapImage();
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
        Bitmap scaledBitmap = VisionUtils.scaledBitmap(selectedImage, getHolderImageCrop().getWidth(), getHolderImageCrop().getHeight());
        getImageView().setImageBitmap(scaledBitmap);

        Bitmap tempBitmap = ((BitmapDrawable) getImageView().getDrawable()).getBitmap();

        Map<Integer, PointF> pointFs;
        try {
            pointFs = getEdgePoints(tempBitmap);

            getPolygonView().setPoints(pointFs);
            getPolygonView().setVisibility(View.VISIBLE);

            int padding = (int) getResources().getDimension(R.dimen.scanPadding);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
            layoutParams.gravity = Gravity.CENTER;

            getPolygonView().setLayoutParams(layoutParams);
            getPolygonView().setPointColor(getResources().getColor(R.color.orange));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Bitmap getCroppedImage() {
        try {
            Map<Integer, PointF> points = getPolygonView().getPoints();

            float xRatio = (float) selectedImage.getWidth() / getImageView().getWidth();
            float yRatio = (float) selectedImage.getHeight() / getImageView().getHeight();

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

        float kx = (float) getHolderImageCrop().getWidth()/selectedImage.getWidth();
        float ky = (float) getHolderImageCrop().getHeight()/selectedImage.getHeight();
        float k = (Math.min(kx, ky));

        for (int i = 0; i < points.size(); i++) {
            result.add(new PointF(((float) points.get(i).x*k), ((float) points.get(i).y*k)));
        }
        return result;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = getPolygonView().getOrderedPoints(pointFs);
        if (!getPolygonView().isValidShape(orderedPoints)) {
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

package com.journaldev.androidcameraxopencv;

import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Camera;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.journaldev.androidcameraxopencv.enums.ScanHint;
import com.journaldev.androidcameraxopencv.helpers.ImageUtils;
import com.journaldev.androidcameraxopencv.helpers.ScannerConstants;
import com.journaldev.androidcameraxopencv.libraries.Line;
import com.journaldev.androidcameraxopencv.libraries.LinePolar;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static android.view.View.GONE;
import static java.lang.Integer.min;
import static java.lang.Integer.max;
import static java.lang.Math.abs;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    PreviewView previewView;
    ImageView ivBitmap;
    FrameLayout frameLayout;
    ImageCapture imageCapture;
    ImageAnalysis imageAnalysis;
    Preview preview;
    Camera camera;

    FloatingActionButton btnCapture, btnOk, btnCancel;

    private TextView captureHintText;
    private LinearLayout captureHintLayout;

    Bitmap overlay;

    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnCapture = findViewById(R.id.btnCapture);
        btnOk = findViewById(R.id.btnAccept);
        btnCancel = findViewById(R.id.btnReject);

        btnOk.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        ivBitmap = findViewById(R.id.ivBitmap);

        captureHintLayout = findViewById(R.id.capture_hint_layout);
        captureHintText = findViewById(R.id.capture_hint_text);

        previewView = findViewById(R.id.preview_view);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                preview = setPreview();
                imageCapture = setImageCapture();
                imageAnalysis = setImageAnalysis();

                // Select back camera
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Attach use cases to the camera with the same lifecycle owner
                camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis);

                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(
                        previewView.createSurfaceProvider());

                frameLayout = findViewById(R.id.frameLayout);

                // Gets the layout params that will allow you to resize the layout
                ViewGroup.LayoutParams params = frameLayout.getLayoutParams();

                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int width = displayMetrics.widthPixels;

                // Changes the height and width to the specified *pixels*
                params.width = width;
                params.height = width*4/3;

                frameLayout.setLayoutParams(params);

            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get() should
                // not block since the listener is being called, so no need to
                // handle InterruptedException.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Preview setPreview() {
        DisplayMetrics metrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(metrics);
        int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);

        int rotation = previewView.getDisplay().getRotation();

        Preview.Builder previewBuilder = new Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation);

        return previewBuilder.build();
    }

    private ImageCapture setImageCapture() {
        DisplayMetrics metrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(metrics);
        int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);

        int rotation = previewView.getDisplay().getRotation();

        ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation);
        final ImageCapture imgCapture = imageCaptureBuilder.build();

        btnCapture.setOnClickListener(v -> {});

        return imgCapture;
    }


    private int aspectRatio(int width, int height) {
        double RATIO_4_3_VALUE = 4.0 / 3.0;
        double RATIO_16_9_VALUE = 16.0 / 9.0;

        double previewRatio = (double) max(width, height) / min(width, height);
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    private ImageAnalysis setImageAnalysis() {
        ImageAnalysis.Builder imageAnalysisBuilder = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setImageQueueDepth(1);

        ImageAnalysis imageAnalysis = imageAnalysisBuilder.build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(),
            (image) -> {
                analyze(image);
            });

        return imageAnalysis;
    }

    private void analyze(ImageProxy image) {
        if(ScannerConstants.captured_finish) {
            image.close();
            return;
        }

        Bitmap bitmap = previewView.getBitmap();
        findContours(bitmap);

        runOnUiThread(() -> {
            displayHint(ScannerConstants.scanHint);
            ivBitmap.setImageBitmap(overlay);
        });

        if(!ScannerConstants.analyzing) {
            image.close();
            return;
        }

        if(ScannerConstants.scanHint == ScanHint.CAPTURING_IMAGE) {
            ScannerConstants.analyzing = false;
            new Handler(Looper.getMainLooper()).post(() -> new CountDownTimer(3000, 100) {
                public void onTick(long millisUntilFinished) {}
                public void onFinish() {
                    ScannerConstants.captured_finish = true;
                    takePicture(image);
                }
            }.start());
        }

        image.close();
    }

    private void takePicture(ImageProxy image) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CAPTURE.jpg");
        ImageCapture.OutputFileOptions.Builder outputFileOptionsBuilder =
                new ImageCapture.OutputFileOptions.Builder(file);

        imageCapture.takePicture(outputFileOptionsBuilder.build(), Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback(){
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Bitmap previewBitmap = previewView.getBitmap();

                File mSaveBit = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CAPTURE.jpg");
                String filePath = mSaveBit.getPath();
                Bitmap captureBitmap = BitmapFactory.decodeFile(filePath);

                mSaveBit.delete();

                double h = (double) previewBitmap.getHeight()*captureBitmap.getHeight()/previewBitmap.getWidth();
                Bitmap croppedBmp = Bitmap.createBitmap(captureBitmap, (int) ((captureBitmap.getWidth()-h)/2), 0, (int) h, captureBitmap.getHeight());
                Bitmap rotated90croppedBmp = ImageUtils.rotateBitmap(croppedBmp, 90);


                MatOfPoint2f contour = findContours(previewBitmap);

                if(contour == null) {
                    image.close();
                    ScannerConstants.captured_finish = false;
                    ScannerConstants.analyzing = true;
                    return;
                }

                double scaleX = (double) rotated90croppedBmp.getWidth()/previewBitmap.getWidth();
                double scaleY = (double) rotated90croppedBmp.getHeight()/previewBitmap.getHeight();

                MatOfPoint2f scaleContour = ImageUtils.scaleContour(contour, scaleX, scaleY);

                ScannerConstants.selectedImageBitmap = rotated90croppedBmp;
                ScannerConstants.croptedPolygon = scaleContour;
                image.close();
                startCrop();
            }
            public void onError(@NotNull ImageCaptureException exception) {}
        });
    }

    private MatOfPoint2f findContours(Bitmap bitmap) {
        clearPoints();

        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        /* get four outline edges of the document */
        // get edges of the image
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);

        Mat hsv = new Mat();
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_RGB2HSV);

        List<Mat> channels = new ArrayList<>();

        Core.split(hsv, channels);

        Mat H = channels.get(0);
        Mat S = channels.get(1);
        Mat V = channels.get(2);

        Mat notGray = new Mat();
        Mat notH = new Mat();
        Mat notS = new Mat();
        Mat notV = new Mat();

        Core.bitwise_not(gray, notGray);
        Core.bitwise_not(H, notH);
        Core.bitwise_not(S, notS);
        Core.bitwise_not(V, notV);

        Mat[] inputMats = {gray, H, S, V, notGray, notH, notS, notV};

        MatOfPoint2f contour = ImageUtils.coverAllMethods4Contours(inputMats);

        if(contour == null) return null;

        drawPoint(contour.toList());

        ScannerConstants.scanHint = ScanHint.CAPTURING_IMAGE;
        return contour;
    }

    public void displayHint(ScanHint scanHint) {
        captureHintLayout.setVisibility(View.VISIBLE);
        switch (scanHint) {
            case MOVE_CLOSER:
                captureHintText.setText(getResources().getString(R.string.move_closer));
                captureHintLayout.setBackground(getResources().getDrawable(R.drawable.hint_white));
                break;
            case MOVE_AWAY:
                captureHintText.setText(getResources().getString(R.string.move_away));
                captureHintLayout.setBackground(getResources().getDrawable(R.drawable.hint_red));
                break;
            case ADJUST_ANGLE:
                captureHintText.setText(getResources().getString(R.string.adjust_angle));
                captureHintLayout.setBackground(getResources().getDrawable(R.drawable.hint_red));
                break;
            case FIND_RECT:
                captureHintText.setText(getResources().getString(R.string.finding_rect));
                captureHintLayout.setBackground(getResources().getDrawable(R.drawable.hint_white));
                break;
            case CAPTURING_IMAGE:
                captureHintText.setText(getResources().getString(R.string.hold_still));
                captureHintLayout.setBackground(getResources().getDrawable(R.drawable.hint_green));
                break;
            case NO_MESSAGE:
                captureHintLayout.setVisibility(GONE);
                break;
            default:
                break;
        }
    }

    private void clearPoints() {
        overlay = Bitmap.createBitmap(previewView.getWidth(), previewView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }

    private  void drawPoint(List<Point> points) {
        int paintColor = Color.RED;

        // Setup paint with color and stroke styles
        Paint drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(5);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        overlay = Bitmap.createBitmap(previewView.getWidth(), previewView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);

        for (Point p : points) {
            canvas.drawCircle((float) p.x, (float)p.y, 30, drawPaint);
        }
    }

    private void startCrop() {
        Intent cropIntent = new Intent(this, ImageCropActivity.class);
        startActivityForResult(cropIntent, 1234);
        finish();
    }

    private void showAcceptedRejectedButton() {}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnReject:
                showAcceptedRejectedButton();
                break;

            case R.id.btnAccept:
                break;
        }
    }

}

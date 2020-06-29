package com.ami.icamdocscanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ami.icamdocscanner.enums.ScanHint;
import com.ami.icamdocscanner.helpers.MathUtils;
import com.ami.icamdocscanner.helpers.Preferences;
import com.ami.icamdocscanner.helpers.ScannerConstants;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static android.view.View.GONE;
import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static java.lang.Math.abs;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[] {
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

    ImageButton btnCapture, btnAutoCapture;

    private TextView captureHintText;
    Context context;

    public Bitmap overlay;
    public Paint fillPaint, strokePaint;

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
        btnCapture.setOnClickListener(v -> {
            setAutoFocus();
            takePicture();
        });

        btnAutoCapture = findViewById(R.id.btnAutoCapture);

        context = this;
        btnAutoCapture.setOnClickListener(v -> {
            Activity activity = (Activity) context;
            if(Preferences.getAutoCapture(activity)) {
                btnAutoCapture.setImageResource(R.drawable.ic_auto_disable);
                Preferences.setAutoCapture(activity, false);
                displayHint("Auto capture: Off");
            } else {
                btnAutoCapture.setImageResource(R.drawable.ic_auto_enable);
                Preferences.setAutoCapture(activity, true);
                displayHint("Auto capture: On");
            }
        });

        ivBitmap = findViewById(R.id.ivBitmap);
        captureHintText = findViewById(R.id.capture_hint_text);
        previewView = findViewById(R.id.preview_view);

        if(Preferences.getAutoCapture(this)) {
            btnAutoCapture.setImageResource(R.drawable.ic_auto_enable);
        } else {
            btnAutoCapture.setImageResource(R.drawable.ic_auto_disable);
        }

        setupPaint();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void setupPaint() {
        // Setup paint with color and stroke styles
        fillPaint = new Paint();
        fillPaint.setColor(Color.parseColor("#ff59a9ff"));
        fillPaint.setStyle(Paint.Style.FILL);

        strokePaint = new Paint();
        strokePaint.setColor(Color.parseColor("#FFFFFF"));
        strokePaint.setStrokeWidth(5);
        strokePaint.setStyle(Paint.Style.STROKE);
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

                setFrameLayoutRatio();
            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get() should
                // not block since the listener is being called, so no need to
                // handle InterruptedException.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setAutoFocus() {
        float width = (float) previewView.getWidth();
        float height = (float)previewView.getHeight();

        float centerWidth = width / 2;
        float centerHeight = height / 2;

        MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(
                width, height);

        MeteringPoint centerPoint = factory.createPoint(centerWidth, centerHeight, 1);
        camera.getCameraControl().startFocusAndMetering(
            new FocusMeteringAction.Builder(
                    centerPoint,
                    FocusMeteringAction.FLAG_AF
            ).build());
    }

    private void setFrameLayoutRatio() {
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
    }

    private Preview setPreview() {
        Preview.Builder previewBuilder = new Preview.Builder()
                .setTargetAspectRatio(aspectRatio())
                .setTargetRotation(previewView.getDisplay().getRotation());

        return previewBuilder.build();
    }

    private ImageCapture setImageCapture() {
        ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetAspectRatio(aspectRatio())
                .setTargetRotation(previewView.getDisplay().getRotation());
        return imageCaptureBuilder.build();
    }

    private int aspectRatio() {
        DisplayMetrics metrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(metrics);

        int width = metrics.widthPixels, height = metrics.heightPixels;

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
                this::analyze);

        return imageAnalysis;
    }

    private void analyze(ImageProxy image) {
        if(!Preferences.getAutoCapture(this)) {
            image.close();
            runOnUiThread(() -> {
                displayHint(ScanHint.NO_MESSAGE);
                clearPoints();
                ivBitmap.setImageBitmap(overlay);
            });
            return;
        }

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
            setAutoFocus();
            new Handler(Looper.getMainLooper()).post(() -> new CountDownTimer(3000, 100) {
                public void onTick(long millisUntilFinished) {}
                public void onFinish() {
                    image.close();
                    ScannerConstants.captured_finish = true;
                    if(!Preferences.getAutoCapture((Activity) context)) {
                        ScannerConstants.captured_finish = false;
                        ScannerConstants.analyzing = true;
                        return;
                    }
                    takePicture();
                }
            }.start());
        }

        image.close();
    }

    // TODO: multiple threads call this function concurrently causes error
    private void takePicture() {
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
                Bitmap rotated90croppedBmp = VisionUtils.rotateBitmap(croppedBmp, 90);

                MatOfPoint2f contour = findContours(rotated90croppedBmp);

                if(contour == null) {
                    ScannerConstants.captured_finish = false;
                    ScannerConstants.analyzing = true;
                    return;
                }

                ScannerConstants.selectedImageBitmap = rotated90croppedBmp;
                ScannerConstants.croptedPolygon = contour;
                startCrop();
            }
            public void onError(@NotNull ImageCaptureException exception) {}
        });
    }

    private MatOfPoint2f findContours(Bitmap bitmap) {

        clearPoints();

        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        double DOWNSCALE_IMAGE_SIZE = 600f;

        // Downscale image for better performance.
        double ratio = DOWNSCALE_IMAGE_SIZE / Math.max(src.width(), src.height());

        Mat mat = VisionUtils.downscaleMat(src, ratio);

        /* get four outline edges of the document */
        // get edges of the image
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);

        Mat hsv = new Mat();
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_RGB2HSV);
        mat.release();

        List<Mat> channels = new ArrayList<>();

        Core.split(hsv, channels);
        hsv.release();

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

        MatOfPoint2f contour = VisionUtils.coverAllMethods4Contours(inputMats, this);

        for(Mat inputMat: inputMats) inputMat.release();

        if(contour == null) return null;

        MatOfPoint2f upScaleContour = MathUtils.scaleRectangle(contour, 1f / ratio);

        drawPoint(upScaleContour.toList());

        ScannerConstants.scanHint = ScanHint.CAPTURING_IMAGE;
        return upScaleContour;
    }

    public void displayHint(String text) {
        captureHintText.setVisibility(View.VISIBLE);
        captureHintText.setText(text);
    }

    public void displayHint(ScanHint scanHint) {
        captureHintText.setVisibility(View.VISIBLE);
        switch (scanHint) {
            case MOVE_CLOSER:
                captureHintText.setText(getResources().getString(R.string.move_closer));
                break;
            case MOVE_AWAY:
                captureHintText.setText(getResources().getString(R.string.move_away));
                break;
            case ADJUST_ANGLE:
                captureHintText.setText(getResources().getString(R.string.adjust_angle));
                break;
            case FIND_RECT:
                captureHintText.setText(getResources().getString(R.string.finding_rect));
                break;
            case CAPTURING_IMAGE:
                captureHintText.setText(getResources().getString(R.string.hold_still));
                break;
            case NO_MESSAGE:
                captureHintText.setVisibility(GONE);
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
        overlay = Bitmap.createBitmap(previewView.getWidth(), previewView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);

        for (Point p : points) {
            canvas.drawCircle((float) p.x, (float)p.y, 30, fillPaint);
            canvas.drawCircle((float) p.x, (float)p.y, 30, strokePaint);
        }
    }

    private void startCrop() {
        Intent cropIntent = new Intent(this, ImageCropActivity.class);
        startActivityForResult(cropIntent, 1234);
        finish();
    }

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
    public void onClick(View v) {}

}

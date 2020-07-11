package com.ami.icamdocscanner.activities;

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
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.Preferences;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

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
    ImageView ivBitmap, capturedView;
    FrameLayout frameLayout;
    ImageCapture imageCapture;
    ImageAnalysis imageAnalysis;
    Preview preview;
    Camera camera;

    private TextView captureHintText;
    Context context;

    public Bitmap overlay;
    public Paint fillPaint, strokePaint;

    private int batchNum = 0;
    int currentImagePosition = -1;
    boolean add = false;

    private static long lastManualCaptureTime, lastAutoCaptureTime;

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

        context = this;

        setupButtons();
        deleteTempDir();

        ivBitmap = findViewById(R.id.ivBitmap);
        capturedView = findViewById(R.id.capturedView);

        captureHintText = findViewById(R.id.capture_hint_text);
        previewView = findViewById(R.id.preview_view);

        setupPaint();

        resetCaptureTime();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void setupButtons() {
        ImageButton btnCapture = findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(v -> {
            // preventing double, using threshold of 1000 ms
            if (lastManualCaptureEarly()){
                return;
            }
            markManualCaptureTime();
            setAutoFocus();
            takePictureManual();
        });

        ImageButton btnAutoCapture = findViewById(R.id.btnAutoCapture);
        btnAutoCapture.setOnClickListener(v -> {
            Activity activity = (Activity) context;
            // set text to "" to make way for other hint
            if(Preferences.getAutoCapture(activity)) {
                btnAutoCapture.setImageResource(R.drawable.ic_auto_disable);
                Preferences.setAutoCapture(activity, false);
                displayHint("Auto capture: Off");
                resetManualCaptureTime();
            } else {
                btnAutoCapture.setImageResource(R.drawable.ic_auto_enable);
                Preferences.setAutoCapture(activity, true);
                displayHint("Auto capture: On");
            }
            // make "Auto capture: On/Off" last 1 seconds
            new Handler().postDelayed(() -> {
                // set text to "" to make way for other hint
                captureHintText.setText("");
                captureHintText.setVisibility(View.INVISIBLE);
            }, 1000);
        });

        if(Preferences.getAutoCapture(this)) {
            btnAutoCapture.setImageResource(R.drawable.ic_auto_enable);
        } else {
            btnAutoCapture.setImageResource(R.drawable.ic_auto_disable);
        }

        RelativeLayout btnBatchThumbnails = findViewById(R.id.batchThumbnailsHolder);

        // for retake image
        currentImagePosition =  getIntent().getIntExtra("currentImagePosition", -1);
        if(currentImagePosition == -1) {
            btnBatchThumbnails.setOnClickListener(v -> {
                Activity activity = (Activity) context;
                // set text to "" to make way for other hint
                startCropActivity();
            });
        }

        // add more images
        add = getIntent().getBooleanExtra("add", false);
        if(add) {
            File tempDir = new File(FileUtils.tempDir(this));
            List<RecyclerImageFile> files = FileUtils.listFilesByName(tempDir);

            RecyclerImageFile latestFile = files.get(files.size() -1);

            Bitmap latestFileBitmap = FileUtils.readBitmap(latestFile.getAbsolutePath());
            int DOWNSCALE_IMAGE_SIZE = 80;
            Bitmap smallOriginBitmap = VisionUtils.scaledBitmap(latestFileBitmap, DOWNSCALE_IMAGE_SIZE, DOWNSCALE_IMAGE_SIZE);

            ImageView batchThumbnails = findViewById(R.id.batchThumbnails);
            batchThumbnails.setImageBitmap(smallOriginBitmap);
            batchNum=files.size();

            TextView batchNumTxt = findViewById(R.id.batchNum);
            batchNumTxt.setText(String.format(Locale.US, "%d", batchNum));
            batchNumTxt.setVisibility(View.VISIBLE);
        }
    }

    private void deleteTempDir() {
        // for retake image
        currentImagePosition =  getIntent().getIntExtra("currentImagePosition", -1);
        add = getIntent().getBooleanExtra("add", false);

        if(add) return;
        if(currentImagePosition>=0) return;
        FileUtils.deleteTempDir(this);
    }

    private void freezePreview(Bitmap lastFrameBitmap) {
        previewView.setVisibility(View.INVISIBLE);
        ivBitmap.setVisibility(View.INVISIBLE);
        capturedView.setVisibility(View.VISIBLE);
        capturedView.setImageBitmap(lastFrameBitmap);
    }

    private void unfreezePreview() {
        previewView.setVisibility(View.VISIBLE);
        ivBitmap.setVisibility(View.VISIBLE);
        capturedView.setVisibility(View.INVISIBLE);
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

        cameraProviderFuture.addListener(() -> previewView.post(() -> {
            // Camera provider is now guaranteed to be available
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

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

        }), ContextCompat.getMainExecutor(this));
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

        Display display = previewView.getDisplay();
        display.getRealMetrics(metrics);

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
                // clearPoints draw canvas, so we need to set overlay to ivBitmap
                clearPoints();
                ivBitmap.setImageBitmap(overlay);
            });
            return;
        }

        Bitmap bitmap=null;
        try {
            bitmap= previewView.getBitmap();
        } catch (Exception e) {
            // some time surface invalid error happened
            e.printStackTrace();
        }

        // sometimes, it continues to find contours when transiting
        // to other screen, so bitmap is null
        if(bitmap== null) {
            image.close();
            return;
        }

        MatOfPoint2f contour = VisionUtils.findContours(bitmap, this);

        runOnUiThread(() -> {
            if(contour==null) {
                clearPoints();
                displayHint("No document");
            } else {
                drawPoint(contour.toList());
                displayHint("Hold firmly. Capturing image...");
            }
            // drawPoint draw canvas, so we need to set overlay to ivBitmap
            ivBitmap.setImageBitmap(overlay);
        });

        // preventing double, using threshold of 1000 ms
        if (lastCaptureEarly()) {
            image.close();
            return;
        }

        if(Preferences.getAutoCapture((Activity) context)) {
            markAutoCaptureTime();
        }

        setAutoFocus();
        new Handler(Looper.getMainLooper()).post(() -> new CountDownTimer(2000, 100) {
            public void onTick(long millisUntilFinished) {
                if(lastManualCaptureEarly()) cancel();
            }
            public void onFinish() {
                if(!Preferences.getAutoCapture((Activity) context)) return;
                takePicture();
            }
        }.start());
        image.close();
    }

    private boolean lastManualCaptureEarly() {
        return (SystemClock.elapsedRealtime() - lastManualCaptureTime) <= 12000000;
    }

    private boolean lastCaptureEarly() {
        return (((SystemClock.elapsedRealtime() - lastAutoCaptureTime) <= 12000000)
                || ((SystemClock.elapsedRealtime() - lastManualCaptureTime) <= 12000000));
    }

    private void markAutoCaptureTime() {
        lastAutoCaptureTime = SystemClock.elapsedRealtime();
    }

    private void markManualCaptureTime() {
        lastManualCaptureTime = SystemClock.elapsedRealtime();
    }

    private void resetManualCaptureTime() {
        lastManualCaptureTime = 0;
    }

    private void resetCaptureTime() {
        lastManualCaptureTime = 0;
        lastAutoCaptureTime = 0;
    }

    private void takePicture() {
        File capturedImg = new File(context.getFilesDir(), "CAPTURE.jpg");
        ImageCapture.OutputFileOptions.Builder outputFileOptionsBuilder =
                new ImageCapture.OutputFileOptions.Builder(capturedImg);

        imageCapture.takePicture(outputFileOptionsBuilder.build(), Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Bitmap previewBitmap = previewView.getBitmap();
                int previewBitmapW = previewBitmap.getWidth(), previewBitmapH = previewBitmap.getHeight();
                Bitmap rotated90croppedBmp = cropCapturedImage(capturedImg, previewBitmapW, previewBitmapH);
                MatOfPoint2f contour = VisionUtils.findContours(rotated90croppedBmp, (Activity) context);

                if(contour == null) {
                    resetCaptureTime();
                    return;
                }
                batchModeCapture(previewBitmap, rotated90croppedBmp);
            }
            public void onError(@NotNull ImageCaptureException exception) {}
        });
    }

    private void takePictureManual() {
        File capturedImg = new File(context.getFilesDir(), "CAPTURE_MANUAL.jpg");
        ImageCapture.OutputFileOptions.Builder outputFileOptionsBuilder =
                new ImageCapture.OutputFileOptions.Builder(capturedImg);

        imageCapture.takePicture(outputFileOptionsBuilder.build(), Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Bitmap previewBitmap = previewView.getBitmap();
                int previewBitmapW = previewBitmap.getWidth(), previewBitmapH = previewBitmap.getHeight();
                Bitmap rotated90croppedBmp = cropCapturedImage(capturedImg, previewBitmapW, previewBitmapH);
                batchModeCapture(previewBitmap, rotated90croppedBmp);
            }
            public void onError(@NotNull ImageCaptureException exception) {}
        });
    }

    private Bitmap cropCapturedImage(File file, int previewBitmapW, int previewBitmapH) {
        String filePath = file.getPath();
        Bitmap captureBitmap = BitmapFactory.decodeFile(filePath);
        file.delete();

        double h = (double) previewBitmapH*captureBitmap.getHeight()/previewBitmapW;
        Bitmap croppedBmp = Bitmap.createBitmap(captureBitmap, (int) ((captureBitmap.getWidth()-h)/2), 0, (int) h, captureBitmap.getHeight());
        return  VisionUtils.rotateBitmap(croppedBmp, 90);
    }

    public void displayHint(String text) {
        if(text.equalsIgnoreCase("Auto capture: Off")) {
            captureHintText.setVisibility(View.VISIBLE);
            captureHintText.setText(text);
            return;
        }
        if(text.equalsIgnoreCase("Auto capture: On")) {
            captureHintText.setVisibility(View.VISIBLE);
            captureHintText.setText(text);
            return;
        }
        if(captureHintText.getText() == "Auto capture: Off") return;
        if(captureHintText.getText() == "Auto capture: On") return;
        captureHintText.setVisibility(View.VISIBLE);
        captureHintText.setText(text);
    }

    private void clearPoints() {
        overlay = Bitmap.createBitmap(previewView.getWidth(), previewView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }

    private void drawPoint(List<Point> points) {
        overlay = Bitmap.createBitmap(previewView.getWidth(), previewView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);

        for (Point p : points) {
            canvas.drawCircle((float) p.x, (float)p.y, 30, fillPaint);
            canvas.drawCircle((float) p.x, (float)p.y, 30, strokePaint);
        }
    }

    private void batchModeCapture(Bitmap previewBitmap, Bitmap rotated90croppedBmp) {
        runOnUiThread(() -> freezePreview(previewBitmap));
        // retake existing capture
        if(currentImagePosition >= 0) {
            String fileName = FileUtils.tempDir(this) + currentImagePosition + ".jpg";
            FileUtils.writeBitmap(rotated90croppedBmp, fileName);

            RecyclerImageFile file = new RecyclerImageFile(fileName);
            ScannerState.updateCroppedPolygon(file, ScannerState.cropImages);

            Intent cropIntent = new Intent(this, ImageCropActivity.class);
            cropIntent.putExtra("currentImagePosition", currentImagePosition);
            startActivity(cropIntent);
            finish();
            return;
        }

        String fileName = FileUtils.tempDir(this) + batchNum + ".jpg";

        RecyclerImageFile file = new RecyclerImageFile(fileName);
        ScannerState.cropImages.add(file);

        FileUtils.ensureTempDir(this);
        FileUtils.writeBitmap(rotated90croppedBmp, fileName);

        if(Preferences.getIsCropAfterEachCapture(this)) {
            startCropActivity();
            return;
        }

        int DOWNSCALE_IMAGE_SIZE = 80;
        Bitmap smallOriginBitmap = VisionUtils.scaledBitmap(rotated90croppedBmp, DOWNSCALE_IMAGE_SIZE, DOWNSCALE_IMAGE_SIZE);

        runOnUiThread(() -> {
            ImageView batchThumbnails = findViewById(R.id.batchThumbnails);
            batchThumbnails.setImageBitmap(smallOriginBitmap);
            batchNum++;
            TextView batchNumTxt = findViewById(R.id.batchNum);
            batchNumTxt.setText(String.format(Locale.US, "%d", batchNum));
            batchNumTxt.setVisibility(View.VISIBLE);
            resetCaptureTime();
            unfreezePreview();
        });
    }

    private void startCropActivity() {
        Intent cropIntent = new Intent(this, ImageCropActivity.class);
        startActivity(cropIntent);
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

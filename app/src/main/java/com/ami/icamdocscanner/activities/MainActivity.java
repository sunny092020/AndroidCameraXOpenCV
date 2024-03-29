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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.ami.icamdocscanner.helpers.ActivityUtils;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.Preferences;
import com.ami.icamdocscanner.helpers.ScannerConstant;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

        setupBottomAppBar();
        setupButtons();

        ivBitmap = findViewById(R.id.ivBitmap);
        capturedView = findViewById(R.id.capturedView);

        captureHintText = findViewById(R.id.capture_hint_text);
        previewView = findViewById(R.id.preview_view);

        setupPaint();

        resetCaptureTime();

        setupBottomAppBar();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void setupBottomAppBar() {
        BottomAppBar bottomAppBar = findViewById(R.id.bottonAppBar);

        //main line for setting bottomAppBar
        setSupportActionBar(bottomAppBar);

        bottomAppBar.setNavigationOnClickListener(item -> {
            FileUtils.deleteTempDir(context);
            ScannerState.resetScannerState();
            Intent doneIntent = new Intent(this, ImageDoneActivity.class);
            startActivity(doneIntent);
            finish();
        });
    }

    private void setupButtons() {
        FloatingActionButton btnCapture = findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(v -> {
            // preventing double, using threshold of 1000 ms
            if (lastManualCaptureEarly()){
                return;
            }
            markManualCaptureTime();
            takePictureManual();
        });

        RelativeLayout btnBatchThumbnails = findViewById(R.id.batchThumbnailsHolder);
        btnBatchThumbnails.setOnClickListener(v -> startCropActivity());

        // add more images
        boolean add = getIntent().getBooleanExtra("add", false);
        if(add) {
            List<RecyclerImageFile> files = ScannerState.getOriginImages();
            RecyclerImageFile latestFile = files.get(files.size() -1);

            Bitmap latestFileBitmap = FileUtils.readBitmap(latestFile.getAbsolutePath());
            int DOWNSCALE_IMAGE_SIZE = 80;
            Bitmap smallOriginBitmap = VisionUtils.scaledBitmap(latestFileBitmap, DOWNSCALE_IMAGE_SIZE, DOWNSCALE_IMAGE_SIZE);

            ImageView batchThumbnails = findViewById(R.id.batchThumbnails);
            batchThumbnails.setImageBitmap(smallOriginBitmap);

            TextView batchNumTxt = findViewById(R.id.batchNum);
            batchNumTxt.setText(String.format(Locale.US, "%d", ScannerState.getOriginImages().size()));
            batchNumTxt.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bottom_app_bar,menu);

        MenuItem btnAutoCapture = menu.findItem(R.id.btnAutoCapture);
        if(Preferences.getAutoCapture(this)) {
            btnAutoCapture.setIcon(R.drawable.ic_auto_enable);
        } else {
            btnAutoCapture.setIcon(R.drawable.ic_auto_disable);
        }

        btnAutoCapture.setOnMenuItemClickListener(item -> {
            Activity activity = (Activity) context;
            // set text to "" to make way for other hint
            if(Preferences.getAutoCapture(activity)) {
                btnAutoCapture.setIcon(R.drawable.ic_auto_disable);
                Preferences.setAutoCapture(activity, false);
                displayHint("Auto capture: Off");
                resetManualCaptureTime();
            } else {
                btnAutoCapture.setIcon(R.drawable.ic_auto_enable);
                Preferences.setAutoCapture(activity, true);
                displayHint("Auto capture: On");
            }
            // make "Auto capture: On/Off" last 1 seconds
            new Handler().postDelayed(() -> {
                // set text to "" to make way for other hint
                captureHintText.setText("");
                captureHintText.setVisibility(View.INVISIBLE);
            }, 1000);

            return false;
        });

        MenuItem btnChoosePhoto = menu.findItem(R.id.btnChoosePhoto);

        LinearLayout progressBarHolder = findViewById(R.id.progressBarHolder);
        progressBarHolder.setVisibility(View.GONE);

        btnChoosePhoto.setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            // The MIME data type filter
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            // Only return URIs that can be opened with ContentResolver
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, ScannerConstant.LAUNCH_FILE_PICKER);
            return false;
        });

        return true;
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ScannerConstant.LAUNCH_FILE_PICKER) {
            if(resultCode == Activity.RESULT_OK) {
                ActivityUtils.filePickerProcessResult(context, data);
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
                Log.d("LAUNCH_FILE_PICKER result", "cancel");
            }
        }
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
                    previewView.getSurfaceProvider());

            setFrameLayoutRatio();

            setFocusOnTab();

        }), ContextCompat.getMainExecutor(this));
    }

    private void setFocusOnTab() {
        previewView.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP) {
            /* Original post returns false here, but in my experience this makes
            onTouch not being triggered for ACTION_UP event */
                return true;
            }

            float width = (float) previewView.getWidth();
            float height = (float)previewView.getHeight();

            MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(
                    width, height);

            MeteringPoint point = factory.createPoint(event.getX(), event.getY());
            camera.getCameraControl().startFocusAndMetering(
                    new FocusMeteringAction.Builder(
                            point,
                            FocusMeteringAction.FLAG_AF
                    ).build());

            return true;
        });
    }

    private void setFrameLayoutRatio() {
        frameLayout = findViewById(R.id.frameLayout);

        // Gets the layout params that will allow you to resize the layout
        ViewGroup.LayoutParams params = frameLayout.getLayoutParams();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        // Changes the height and width to the specified *pixels*
        params.width = width;
        params.height = height;

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

        runOnUiThread(() -> {
            Bitmap bitmap=null;
            try {
                bitmap = previewView.getBitmap();
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

            MatOfPoint2f contour = VisionUtils.findContours(bitmap, (Activity) context);

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
        return (System.currentTimeMillis() - lastManualCaptureTime) <= 12000000;
    }

    private boolean lastCaptureEarly() {
        return (((System.currentTimeMillis() - lastAutoCaptureTime) <= 12000000)
                || ((System.currentTimeMillis() - lastManualCaptureTime) <= 12000000));
    }

    private void markAutoCaptureTime() {
        lastAutoCaptureTime = System.currentTimeMillis();
    }

    private void markManualCaptureTime() {
        lastManualCaptureTime = System.currentTimeMillis();
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
                runOnUiThread(() -> {
                    Bitmap previewBitmap = previewView.getBitmap();
                    Bitmap rotated90croppedBmp = cropCapturedImage(capturedImg);
                    MatOfPoint2f contour = VisionUtils.findContours(rotated90croppedBmp, (Activity) context);

                    if(contour == null) {
                        resetCaptureTime();
                        return;
                    }
                    batchModeCapture(previewBitmap, rotated90croppedBmp);
                });
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
                runOnUiThread(() -> {
                    Bitmap previewBitmap = previewView.getBitmap();
                    Bitmap rotated90croppedBmp = cropCapturedImage(capturedImg);
                    batchModeCapture(previewBitmap, rotated90croppedBmp);
                });
            }
            public void onError(@NotNull ImageCaptureException exception) {}
        });
    }

    private Bitmap cropCapturedImage(File file) {
        String filePath = file.getPath();
        Bitmap captureBitmap = BitmapFactory.decodeFile(filePath);
        int originW = captureBitmap.getWidth();
        int originH = captureBitmap.getHeight();
        if(originW > originH) {
            captureBitmap = VisionUtils.rotateBitmap(captureBitmap, 90);
        }

        return  captureBitmap;
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

        int currentImagePosition =  getIntent().getIntExtra("currentImagePosition", -1);

        // retake existing capture
        if(currentImagePosition >= 0) {
            RecyclerImageFile retakingFile = ScannerState.getOriginImages().get(currentImagePosition);
            FileUtils.writeBitmap(rotated90croppedBmp, retakingFile.getAbsolutePath());

            MatOfPoint2f contour = VisionUtils.findContours(rotated90croppedBmp, (Activity) context);
            if(contour == null) {
                int originW = rotated90croppedBmp.getWidth();
                int originH = rotated90croppedBmp.getHeight();
                contour = VisionUtils.dummyContour(originW, originH);
            }

            retakingFile.setCroppedPolygon(contour);
            retakingFile.setChanged(false);
            Bitmap croppedBitmap = VisionUtils.getCroppedImage(rotated90croppedBmp, contour);
            String editImageFilePath =  FileUtils.editImagePath(context, retakingFile.getName());
            String doneImageFilePath =  FileUtils.doneImagePath(context, retakingFile.getName());
            RecyclerImageFile editFile = new RecyclerImageFile(editImageFilePath);
            RecyclerImageFile doneFile = new RecyclerImageFile(doneImageFilePath);

            // in case of add a new scan
            if(!editFile.exists()) {
                ScannerState.getEditImages().add(editFile);
            }

            // in case of add a new scan
            if(!doneFile.exists()) {
                ScannerState.getDoneImages().add(doneFile);
            }

            FileUtils.writeBitmap(croppedBitmap, editImageFilePath);
            FileUtils.writeBitmap(croppedBitmap, doneImageFilePath);

            Intent cropIntent = new Intent(this, ImageCropActivity.class);
            cropIntent.putExtra("currentImagePosition", currentImagePosition);
            setResult(Activity.RESULT_OK,cropIntent);
            finish();
            return;
        }

        String fileName =  FileUtils.originImagePath(context, ScannerState.getOriginImages().size() + ".jpg");
        RecyclerImageFile file = new RecyclerImageFile(fileName);
        MatOfPoint2f contour = VisionUtils.findContours(rotated90croppedBmp, (Activity) context);
        if(contour == null) {
            int originW = rotated90croppedBmp.getWidth();
            int originH = rotated90croppedBmp.getHeight();
            contour = VisionUtils.dummyContour(originW, originH);
        }

        file.setCroppedPolygon(contour);
        ScannerState.getOriginImages().add(file);
        FileUtils.ensureTempDir(this);
        FileUtils.writeBitmap(rotated90croppedBmp, fileName);

        file.setChanged(false);
        Bitmap croppedBitmap = VisionUtils.getCroppedImage(rotated90croppedBmp, contour);
        String editImageFilePath =  FileUtils.editImagePath(context, file.getName());
        String doneImageFilePath =  FileUtils.doneImagePath(context, file.getName());
        ScannerState.getEditImages().add(new RecyclerImageFile(editImageFilePath));
        ScannerState.getDoneImages().add(new RecyclerImageFile(doneImageFilePath));
        FileUtils.writeBitmap(croppedBitmap, editImageFilePath);
        FileUtils.writeBitmap(croppedBitmap, doneImageFilePath);

        if(Preferences.getIsCropAfterEachCapture(this)) {
            startCropActivity();
            return;
        }

        int DOWNSCALE_IMAGE_SIZE = 80;
        Bitmap smallOriginBitmap = VisionUtils.scaledBitmap(rotated90croppedBmp, DOWNSCALE_IMAGE_SIZE, DOWNSCALE_IMAGE_SIZE);

        runOnUiThread(() -> {
            ImageView batchThumbnails = findViewById(R.id.batchThumbnails);
            batchThumbnails.setImageBitmap(smallOriginBitmap);
            TextView batchNumTxt = findViewById(R.id.batchNum);
            batchNumTxt.setText(String.format(Locale.US, "%d", ScannerState.getOriginImages().size()));
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
    public void onClick(View v) {}
}

package com.journaldev.androidcameraxopencv;

import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Menu;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.Camera;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.Preview.Builder;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.journaldev.androidcameraxopencv.enums.ScanHint;
import com.journaldev.androidcameraxopencv.helpers.ScannerConstants;
import com.journaldev.androidcameraxopencv.libraries.SimpleDrawingView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };
    PreviewView previewView;

    TextureView textureView;
    ImageView ivBitmap;
    LinearLayout llBottom;

    ImageCapture imageCapture;
    ImageAnalysis imageAnalysis;
    Preview preview;
    Camera camera;

    FloatingActionButton btnCapture, btnOk, btnCancel;

    private TextView captureHintText;
    private LinearLayout captureHintLayout;
    SimpleDrawingView simpleDrawingView;
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

        llBottom = findViewById(R.id.llBottom);
        textureView = findViewById(R.id.textureView);
        ivBitmap = findViewById(R.id.ivBitmap);

        captureHintLayout = findViewById(R.id.capture_hint_layout);
        captureHintText = findViewById(R.id.capture_hint_text);

        simpleDrawingView = findViewById(R.id.simpleDrawingView1);

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
                        ((LifecycleOwner) this),
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis);

                // Connect the preview use case to the previewView
                previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.TEXTURE_VIEW);
                preview.setSurfaceProvider(
                        previewView.createSurfaceProvider());

            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get() should
                // not block since the listener is being called, so no need to
                // handle InterruptedException.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Preview setPreview() {
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen
        Preview.Builder previewBuilder = new Preview.Builder().setTargetResolution(screen);
        Preview preview = previewBuilder.build();
        return preview;
    }

    private ImageCapture setImageCapture() {
        ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation());


        final ImageCapture imgCapture = imageCaptureBuilder.build();

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {}
        });

        return imgCapture;
    }

    private ImageAnalysis setImageAnalysis() {
        ImageAnalysis.Builder imageAnalysisBuilder = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setImageQueueDepth(1);

        ImageAnalysis imageAnalysis = imageAnalysisBuilder.build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(),
            (image) -> {
                Bitmap bitmap = previewView.getBitmap();
                findContours(bitmap);

                Log.d("after", "findContours");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayHint(ScannerConstants.scanHint);
                        ivBitmap.setImageBitmap(overlay);
                    }
                });

                if(!ScannerConstants.analyzing) {
                    image.close();
                    return;
                };

                if(ScannerConstants.scanHint == ScanHint.CAPTURING_IMAGE) {
                    ScannerConstants.analyzing = false;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            new CountDownTimer(3000, 100) {
                                public void onTick(long millisUntilFinished) {}
                                public void onFinish() {
                                    Bitmap bitmap = previewView.getBitmap();
                                    findContours(bitmap);

                                    image.close();
                                    startCrop();
                                }
                            }.start();
                        }
                    });
                }

                image.close();
            });

        return imageAnalysis;
    }

    private void findContours(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        Mat originMat = new Mat();
        Utils.bitmapToMat(bitmap, originMat);

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);

        // Preparing the kernel matrix object
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new  org.opencv.core.Size(5, 5));

        Imgproc.dilate(mat, mat, kernel);

        Imgproc.medianBlur(mat, mat, 1);

        Imgproc.adaptiveThreshold(mat, mat, 255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,11, 1);

        // increase contrast
        for(int i = 1; i<=3; i++) {
            Imgproc.equalizeHist(mat, mat);
        }

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchyMat = new Mat();

        Imgproc.findContours(mat, contours, hierarchyMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

//        Imgproc.drawContours(originMat, contours, -1, new Scalar(255,0,0));
//
//        overlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(originMat, overlay);

        Collections.sort(contours, AreaDescendingComparator);

        double minS = (mat.width()*mat.height())*0.5;

        ScannerConstants.scanHint = ScanHint.NO_MESSAGE;

        for(MatOfPoint c : contours){
            MatOfPoint2f contour = new MatOfPoint2f(c.toArray());

            double length  = Imgproc.arcLength(contour,true);
            Imgproc.approxPolyDP(contour, contour,0.02*length,true);

            // break loop if it is not quad
            if(contour.total() != 4) break;

            drawPoint(contour, bitmap);

            // break loop if points are in the edge of the frame
            if(isExceedMat(contour, mat)) break;

            // break loop if the document is too far from the phone
            if(Imgproc.contourArea(contour) < minS) {
                ScannerConstants.scanHint = ScanHint.MOVE_CLOSER;
                break;
            }

            ScannerConstants.scanHint = ScanHint.CAPTURING_IMAGE;
            ScannerConstants.selectedImageBitmap = bitmap;
            ScannerConstants.croptedPolygon = contour;

            break;
        }
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

    private boolean isExceedMat(MatOfPoint2f contour, Mat myMat) {
        List<Point> points = contour.toList();

        for(Point p : points){
            double rateX = p.x/myMat.width();
            double rateY = p.y/myMat.height();

            if (rateX < 0.01) return true;
            if (rateX > 0.99) return true;
            if (rateY < 0.01) return true;
            if (rateY > 0.99) return true;
        }
        return false;
    }

    private  void drawPoint(MatOfPoint2f contour, Bitmap bitmap) {
        List<Point> points = contour.toList();

        int paintColor = Color.RED;

        // Setup paint with color and stroke styles
        Paint drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(5);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        overlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
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

    private static Comparator<MatOfPoint> AreaDescendingComparator = new Comparator<MatOfPoint>() {
        public int compare(MatOfPoint m1, MatOfPoint m2) {
            double area1 = Imgproc.contourArea(m1);
            double area2 = Imgproc.contourArea(m2);
            if(area2>area1) return 1;
            else if (area2<area1) return -1;
            else return 0;
        }
    };

    private void showAcceptedRejectedButton(boolean acceptedRejected) {}

    private void updateTransform() {
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int) textureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float) rotationDgr, cX, cY);
        textureView.setTransform(mx);
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnReject:
                showAcceptedRejectedButton(false);
                break;

            case R.id.btnAccept:
                File file = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "" + System.currentTimeMillis() + "_JDCameraX.jpg");
                break;
        }
    }
}

package com.journaldev.androidcameraxopencv;

import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Camera;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.journaldev.androidcameraxopencv.enums.ScanHint;
import com.journaldev.androidcameraxopencv.helpers.ScannerConstants;

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
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static android.view.View.GONE;
import static java.lang.Math.abs;
import static java.lang.Math.min;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    PreviewView previewView;
    ImageView ivBitmap;
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
                        ((LifecycleOwner) this),
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis);

                // Connect the preview use case to the previewView
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
        Size screen = new Size(previewView.getWidth(), previewView.getHeight()); //size of the screen
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
                if(ScannerConstants.captured_finish) {
                    image.close();
                    return;
                };

                Bitmap bitmap = previewView.getBitmap();
                findContours(bitmap);

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
                                    ScannerConstants.captured_finish = true;
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

    Point computeIntersect(Line l1, Line l2) {
        double x1 = l1._p1.x, x2 = l1._p2.x, y1 = l1._p1.y, y2 = l1._p2.y;
        double x3 = l2._p1.x, x4 = l2._p2.x, y3 = l2._p1.y, y4 = l2._p2.y;
        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (d != 0) {
            Point pt = new Point();
            pt.x= ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
            pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
            return pt;
        }
        return new Point(-1, -1);
    }


    private void getCanny(Mat gray, Mat canny) {
        Mat thres = new Mat();
        double high_thres = Imgproc.threshold(gray, thres, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU),
                low_thres = high_thres * 0.5;
        Imgproc.Canny(gray, canny, low_thres, high_thres);
    }

    private void findContours(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        Mat originMat = new Mat();
        Utils.bitmapToMat(bitmap, originMat);

        int w = mat.width(), h = mat.height();

        /* get four outline edges of the document */
        // get edges of the image
        Mat gray = new Mat(), canny = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);

        Mat hsv = new Mat();
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_RGB2HSV);

        List<Mat> channels = new ArrayList<>();

        Core.split(hsv, channels);

        Mat H = channels.get(0);
        Mat S = channels.get(1);
        Mat V = channels.get(2);

//        Mat blurMat = new Mat();
//        Imgproc.medianBlur(gray, blurMat, 3);

        Mat blurMat = new Mat();
        Imgproc.GaussianBlur(gray, blurMat, new org.opencv.core.Size(5, 5), 0);

        // Preparing the kernel matrix object
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new  org.opencv.core.Size(1, 1));

        Mat dilateMat = new Mat();

        Imgproc.dilate(blurMat, dilateMat, kernel);

        getCanny(dilateMat, canny);

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

        canvas.drawCircle(0, 0, 30, drawPaint);

        canvas.drawLine(0, 0,0, 1000,drawPaint );
        canvas.drawLine(0, 0,1000, 0,drawPaint );

//        Line line =  new Line(new Point(0, 100), new Point(200, 100));
//        canvas.drawLine(0,100, 200, 100, drawPaint );
//        LinePolar lp = toLinePolar(line);
//        Log.d("r", Double.toString(lp._r));
//        Log.d("theta", Double.toString(lp._theta));



        // extract lines from the edge image
        Mat lines = new Mat();
        Vector<Line> horizontals = new Vector<>(), verticals = new Vector<>();

        Imgproc.HoughLinesP(canny, lines, 1, Math.PI / 180, 70, 30, 10);

//        HashMap<Double, HashMap<Double, List<Line>>> lineMap = new HashMap<Double, HashMap<Double, List<Line>>>();

        HashMap<LinePolar, List<Line>> lineMap = new HashMap<LinePolar, List<Line>>();

        for (int i = 0; i < lines.rows(); i++) {
            double[] v = lines.get(i, 0);

            List<Point> points = new ArrayList<>();
            points.add(new Point(v[0], v[1]));
            points.add(new Point(v[2], v[3]));

            double delta_x = v[0] - v[2], delta_y = v[1] - v[3];

            Line l = new Line(new Point(v[0], v[1]), new Point(v[2], v[3]));
            LinePolar pl = toLinePolar(l);

            if (pl._theta > 10 && pl._theta < 80 ) continue;

            if (pl._theta < -10 && pl._theta > -80 ) continue;


            // get horizontal lines and vertical lines respectively
            if (abs(delta_x) > abs(delta_y)) {
                horizontals.addElement(l);
            } else {
                verticals.addElement(l);
            }

            canvas.drawLine((float) v[0], (float)v[1],(float) v[2], (float)v[3],drawPaint );

            if (!lineMap.containsKey(pl)) {
                List<Line> linesByPl = new ArrayList<Line>();
                linesByPl.add(l);
                lineMap.put(pl, linesByPl);
            } else {
                List<Line> linesByPl = lineMap.get(pl);
                linesByPl.add(l);
                lineMap.put(pl, linesByPl);
            }

            Comparator<Map.Entry> valueComparator = new Comparator<Map.Entry>() {
                @Override
                public int compare(Map.Entry o1, Map.Entry o2) {
                    List<Line> linesByPl = (List<Line>) o1.getValue();
                    return 0;
                }
            };



//            if (!lineMap.containsKey(pl._theta)) {
//                HashMap<Double, List<Line>> lineMapByR = new HashMap<Double, List<Line>>();
//
//                List<Line> linesByR = new ArrayList<Line>();
//                linesByR.add(l);
//
//                lineMapByR.put(pl._r, linesByR);
//                lineMap.put(pl._theta, lineMapByR);
//            } else {
//                HashMap<Double, List<Line>> lineMapByR = lineMap.get(pl._theta);
//
//                if (!lineMapByR.containsKey(pl._r)) {
//                    List<Line> linesByR = new ArrayList<Line>();
//                    linesByR.add(l);
//                    lineMapByR.put(pl._r, linesByR);
//                } else {
//                    List<Line> linesByR = lineMapByR.get(pl._r);
//                    linesByR.add(l);
//                    lineMapByR.put(pl._r, linesByR);
//                }
//                lineMap.put(pl._theta, lineMapByR);
//            }

        }

    }

    private void findContours1(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        Mat hsv = new Mat();
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_RGB2HSV);

        List<Mat> channels = new ArrayList<>();

        Core.split(hsv, channels);

        Mat H = channels.get(0);
        Mat S = channels.get(1);
        Mat V = channels.get(2);

        Mat originMat = mat.clone();

        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);

        // Preparing the kernel matrix object
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new  org.opencv.core.Size(3, 3));

        Mat dilate = new Mat();
        Imgproc.dilate(V, dilate, kernel);

        Mat medianBlur = new Mat();

        Imgproc.medianBlur(dilate, medianBlur, 1);

        Mat adaptiveThreshold = new Mat();

        Imgproc.adaptiveThreshold(medianBlur, adaptiveThreshold, 255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,11, 1);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchyMat = new Mat();

        Imgproc.findContours(adaptiveThreshold, contours, hierarchyMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

//        Imgproc.drawContours(originMat, contours, -1, new Scalar(255,0,0));
//
//        overlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(originMat, overlay);

        Collections.sort(contours, AreaDescendingComparator);

        double minS = (mat.width()*mat.height())*0.3;

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

    private boolean isExceedMat(MatOfPoint c, Mat myMat) {
        Point[] points = c.toArray();

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

    private boolean isExceedMat(List<Point> points, Mat myMat) {
        for(Point p : points){
            double rateX = p.x/myMat.width();
            double rateY = p.y/myMat.height();

            Log.d("rateX", Double.toString(rateX));
            Log.d("rateY", Double.toString(rateY));

            if (rateX < 0.01) return true;
            if (rateX > 0.99) return true;
            if (rateY < 0.01) return true;
            if (rateY > 0.99) return true;
        }
        return false;
    }

    private  void drawPoint(MatOfPoint contour, Bitmap bitmap) {
        Point[] points = contour.toArray();

        int paintColor = Color.RED;

        // Setup paint with color and stroke styles
        Paint drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(5);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

//        overlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);

        for (Point p : points) {
            Log.d("p.x", Double.toString(p.x));
            Log.d("p.y", Double.toString(p.y));
            canvas.drawCircle((float) p.x, (float)p.y, 30, drawPaint);
        }
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
            Log.d("p.x", Double.toString(p.x));
            Log.d("p.y", Double.toString(p.y));
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

    private LinePolar toLinePolar(Line l) {
        // y = ax+b
        double x1 = l._p1.x, y1 = l._p1.y,
                x2 = l._p2.x, y2 = l._p2.y;
        double a = (y2-y1)/(x2-x1);

        if (x2==x1) return new LinePolar(x1, 90);
        if (y2==y1) return new LinePolar(y1, 0);

//        Log.d("a", Double.toString(a));

        double b = y1 - a*x1;
//        Log.d("b", Double.toString(b));

        double x0 = -b/a;
        double y0 = b;
//        Log.d("x0", Double.toString(x0));
//        Log.d("y0", Double.toString(y0));

        double r     = x0*y0/Math.sqrt(x0*x0 + y0*y0);
        double theta = Math.atan2(x0, y0)*180/Math.PI;

        if(theta>90) theta=theta-180;
        if(theta<-90) theta=theta+180;

        LinePolar lp = new LinePolar(r,theta);
        return lp;
    }

    class Line {
        Point _p1;
        Point _p2;
        Point _center;

        Line(Point p1, Point p2) {
            _p1 = p1;
            _p2 = p2;
            _center = new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
        }
    };

    class LinePolar {
        double _theta;
        double _r;

        LinePolar(double r, double theta) {
            _theta = theta;
            _r = r;
        }

        public int hashCode() {
            return Objects.hash(getSigFields());
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LinePolar other = (LinePolar) o;
            return ((this._theta == other._theta) &&
                (this._r == other._r));
        }

        private Object[] getSigFields(){
            Object[] result = {_theta, _r};
            return result;
        }
    };

}

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
import com.journaldev.androidcameraxopencv.libraries.Line;
import com.journaldev.androidcameraxopencv.libraries.LinePolar;

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
//        double high_thres = 2*Imgproc.threshold(gray, thres, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU),
//                low_thres = high_thres/3;
        Imgproc.Canny(gray, canny, 255/3, 255);
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

        // extract lines from the edge image
        Mat lines = new Mat();

        Imgproc.HoughLinesP(canny, lines, 1, Math.PI / 180, 70, 30, 10);

        HashMap<LinePolar, List<Line>> verticalLineMap = new HashMap<LinePolar, List<Line>>();
        HashMap<LinePolar, List<Line>> horizontalLineMap = new HashMap<LinePolar, List<Line>>();

        for (int i = 0; i < lines.rows(); i++) {
            double[] v = lines.get(i, 0);

            List<Point> points = new ArrayList<>();
            points.add(new Point(v[0], v[1]));
            points.add(new Point(v[2], v[3]));

            double delta_x = v[0] - v[2], delta_y = v[1] - v[3];

            Line l = new Line(new Point(v[0], v[1]), new Point(v[2], v[3]));
            LinePolar pl = l.toLinePolar();

            if (pl._theta > 10 && pl._theta < 80 ) continue;

            if (pl._theta < -10 && pl._theta > -80 ) continue;


            // put horizontal lines and vertical lines respectively
            if (abs(delta_x) > abs(delta_y)) {
                putToLineMap(horizontalLineMap, l);
            } else {
                putToLineMap(verticalLineMap, l);
            }


        }

        List<Map.Entry<LinePolar, List<Line>>> horizontalLineMapValsList = sortLineMap(horizontalLineMap);
        List<Map.Entry<LinePolar, List<Line>>> verticalLineMapValsList = sortLineMap(verticalLineMap);

        int max = 0;

        for(Map.Entry<LinePolar, List<Line>> e : horizontalLineMapValsList) {
            LinePolar key = e.getKey();
            List<Line> localLines = e.getValue();
            for(Line l: localLines) {
                LinePolar lp = l.toLinePolar();
//                l.draw(canvas, drawPaint);
            }

            max++;

            if(max>=2) break;

        }

        max = 0;
        for(Map.Entry<LinePolar, List<Line>> e : verticalLineMapValsList) {
            LinePolar key = e.getKey();
            List<Line> localLines = e.getValue();
            for(Line l: localLines) {
                LinePolar lp = l.toLinePolar();
//                l.draw(canvas, drawPaint);
            }

            max++;

            if(max>=2) break;

        }

        List<Line> documentHorizontalEdges = getDocumentHorizontalEdges(horizontalLineMapValsList);
        List<Line> documentVerticalEdges = getDocumentVerticalEdges(verticalLineMapValsList);


        if(documentHorizontalEdges.size()==2&&documentVerticalEdges.size()==2) {
            Line l1 = documentHorizontalEdges.get(0);
            Line l3 = documentHorizontalEdges.get(1);

            Line l2 = documentVerticalEdges.get(0);
            Line l4 = documentVerticalEdges.get(1);

            Point p1 = computeIntersect(l1, l2);
            Point p2 = computeIntersect(l2, l3);
            Point p3 = computeIntersect(l3, l4);
            Point p4 = computeIntersect(l4, l1);

//            Log.d("p1.x", Double.toString(p1.x));
//            Log.d("p1.y", Double.toString(p1.y));
//            Log.d("p2.x", Double.toString(p2.x));
//            Log.d("p2.y", Double.toString(p2.y));
//            Log.d("p3.x", Double.toString(p3.x));
//            Log.d("p3.y", Double.toString(p3.y));
//            Log.d("p4.x", Double.toString(p4.x));
//            Log.d("p4.y", Double.toString(p4.y));



            canvas.drawCircle((float) p1.x, (float)p1.y, 30, drawPaint);
            canvas.drawCircle((float) p2.x, (float)p2.y, 30, drawPaint);
            canvas.drawCircle((float) p3.x, (float)p3.y, 30, drawPaint);
            canvas.drawCircle((float) p4.x, (float)p4.y, 30, drawPaint);

            Point[] points ={p1, p2, p3, p4};
            MatOfPoint2f contour = new MatOfPoint2f(points);

            // break loop if the document is too far from the phone
            double minS = mat.width()*mat.height()*0.3;
            if(Imgproc.contourArea(contour) < minS) {
                ScannerConstants.scanHint = ScanHint.MOVE_CLOSER;
                return;
            }

            ScannerConstants.scanHint = ScanHint.CAPTURING_IMAGE;
            ScannerConstants.selectedImageBitmap = bitmap;

            ScannerConstants.croptedPolygon = contour;

        }

    }

    private void putToLineMap(HashMap<LinePolar, List<Line>> lineMap, Line line) {
        LinePolar lp = line.toLinePolar();

        LinePolar oldAverage=null, newAverage=null;
        List<Line> lines = null;

        boolean isNewBucket = true;

        for (Map.Entry<LinePolar, List<Line>> entry : lineMap.entrySet()) {
            LinePolar averageLp = entry.getKey();
            Double deltaTheta = averageLp.deltaTheta(lp);
            Double deltaR = averageLp.deltaR(lp);
            if((deltaTheta < 1) && (deltaR < 2)) {
                isNewBucket = false;
                oldAverage = averageLp;
                lines = entry.getValue();
                lines.add(line);
                break;
            }
        }

        if (isNewBucket) {
            List<Line> newLines = new ArrayList<>();
            newLines.add(line);
            lineMap.put(lp, newLines);
        } else {
            lineMap.remove(oldAverage);
            newAverage = LinePolar.average(lines);
            lineMap.put(newAverage, lines);
        }
    }

    private List<Line> getDocumentHorizontalEdges(List<Map.Entry<LinePolar, List<Line>>> horizontalLineMapValsList) {
        if(horizontalLineMapValsList.size()<2) return new ArrayList<>();
        List<Line> ret = new ArrayList<Line>();
        Line firstEdge = horizontalLineMapValsList.get(0).getValue().get(0);

        LinePolar firstEdgePolar = horizontalLineMapValsList.get(0).getKey();
//        Log.d("firstEdge x1 ", Double.toString(firstEdge._p1.x));
//        Log.d("firstEdge y1 ", Double.toString(firstEdge._p1.y));
//        Log.d("firstEdge x2 ", Double.toString(firstEdge._p2.x));
//        Log.d("firstEdge y2 ", Double.toString(firstEdge._p2.y));
//

        Line secondEdge = null;

        double maxDistance = 0;

        for (Map.Entry<LinePolar, List<Line>> entry: horizontalLineMapValsList) {
            List<Line> localLines = entry.getValue();
            LinePolar key = entry.getKey();
            if(key.deltaTheta(firstEdgePolar) < 2) {
                double deltaR = key.deltaR(firstEdgePolar);
                if(deltaR > maxDistance ) {
                    maxDistance = deltaR;
                    secondEdge = localLines.get(0);
                }
            }
        }
        ret.add(firstEdge);

        if(maxDistance > previewView.getBitmap().getHeight()*0.5) {
            ret.add(secondEdge);

            Log.d("firstEdge beta ", Double.toString(firstEdgePolar.getBeta()));
            Log.d("firstEdge r ", Double.toString(firstEdgePolar._r));

            LinePolar secondEdgePolar = secondEdge.toLinePolar();
            Log.d("secondEdge beta 2 ", Double.toString(secondEdgePolar.getBeta()));
            Log.d("secondEdge r 2 ", Double.toString(secondEdgePolar._r));
            Log.d("maxDistance Horizon", Double.toString(maxDistance));

            Log.d("preview height", Double.toString(previewView.getBitmap().getHeight()/2));
        }

        return ret;
    }

    private List<Line> getDocumentVerticalEdges(List<Map.Entry<LinePolar, List<Line>>> verticalLineMapValsList) {
        if(verticalLineMapValsList.size()<2) return new ArrayList<>();
        List<Line> ret = new ArrayList<Line>();
        Line firstEdge = verticalLineMapValsList.get(0).getValue().get(0);
        LinePolar firstEdgePolar = verticalLineMapValsList.get(0).getKey();
        Line secondEdge = null;

        double maxDistance = 0;

        for (Map.Entry<LinePolar, List<Line>> entry: verticalLineMapValsList) {
            List<Line> localLines = entry.getValue();
            LinePolar key = entry.getKey();
            double deltaTheta = key.deltaTheta(firstEdgePolar);

//            Log.d("deltaTheta", Double.toString(deltaTheta));
            if(deltaTheta < 2 || deltaTheta > 178) {
                double deltaR = key.deltaR(firstEdgePolar);
//                Log.d("deltaR", Double.toString(deltaR));
                if(deltaR > maxDistance ) {
                    maxDistance = deltaR;
                    secondEdge = localLines.get(0);
                }
            }
        }
        ret.add(firstEdge);

//        Log.d("maxDistance", Double.toString(maxDistance));

        if(maxDistance > previewView.getBitmap().getWidth()*0.5) {
//            Log.d("maxDistance vertical", Double.toString(maxDistance));
            ret.add(secondEdge);
            LinePolar secondEdgePolar = secondEdge.toLinePolar();
//            Log.d("secondEdge beta 2 ", Double.toString(secondEdgePolar.getBeta()));
//            Log.d("secondEdge r 2 ", Double.toString(secondEdgePolar._r));
//            Log.d("maxDistance ", Double.toString(maxDistance));
        }

        return ret;
    }

    private List<Map.Entry<LinePolar, List<Line>>> sortLineMap(HashMap<LinePolar, List<Line>> lineMap) {
        Comparator<Map.Entry> distanceComparator = new Comparator<Map.Entry>() {
            @Override
            public int compare(Map.Entry o1, Map.Entry o2) {
                List<Line> linesByPl1 = (List<Line>) o1.getValue();
                List<Line> linesByPl2 = (List<Line>) o2.getValue();

                double maxDistance1 = maxDistance(linesByPl1);
                double maxDistance2 = maxDistance(linesByPl2);

                return Double.compare(maxDistance2, maxDistance1);
            }
        };

        Set<Map.Entry<LinePolar, List<Line>>> lineMapVals = lineMap.entrySet();

        List<Map.Entry<LinePolar, List<Line>>> lineMapValsList = new ArrayList<Map.Entry<LinePolar, List<Line>>>(lineMapVals);
        Collections.sort(lineMapValsList, distanceComparator);

        return lineMapValsList;
    }

    private double maxDistance(List<Line> lines) {
        double ret = 0;

//        for(Line l:lines) {
//            ret = ret+l.distance();
//        }


        List xs = new ArrayList();
        List ys = new ArrayList();

        for(Line line: lines) {
            xs.add(line._p1.x);
            xs.add(line._p2.x);
            ys.add(line._p1.y);
            ys.add(line._p2.y);
        }


        Comparator xComparator = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                double x1 = (double)o1;
                double x2 = (double)o2;
                return Double.compare(x1, x2);
            }
        };

        Comparator yComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                double y1 = (double)o1;
                double y2 = (double)o2;
                return Double.compare(y1, y2);
            }
        };

        Collections.sort(xs, xComparator);
        double xmin = (double) xs.get(0),
                xmax = (double) xs.get(xs.size()-1);

        Collections.sort(ys, xComparator);
        double ymin = (double) ys.get(0),
                ymax = (double) ys.get(ys.size()-1);

        ret = Math. sqrt((xmax-xmin)*(xmax-xmin) + (ymax-ymin)*(ymax-ymin));

        return ret;
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
        Imgproc.dilate(gray, dilate, kernel);

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

}

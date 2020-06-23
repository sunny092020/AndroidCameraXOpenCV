package com.journaldev.androidcameraxopencv.helpers;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.journaldev.androidcameraxopencv.enums.ScanHint;
import com.journaldev.androidcameraxopencv.libraries.Line;
import com.journaldev.androidcameraxopencv.libraries.LinePolar;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.lang.Math.abs;

public class ImageUtils {

    public static MatOfPoint2f scaleContour(MatOfPoint2f contour, double scaleX, double scaleY) {
        List<Point> points = contour.toList();

        Point[] scalePoints = new Point[4];

        for(int i=0; i<4; i++) {
            Point p = points.get(i);
            scalePoints[i] = new Point(scaleX*p.x, scaleY*p.y);
        }

        return new MatOfPoint2f(scalePoints);
    }

    public static MatOfPoint2f coverAllMethods4Contours(Mat[] inputMats) {
        if((ScannerConstants.cacheFindContoursFun!=null) && (ScannerConstants.cacheMatIndex>=0)) {
            Mat localMat = inputMats[ScannerConstants.cacheMatIndex];

            MatOfPoint2f ret = ScannerConstants.cacheFindContoursFun.apply(localMat);
            if (ret != null) return ret;
        }

        List<Function<Mat, MatOfPoint2f>> functions = new ArrayList<>();

        // apply in this order
        functions.add(ImageUtils::adaptiveThreshold);
        functions.add(ImageUtils::houghLines);

        for(Function <Mat, MatOfPoint2f> f:functions) {
            int inputMatsIndex = 0;
            for(Mat localMat: inputMats) {
                MatOfPoint2f contour = f.apply(localMat);
                if(contour!=null) {
                    ScannerConstants.cacheFindContoursFun = f;
                    ScannerConstants.cacheMatIndex = inputMatsIndex;
                    return contour;
                }
                inputMatsIndex++;
            }
        }
        return null;
    }

    private static MatOfPoint2f houghLines(Mat mat) {
        double matWidth = mat.width(), matHeight = mat.height();

        Mat blurMat = new Mat();
        Imgproc.GaussianBlur(mat, blurMat, new org.opencv.core.Size(5, 5), 0);

        // Preparing the kernel matrix object
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new  org.opencv.core.Size(1, 1));

        Mat dilateMat = new Mat();

        Imgproc.dilate(blurMat, dilateMat, kernel);
        blurMat.release();

        Mat canny = new Mat();
        getCanny(dilateMat, canny);
        dilateMat.release();

        // extract lines from the edge image
        Mat lines = new Mat();

        Imgproc.HoughLinesP(canny, lines, 1, Math.PI / 180, 70, 30, 10);
        canny.release();

        HashMap<LinePolar, List<Line>> verticalLineMap = new HashMap<>();
        HashMap<LinePolar, List<Line>> horizontalLineMap = new HashMap<>();

        for (int i = 0; i < lines.rows(); i++) {
            double[] v = lines.get(i, 0);

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

        List<Line> documentHorizontalEdges = getDocumentHorizontalEdges(horizontalLineMapValsList, matHeight);
        List<Line> documentVerticalEdges = getDocumentVerticalEdges(verticalLineMapValsList, matWidth);

        if(documentHorizontalEdges.size()==2&&documentVerticalEdges.size()==2) {
            Line l1 = documentHorizontalEdges.get(0);
            Line l3 = documentHorizontalEdges.get(1);

            Line l2 = documentVerticalEdges.get(0);
            Line l4 = documentVerticalEdges.get(1);

            Point p1 = computeIntersect(l1, l2);
            Point p2 = computeIntersect(l2, l3);
            Point p3 = computeIntersect(l3, l4);
            Point p4 = computeIntersect(l4, l1);

            Point[] points ={p1, p2, p3, p4};

            MatOfPoint2f contour =  new MatOfPoint2f(points);

            // break loop if points are in the edge of the frame
            if(isExceedMat(contour.toList(), matWidth, matHeight)) return null;

            // break loop if the document is too far from the phone
            double minS =matWidth*matHeight*0.3;
            if(Imgproc.contourArea(contour) < minS) {
                ScannerConstants.scanHint = ScanHint.MOVE_CLOSER;
                return null;
            }

            return contour;
        }

        return null;
    }

    private static boolean isExceedMat(List<Point> points, double width, double height) {
        for(Point p : points){
            double rateX = p.x/width;
            double rateY = p.y/height;
            if (rateX < 0.01) return true;
            if (rateX > 0.99) return true;
            if (rateY < 0.01) return true;
            if (rateY > 0.99) return true;
        }
        return false;
    }

    private static MatOfPoint2f adaptiveThreshold(Mat mat) {
        double matWidth = mat.width(), matHeight = mat.height();

        // Preparing the kernel matrix object
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new  org.opencv.core.Size(3, 3));

        Mat dilate = new Mat();
        Imgproc.dilate(mat, dilate, kernel);

        Mat medianBlur = new Mat();

        Imgproc.medianBlur(dilate, medianBlur, 1);
        dilate.release();

        Mat adaptiveThreshold = new Mat();

        Imgproc.adaptiveThreshold(medianBlur, adaptiveThreshold, 255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,11, 1);
        medianBlur.release();

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchyMat = new Mat();

        Imgproc.findContours(adaptiveThreshold, contours, hierarchyMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchyMat.release();

        Collections.sort(contours, AreaDescendingComparator);

        double minS = (matWidth*matHeight)*0.3;

        ScannerConstants.scanHint = ScanHint.NO_MESSAGE;

        for(MatOfPoint c : contours){
            MatOfPoint2f contour = new MatOfPoint2f(c.toArray());

            double length  = Imgproc.arcLength(contour,true);
            Imgproc.approxPolyDP(contour, contour,0.02*length,true);

            // break loop if it is not quad
            if(contour.total() != 4) break;

            // break loop if points are in the edge of the frame
            if(isExceedMat(contour.toList(), matWidth, matHeight)) break;

            // break loop if the document is too far from the phone
            if(Imgproc.contourArea(contour) < minS) {
                ScannerConstants.scanHint = ScanHint.MOVE_CLOSER;
                break;
            }

            return contour;
        }
        return null;
    }

    private static Comparator<MatOfPoint> AreaDescendingComparator = (m1, m2) -> {
        double area1 = Imgproc.contourArea(m1);
        double area2 = Imgproc.contourArea(m2);
        return Double.compare(area2, area1);
    };

    private static void putToLineMap(HashMap<LinePolar, List<Line>> lineMap, Line line) {
        LinePolar lp = line.toLinePolar();

        LinePolar oldAverage=null, newAverage;
        List<Line> lines = null;

        boolean isNewBucket = true;

        for (Map.Entry<LinePolar, List<Line>> entry : lineMap.entrySet()) {
            LinePolar averageLp = entry.getKey();
            double deltaTheta = averageLp.deltaTheta(lp);
            double deltaR = averageLp.deltaR(lp);
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

    private static List<Line> getDocumentHorizontalEdges(List<Map.Entry<LinePolar, List<Line>>> horizontalLineMapValsList, double matHeight) {
        if(horizontalLineMapValsList.size()<2) return new ArrayList<>();
        List<Line> ret = new ArrayList<>();
        LinePolar firstEdgePolar = horizontalLineMapValsList.get(0).getKey();
        double maxDistance = 0;
        List<Line> secondBucket = new ArrayList<>();

        for (Map.Entry<LinePolar, List<Line>> entry: horizontalLineMapValsList) {
            LinePolar key = entry.getKey();
            if(key.deltaTheta(firstEdgePolar) < 2) {
                double deltaR = key.deltaR(firstEdgePolar);
                if(deltaR > maxDistance ) {
                    maxDistance = deltaR;
                    secondBucket = entry.getValue();
                }
            }
        }

        List<Line> firstBucket = horizontalLineMapValsList.get(0).getValue();

        ret.add(LinePolar.averageLine(firstBucket));

        if(maxDistance > matHeight*0.5) {
            ret.add(LinePolar.averageLine(secondBucket));
        }

        return ret;
    }

    private static List<Line> getDocumentVerticalEdges(List<Map.Entry<LinePolar, List<Line>>> verticalLineMapValsList, double matWidth) {
        if(verticalLineMapValsList.size()<2) return new ArrayList<>();
        List<Line> ret = new ArrayList<>();
        LinePolar firstEdgePolar = verticalLineMapValsList.get(0).getKey();
        double maxDistance = 0;

        List<Line> secondBucket = new ArrayList<>();

        for (Map.Entry<LinePolar, List<Line>> entry: verticalLineMapValsList) {
            LinePolar key = entry.getKey();
            double deltaTheta = key.deltaTheta(firstEdgePolar);

            if(deltaTheta < 2 || deltaTheta > 178) {
                double deltaR = key.deltaR(firstEdgePolar);
                if(deltaR > maxDistance ) {
                    maxDistance = deltaR;
                    secondBucket = entry.getValue();
                }
            }
        }

        List<Line> firstBucket = verticalLineMapValsList.get(0).getValue();
        ret.add(LinePolar.averageLine(firstBucket));
        if(maxDistance > matWidth*0.5) {
            ret.add(LinePolar.averageLine(secondBucket));
        }
        return ret;
    }

    private static List<Map.Entry<LinePolar, List<Line>>> sortLineMap(HashMap<LinePolar, List<Line>> lineMap) {
        Comparator<Map.Entry> distanceComparator = (o1, o2) -> {
            List<Line> linesByPl1 = (List<Line>) o1.getValue();
            List<Line> linesByPl2 = (List<Line>) o2.getValue();

            double maxDistance1 = maxDistance(linesByPl1);
            double maxDistance2 = maxDistance(linesByPl2);

            return Double.compare(maxDistance2, maxDistance1);
        };

        Set<Map.Entry<LinePolar, List<Line>>> lineMapVals = lineMap.entrySet();

        List<Map.Entry<LinePolar, List<Line>>> lineMapValsList = new ArrayList<Map.Entry<LinePolar, List<Line>>>(lineMapVals);
        Collections.sort(lineMapValsList, distanceComparator);

        return lineMapValsList;
    }

    private static double maxDistance(List<Line> lines) {
        double ret;

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


        Comparator xyComparator = (o1, o2) -> {
            double xy1 = (double)o1;
            double xy2 = (double)o2;
            return Double.compare(xy1, xy2);
        };

        Collections.sort(xs, xyComparator);
        double xmin = (double) xs.get(0),
                xmax = (double) xs.get(xs.size()-1);

        Collections.sort(ys, xyComparator);
        double ymin = (double) ys.get(0),
                ymax = (double) ys.get(ys.size()-1);

        ret = Math. sqrt((xmax-xmin)*(xmax-xmin) + (ymax-ymin)*(ymax-ymin));

        return ret;
    }

    private static Point computeIntersect(Line l1, Line l2) {
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


    private static void getCanny(Mat gray, Mat canny) {
        Mat thres = new Mat();
//        double high_thres = 2*Imgproc.threshold(gray, thres, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU),
//                low_thres = high_thres/3;
        Imgproc.Canny(gray, canny, 255/3, 255);
    }

    public static Bitmap rotateBitmap(Bitmap original, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
    }

    public static Mat bitmapToMat(Bitmap bitmap) {
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8U, new Scalar(4));
        Bitmap bitmap32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bitmap32, mat);
        return mat;
    }

    public static Bitmap matToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

}

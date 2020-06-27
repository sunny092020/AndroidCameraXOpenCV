package com.journaldev.androidcameraxopencv.helpers;

import android.app.Activity;
import android.graphics.Bitmap;

import com.journaldev.androidcameraxopencv.enums.ScanHint;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ScannerConstants {
    public static boolean analyzing = true;
    public static boolean captured_finish = false;
    public static MatOfPoint2f croptedPolygon;
    public static Bitmap selectedImageBitmap;
    public static Bitmap cropImageBitmap;
    public static ScanHint scanHint = ScanHint.NO_MESSAGE;
    public static boolean saveStorage=false;

    public static BiFunction<Mat, Activity, MatOfPoint2f> cacheFindContoursFun = null;
    public static int cacheMatIndex = -1;

    public static String cropText="Crop",backText="Retake",
            imageError="No images selected, please try again.",
            cropError="You have not selected a valid field. Please correct it until the lines turn blue.";
    public static String cropColor="#3399ff",backColor="#000000",progressColor="#331199";

    // retake image, back to main activity from crop activity
    public static void resetCaptureState() {
        ScannerConstants.analyzing = true;
        ScannerConstants.captured_finish = false;
        ScannerConstants.scanHint = ScanHint.NO_MESSAGE;
        croptedPolygon = null;
        selectedImageBitmap = null;
        cropImageBitmap = null;
    }
}

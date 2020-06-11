package com.journaldev.androidcameraxopencv.helpers;

import android.graphics.Bitmap;

import com.journaldev.androidcameraxopencv.enums.ScanHint;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;

public class ScannerConstants {
    public static boolean analyzing = true;
    public static MatOfPoint2f croptedPolygon;
    public static Bitmap selectedImageBitmap;
    public static ScanHint scanHint = ScanHint.NO_MESSAGE;
    public static boolean saveStorage=false;

    public static String cropText="Crop",backText="Retake",
            imageError="No images selected, please try again.",
            cropError="You have not selected a valid field. Please correct it until the lines turn blue.";
    public static String cropColor="#3399ff",backColor="#000000",progressColor="#331199";

    public static void resetCaptureState() {
        ScannerConstants.analyzing = true;
        ScannerConstants.scanHint = ScanHint.NO_MESSAGE;
        croptedPolygon = null;
        selectedImageBitmap = null;
    }
}

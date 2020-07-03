package com.ami.icamdocscanner.helpers;

import android.graphics.Bitmap;
import org.opencv.core.MatOfPoint2f;

public class ScannerConstants {
    public static MatOfPoint2f croppedPolygon;
    public static Bitmap selectedImageBitmap;
    public static Bitmap cropImageBitmap;

    public static String cropText="Crop";
    public static String backText="Retake";
    public static String cropError="You have not selected a valid field. Please correct it until the lines turn blue.";
    public static String cropColor="#3399ff",backColor="#000000",progressColor="#331199";

    // retake image, back to main activity from crop activity
    public static void resetCaptureState() {
        croppedPolygon = null;
        selectedImageBitmap = null;
        cropImageBitmap = null;
    }
}

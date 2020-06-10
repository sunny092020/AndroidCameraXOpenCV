package com.journaldev.androidcameraxopencv.helpers;

import android.graphics.Bitmap;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;

public class ScannerConstants {
    public static boolean analyzing = true;
    public static MatOfPoint2f croptedPolygon;
    public static Bitmap selectedImageBitmap;
    public static String cropText="Crop",backText="Retake",
            imageError="No images selected, please try again.",
            cropError="You have not selected a valid field. Please correct it until the lines turn blue.";
    public static String cropColor="#6666ff",backColor="#ff0000",progressColor="#331199";
    public static boolean saveStorage=false;
}

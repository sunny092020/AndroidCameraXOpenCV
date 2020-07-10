package com.ami.icamdocscanner.helpers;

import android.graphics.Bitmap;

import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.core.MatOfPoint2f;

import java.util.ArrayList;
import java.util.List;

public class ScannerState {
    public static MatOfPoint2f croppedPolygon;
    public static Bitmap selectedImageBitmap;
    public static Bitmap cropImageBitmap;

    public static List<RecyclerImageFile> capturedImages = new ArrayList<>();
    public static List<RecyclerImageFile> croppedImages = new ArrayList<>();

    public static String cropText="Crop";
    public static String backText="Retake";
    public static String cropError="You have not selected a valid field. Please correct it until the lines turn blue.";
    public static String cropColor="#3399ff",backColor="#000000",progressColor="#331199";

    // retake image, back to main activity from crop activity
    public static void resetScannerState() {
        croppedPolygon = null;
        selectedImageBitmap = null;
        cropImageBitmap = null;
    }

    public static void updateCroppedPolygon(RecyclerImageFile file, List<RecyclerImageFile> files) {
        for(RecyclerImageFile f: files) {
            if(f.equals(file)) {
                f.setCroppedPolygon(null);
                return;
            }
        }
    }
}

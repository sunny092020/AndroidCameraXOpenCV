package com.ami.icamdocscanner.helpers;

import android.graphics.Bitmap;

import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.core.MatOfPoint2f;

import java.util.ArrayList;
import java.util.List;

public class ScannerState {
    public static List<RecyclerImageFile> cropImages = new ArrayList<>();
    public static List<RecyclerImageFile> editImages = new ArrayList<>();
    public static List<RecyclerImageFile> doneImages = new ArrayList<>();

    public static String progressColor="#331199";

    // retake image, back to main activity from crop activity
    public static void resetScannerState() {
        cropImages = new ArrayList<>();
        editImages = new ArrayList<>();
        doneImages = new ArrayList<>();
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

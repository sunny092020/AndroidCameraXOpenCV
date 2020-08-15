package com.ami.icamdocscanner.helpers;

import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.util.ArrayList;
import java.util.List;

public class ScannerState {
    // original images
    private static List<RecyclerImageFile> originImages = new ArrayList<>();

    // cropped images
    private static List<RecyclerImageFile> editImages = new ArrayList<>();

    // filtered images
    private static List<RecyclerImageFile> doneImages = new ArrayList<>();

    // retake image, back to main activity from crop activity
    public static void resetScannerState() {
        originImages.clear();
        editImages.clear();
        doneImages.clear();
    }

    public static List<RecyclerImageFile> getOriginImages() {
        return originImages;
    }

    public static List<RecyclerImageFile> getEditImages() {
        return editImages;
    }

    public static List<RecyclerImageFile> getDoneImages() {
        return doneImages;
    }

    public static RecyclerImageFile getFileByName(String fileName, List<RecyclerImageFile> files) {
        for(RecyclerImageFile file: files) {
            if(fileName.equalsIgnoreCase(file.getAbsolutePath())) {
                return file;
            }
        }
        return null;
    }

    public static boolean isFileExist(String fileName, List<RecyclerImageFile> files) {
        for(RecyclerImageFile file: files) {
            if(fileName.equalsIgnoreCase(file.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }
}

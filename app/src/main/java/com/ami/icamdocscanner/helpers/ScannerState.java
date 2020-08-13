package com.ami.icamdocscanner.helpers;

import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.util.ArrayList;
import java.util.List;

public class ScannerState {
    private static List<RecyclerImageFile> cropImages = new ArrayList<>();
    private static List<RecyclerImageFile> editImages = new ArrayList<>();
    private static List<RecyclerImageFile> doneImages = new ArrayList<>();
    private static List<RecyclerImageFile> savedImages = new ArrayList<>();
    public static int holderCropWidth = 0, holderCropHeight = 0;

    // retake image, back to main activity from crop activity
    public static void resetScannerState() {
        cropImages.clear();
        editImages.clear();
        doneImages.clear();
        savedImages.clear();
    }

    public static List<RecyclerImageFile> getCropImages() {
        return cropImages;
    }

    public static List<RecyclerImageFile> getEditImages() {
        return editImages;
    }

    public static List<RecyclerImageFile> getDoneImages() {
        return doneImages;
    }

    public static List<RecyclerImageFile> getSavedImages() {
        return savedImages;
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

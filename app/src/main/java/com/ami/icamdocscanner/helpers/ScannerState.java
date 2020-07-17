package com.ami.icamdocscanner.helpers;

import android.util.Log;

import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ScannerState {
    private static List<RecyclerImageFile> cropImages = new ArrayList<>();
    private static List<RecyclerImageFile> editImages = new ArrayList<>();
    private static List<RecyclerImageFile> doneImages = new ArrayList<>();

    public static String progressColor="#331199";

    // retake image, back to main activity from crop activity
    public static void resetScannerState() {
        cropImages.clear();
        editImages.clear();
        doneImages.clear();
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

    public static int getNextFileName(List<RecyclerImageFile> files) {
        if(files.size()==0) return 0;
        Comparator<RecyclerImageFile> nameComparator = (o1, o2) -> {
            int nameInt1 = Integer.parseInt(o1.getName().split("_")[0]);
            int nameInt2 = Integer.parseInt(o2.getName().split("_")[0]);
            return Integer.compare(nameInt1, nameInt2);
        };
        Collections.sort(files, nameComparator);
        int nameInt = Integer.parseInt(files.get(files.size()-1).getName().split("_")[0]);
        return nameInt+1;
    }
}

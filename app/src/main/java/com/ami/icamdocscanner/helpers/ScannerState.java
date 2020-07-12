package com.ami.icamdocscanner.helpers;

import android.util.Log;

import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    public static int getNextFileName(List<RecyclerImageFile> files) {
        if(files.size()==0) return 0;
        Comparator<RecyclerImageFile> nameComparator = (o1, o2) -> {
            int nameInt1 = Integer.parseInt(((RecyclerImageFile)o1).getName().split("_")[0]);
            int nameInt2 = Integer.parseInt(((RecyclerImageFile)o2).getName().split("_")[0]);
            return Integer.compare(nameInt2, nameInt1);
        };
        Collections.sort(files, nameComparator);
        int nameInt = Integer.parseInt(files.get(0).getName().split("_")[0]);
        Log.d("nameInt", "" + nameInt);
        return nameInt+1;
    }
}

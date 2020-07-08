package com.ami.icamdocscanner.models;

import androidx.annotation.NonNull;

import org.opencv.core.MatOfPoint2f;

import java.io.File;
import java.io.Serializable;

public class RecyclerImageFile extends File implements Serializable {

    private boolean isChecked = false;
    private MatOfPoint2f croppedPolygon;

    public RecyclerImageFile(@NonNull File file) {
        super(file.getAbsolutePath());
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public MatOfPoint2f getCroppedPolygon() {
        return croppedPolygon;
    }

    public void setCroppedPolygon(MatOfPoint2f croppedPolygon) {
        this.croppedPolygon = croppedPolygon;
    }

}

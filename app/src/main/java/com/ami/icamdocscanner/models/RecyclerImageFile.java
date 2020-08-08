package com.ami.icamdocscanner.models;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.opencv.core.MatOfPoint2f;

import java.io.File;
import java.io.Serializable;

public class RecyclerImageFile extends File implements Serializable {

    private boolean isChecked = false;

    public int getOriginWidth() {
        return originWidth;
    }

    public void setOriginWidth(int originWidth) {
        this.originWidth = originWidth;
    }

    public int getOriginHeight() {
        return originHeight;
    }

    public void setOriginHeight(int originHeight) {
        this.originHeight = originHeight;
    }

    private int originWidth = 0, originHeight=0;

    public Bitmap getScaledBitmap() {
        return scaledBitmap;
    }

    public void setScaledBitmap(Bitmap scaledBitmap) {
        this.scaledBitmap = scaledBitmap;
    }

    private Bitmap scaledBitmap = null;

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    private Uri uri;

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
    }

    private boolean isSaved = false;

    private MatOfPoint2f croppedPolygon;

    public RecyclerImageFile(@NonNull File file) {
        super(file.getAbsolutePath());
    }

    public RecyclerImageFile(@NonNull String fileName) {
        super(fileName);
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

    public void waitUntilSaved() {
        while (!isSaved()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

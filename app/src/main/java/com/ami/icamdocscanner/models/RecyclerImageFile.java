package com.ami.icamdocscanner.models;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.PdfUtils;
import com.ami.icamdocscanner.helpers.VisionUtils;

import org.opencv.core.MatOfPoint2f;

import java.io.File;
import java.io.Serializable;

public class RecyclerImageFile extends File implements Serializable {

    private boolean isChecked = false;

    public boolean isChanged() {
        return isChanged;
    }

    public void setChanged(boolean changed) {
        isChanged = changed;
    }

    private boolean isChanged = true;

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

    private String thumbnailPath() {
        String thumbnailPath = this.getParent() + "/thumbnails/" + this.getName();

        if(this.isDirectory()) {
            thumbnailPath += ".jpg";
        }

        if(FileUtils.isFileType(this.getName(), "pdf")) {
            thumbnailPath = this.getParent() + "/thumbnails/" + FileUtils.fileNameWithoutExtension(this.getName()) + "-pdf.jpg";
        }
        return thumbnailPath;
    }

    public Bitmap thumbnailBitmap() {
        int DOWNSCALE_IMAGE_SIZE = 300;
        Bitmap thumbnailBitmap = null;
        if(this.isFile()) {
            if(FileUtils.isFileType(this.getName(), "pdf")) {
                thumbnailBitmap = PdfUtils.thumbnail(this);
            } else {
                thumbnailBitmap = VisionUtils.getImage(this, DOWNSCALE_IMAGE_SIZE, DOWNSCALE_IMAGE_SIZE);
            }
        } else {
            RecyclerImageFile firstFile = FileUtils.listFiles(this).get(0);
            thumbnailBitmap = firstFile.thumbnailBitmap();
        }
        return thumbnailBitmap;
    }

    public void createThumbnail() {
        File thumbnailFile = new File(thumbnailPath());
        File thumbnailDir = thumbnailFile.getParentFile();
        if(!thumbnailDir.exists()) thumbnailDir.mkdir();
        FileUtils.writeBitmap(thumbnailBitmap(), thumbnailPath());
    }

    public Bitmap getThumbnail() {
        if(thumbnailExist()) return FileUtils.readBitmap(thumbnailPath());
        return null;
    }

    public boolean deleteThumbnail () {
        File thumbnailFile = new File(thumbnailPath());
        return thumbnailFile.delete();
    }

    public boolean thumbnailExist() {
        File thumbnailFile = new File(thumbnailPath());
        return thumbnailFile.exists();
    }

    public boolean delete() {
        boolean deleteThumbnail = deleteThumbnail();
        if(this.isFile()) {
            return deleteThumbnail && super.delete();
        } else {
            return deleteThumbnail && FileUtils.deleteDirectoryStream(this);
        }
    }
}

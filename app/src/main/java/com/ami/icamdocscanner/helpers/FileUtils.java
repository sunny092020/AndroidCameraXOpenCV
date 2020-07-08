package com.ami.icamdocscanner.helpers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {
    public static String fileNameWithoutExtension(String fileName) {
        if (fileName.indexOf(".") > 0)
            return fileName.substring(0, fileName.lastIndexOf("."));

        return fileName;
    }

    public static String fileExtension(String fileName) {
        if(fileName.indexOf(".") > 0)
            return fileName.substring(fileName.lastIndexOf(".")+1);
        else return "";
    }

    public static boolean isFileType(String fileName, String extension) {
        return FileUtils.fileExtension(fileName).equalsIgnoreCase(extension);
    }

    public static Bitmap getThumbnail(RecyclerImageFile imageFile) {
        String thumbnailPath = imageFile.getParent() + "/thumbnails/" + imageFile.getName();

        if(FileUtils.isFileType(imageFile.getName(), "pdf")) {
            thumbnailPath = imageFile.getParent() + "/thumbnails/" + FileUtils.fileNameWithoutExtension(imageFile.getName()) + "-pdf.jpg";
        }

        File thumbnailFile = new File(thumbnailPath);

        if(thumbnailFile.exists()) {
            return BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
        }else {

            if(FileUtils.isFileType(imageFile.getName(), "pdf")) {
                // TODO: cannot create thumbnail of a pdf
                return null;
            } else {
                return createThumbnail(imageFile, thumbnailPath);
            }
        }
    }

    public static void removeThumbnail(RecyclerImageFile imageFile) {
        String thumbnailPath = imageFile.getParent() + "/thumbnails/" + imageFile.getName();

        if(FileUtils.isFileType(imageFile.getName(), "pdf")) {
            thumbnailPath = imageFile.getParent() + "/thumbnails/" + FileUtils.fileNameWithoutExtension(imageFile.getName()) + "-pdf.jpg";
        }

        File thumbnailFile = new File(thumbnailPath);
        boolean delete_result = thumbnailFile.delete();
        Log.d("delete_result", Boolean.toString(delete_result));
    }

    public static Bitmap createThumbnail(RecyclerImageFile imageFile, String thumbnailPath) {

        File directory = new File(imageFile.getParent() + "/thumbnails/");
        if (!directory.exists()){
            if (!directory.mkdir()) return null;
        }

        Bitmap originBitmap;

        originBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        int DOWNSCALE_IMAGE_SIZE = 300;

        Bitmap smallOriginBitmap = VisionUtils.scaledBitmap(originBitmap, DOWNSCALE_IMAGE_SIZE, DOWNSCALE_IMAGE_SIZE);

        try (FileOutputStream out = new FileOutputStream(thumbnailPath)) {
            smallOriginBitmap.compress(Bitmap.CompressFormat.JPEG, 30, out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return smallOriginBitmap;
    }

    public static void ensureTempDir(Activity activity) {
        File directory = new File(activity.getFilesDir().getAbsolutePath() + "/" +  "temp_dir" + "/");
        if (!directory.exists()){
            if (!directory.mkdir()) return;
        }
    }

    public static void deleteTempDir(Activity activity) {
        File directory = new File(activity.getFilesDir().getAbsolutePath() + "/" +  "temp_dir");
        if (directory.exists()) {
            File[] allContents = directory.listFiles();
            if (allContents != null) {
                for (File file : allContents) {
                    file.delete();
                }
            }
            directory.delete();
        }
    }

    public static Bitmap readBitmap(String filename) {
        return BitmapFactory.decodeFile(filename);
    }

    public static void writeBitmap(Bitmap bitmap, String filename) {
        try (FileOutputStream out = new FileOutputStream(filename)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 99, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String home(Activity activity) {
        return activity.getFilesDir().getAbsolutePath();
    }

    public static String tempDir(Activity activity) {
        return activity.getFilesDir().getAbsolutePath() + "/" +  "temp_dir" + "/";
    }

}

package com.ami.icamdocscanner.helpers;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileUtils {

    public static List<RecyclerImageFile> listFilesByName(File directory) {
        File[] files = directory.listFiles(File::isFile);
        assert files != null;
        Arrays.sort( files, (Comparator<File>) (o1, o2) -> {
            String name1 = FileUtils.fileNameWithoutExtension(o1.getName());
            String name2 = FileUtils.fileNameWithoutExtension(o2.getName());
            return Integer.compare(Integer.parseInt(name1), Integer.parseInt(name2));
        });

        List<RecyclerImageFile> recyclerImageFiles = new ArrayList<>();

        for (File file : files) {
            RecyclerImageFile returnFile = new RecyclerImageFile(file);
            recyclerImageFiles.add(returnFile);
        }
        return recyclerImageFiles;
    }

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
        thumbnailFile.delete();
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

    public static void ensureTempDir(Context context) {
        File directory = new File(context.getFilesDir().getAbsolutePath() + "/" +  "temp_dir" + "/");
        if (!directory.exists()){
            if (!directory.mkdir()) return;
        }
    }

    public static void deleteTempDir(Context context) {
        File directory = new File(context.getFilesDir().getAbsolutePath() + "/" +  "temp_dir");
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

    public static boolean writeBitmap(Bitmap bitmap, String filename) {
        try (FileOutputStream out = new FileOutputStream(filename)) {
            return bitmap.compress(Bitmap.CompressFormat.JPEG, 99, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String home(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }

    public static String tempDir(Context context) {
        return context.getFilesDir().getAbsolutePath() + "/" +  "temp_dir" + "/";
    }

    public static String cropImagePath(Context context, String fileName) {
        return FileUtils.tempDir(context) + FileUtils.getNumberFromFileName(fileName) + "_crop.jpg";
    }

    public static String editImagePath(Context context, String fileName) {
        return FileUtils.tempDir(context) + FileUtils.getNumberFromFileName(fileName) + "_edit.jpg";
    }

    public static String doneImagePath(Context context, String fileName) {
        return FileUtils.tempDir(context) + FileUtils.getNumberFromFileName(fileName) + "_done.jpg";
    }

    public static String cropImagePath(Context context, String subDir, String fileName) {
        return FileUtils.tempDir(context) + subDir + FileUtils.getNumberFromFileName(fileName) + "_crop.jpg";
    }

    public static String editImagePath(Context context, String subDir, String fileName) {
        return FileUtils.tempDir(context) + subDir + FileUtils.getNumberFromFileName(fileName) + "_edit.jpg";
    }

    public static String doneImagePath(Context context, String subDir, String fileName) {
        return FileUtils.tempDir(context) + subDir + FileUtils.getNumberFromFileName(fileName) + "_done.jpg";
    }

    private static String getNumberFromFileName(String fileName) {
        String fileNameWithoutExtension = FileUtils.fileNameWithoutExtension(fileName);
        if (fileNameWithoutExtension.indexOf("_") > 0)
            return fileNameWithoutExtension.split("_")[0];
        return fileNameWithoutExtension;
    }
}

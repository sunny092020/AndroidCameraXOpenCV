package com.ami.icamdocscanner.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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
        if(FileUtils.fileExtension(fileName).equalsIgnoreCase(extension)) return true;
        return false;
    }

    public static Bitmap createThumbnail(RecyclerImageFile imageFile) {
        String thumbnailPath = imageFile.getParent() + "/thumbnails/" + imageFile.getName();

        if(FileUtils.isFileType(imageFile.getName(), "pdf")) {
            thumbnailPath = imageFile.getParent() + "/thumbnails/" + FileUtils.fileNameWithoutExtension(imageFile.getName()) + "jpg";
        }

        File thumbnailFile = new File(thumbnailPath);
        if(thumbnailFile.exists()) {
            return BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
        }else {
            File directory = new File(imageFile.getParent() + "/thumbnails/");
            if (!directory.exists()){
                if (!directory.mkdir()) return null;
            }

            Bitmap originBitmap = null;

            if(FileUtils.isFileType(imageFile.getName(), "pdf")) {
                // TODO: cannot create thumbnail of a pdf
                return null;
            } else {
                originBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                int DOWNSCALE_IMAGE_SIZE = 200;

                Bitmap smallOriginBitmap = VisionUtils.scaledBitmap(originBitmap, DOWNSCALE_IMAGE_SIZE, DOWNSCALE_IMAGE_SIZE);

                try (FileOutputStream out = new FileOutputStream(thumbnailPath)) {
                    smallOriginBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return smallOriginBitmap;
            }
        }
    }
}

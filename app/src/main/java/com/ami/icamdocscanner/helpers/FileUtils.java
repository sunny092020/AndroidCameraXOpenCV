package com.ami.icamdocscanner.helpers;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
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
        return FilenameUtils.getBaseName(fileName);
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

    public static Bitmap getThumbnailNoCreate(RecyclerImageFile imageFile) {
        String thumbnailPath = imageFile.getParent() + "/thumbnails/" + imageFile.getName();
        if(imageFile.isDirectory()) {
            thumbnailPath += ".jpg";
        }

        if(FileUtils.isFileType(imageFile.getName(), "pdf")) {
            thumbnailPath = imageFile.getParent() + "/thumbnails/" + FileUtils.fileNameWithoutExtension(imageFile.getName()) + "-pdf.jpg";
        }

        File thumbnailFile = new File(thumbnailPath);

        if(thumbnailFile.exists()) {
            return BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
        }else {
            return null;
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
        String thumbnailsDirectoryPath = new File(thumbnailPath).getParent();
        File thumbnailsDirectory = new File(thumbnailsDirectoryPath);
        if (!thumbnailsDirectory.exists()){
            if (!thumbnailsDirectory.mkdir()) return null;
        }

        Bitmap originBitmap;

        originBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        int DOWNSCALE_IMAGE_SIZE = 300;

        Bitmap smallOriginBitmap = VisionUtils.scaledBitmap(originBitmap, DOWNSCALE_IMAGE_SIZE, DOWNSCALE_IMAGE_SIZE);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(thumbnailPath);
            smallOriginBitmap.compress(Bitmap.CompressFormat.JPEG, 99, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(out!=null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return smallOriginBitmap;
    }

    public static void ensureTempDir(Context context) {
        File directory = new File(context.getFilesDir().getAbsolutePath() + "/" +  "temp_dir" + "/");
        if (!directory.exists()){
            if (!directory.mkdir()) return;
        }
    }

    public static void ensureDir(Context context, String dir) {
        File directory = new File(context.getFilesDir().getAbsolutePath() + "/" + dir + "/");
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

    public static Bitmap readBitmap(RecyclerImageFile image) {
        return BitmapFactory.decodeFile(image.getAbsolutePath());
    }

    public static boolean writeBitmap(Bitmap bitmap, String filename) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            return bitmap.compress(Bitmap.CompressFormat.JPEG, 99, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        finally {
            if(out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String home(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }

    public static String tempDir(Context context) {
        return context.getFilesDir().getAbsolutePath() + "/" +  "temp_dir" + "/";
    }

    public static String cropImagePath(Context context, String fileName) {
        return FileUtils.tempDir(context) + "crop_" + FileUtils.getOriginFileName(fileName);
    }

    public static String editImagePath(Context context, String fileName) {
        return FileUtils.tempDir(context) + "edit_" + FileUtils.getOriginFileName(fileName);
    }

    public static String doneImagePath(Context context, String fileName) {
        return FileUtils.tempDir(context) + "done_" + FileUtils.getOriginFileName(fileName);
    }

    public static String getOriginFileName(String fileName) {
        if (fileName.indexOf("crop_") >= 0)
            return fileName.substring(fileName.lastIndexOf("crop_") + 5);
        if (fileName.indexOf("edit_") >= 0)
            return fileName.substring(fileName.lastIndexOf("edit_") + 5);
        if (fileName.indexOf("done_") >= 0)
            return fileName.substring(fileName.lastIndexOf("done_") + 5);
        return fileName;
    }

    public static String ocrDir(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath() + "/ocr/";
    }

    public static void ensureOcrDir(Context context) {
        File directory = new File(ocrDir(context) + "tessdata/");
        if (!directory.exists()){
            if (!directory.mkdir()) return;
        }
    }

    public static String ocrFile(Context context, String lang) {
        return ocrDir(context) + "tessdata/" + lang + ".traineddata";
    }

    public static String getPath(Context context, Uri uri) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                    // TODO handle non-primary volumes
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static void copyFileStream(Context context, File dest, Uri uri) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;

            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            is.close();
            os.close();
        }
    }

    public static Bitmap readBitmap(Context context, Uri uri) throws IOException {
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(is!= null) {
                is.close();
            }
        }
        return bitmap;
    }

    public static String fileNameFromUri(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        String filename;
        if (mimeType == null) {
            String path = FileUtils.getPath(context, uri);
            if (path == null) {
                filename = FilenameUtils.getName(uri.toString());
            } else {
                File file = new File(path);
                filename = file.getName();
            }
        } else {
            Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            filename = returnCursor.getString(nameIndex);
        }
        return filename;
    }

    public static void copyFileUsingChannel(File source, File dest) {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            if(sourceChannel!=null) {
                try {
                    sourceChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(destChannel!=null){
                try {
                    destChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String fileType(RecyclerImageFile file) {
        if(fileExtension(file.getName()).equalsIgnoreCase("pdf"))
            return "application/pdf";
        else return "image/*";
    }

}

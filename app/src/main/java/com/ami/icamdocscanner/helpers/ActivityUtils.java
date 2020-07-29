package com.ami.icamdocscanner.helpers;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.core.MatOfPoint2f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ActivityUtils {

    public static void filePickerProcessResult(Context context, Intent data) {
        List<Uri> uris = new ArrayList<>();
        if(data.getData() != null) {
            Uri uri = data.getData();
            uris.add(uri);
        }

        if(data.getClipData() != null) {
            for(int i = 0; i<data.getClipData().getItemCount(); i++) {
                ClipData.Item item = data.getClipData().getItemAt(i);
                Uri uri = item.getUri();
                uris.add(uri);
            }
        }

        for(int i=0; i<uris.size(); i++) {
            Uri uri = uris.get(i);
            String fileName = FileUtils.cropImagePath(context, FileUtils.fileNameFromUri(context, uri) + ".jpg");
            if(ScannerState.isFileExist(fileName, ScannerState.getCropImages())) continue;
            RecyclerImageFile file = new RecyclerImageFile(fileName);
            ScannerState.getCropImages().add(file);
        }

        new Thread(() -> {
            FileUtils.ensureTempDir(context);
            for(int i=uris.size()-1; i>=0; i--) {
                Uri uri = uris.get(i);
                String fileName = FileUtils.cropImagePath(context, FileUtils.fileNameFromUri(context, uri) + ".jpg");
                try {
                    Bitmap bitmap = FileUtils.readBitmap(context, uri);
                    int originW = bitmap.getWidth();
                    int originH = bitmap.getHeight();
                    if(originW > originH) {
                        bitmap = VisionUtils.rotateBitmap(bitmap, 90);
                        originW = bitmap.getWidth();
                        originH = bitmap.getHeight();
                    }

                    MatOfPoint2f contour = VisionUtils.findContours(bitmap, (Activity) context);
                    if(contour == null) {
                        contour = VisionUtils.dummyContour(originW, originH);
                    }

                    FileUtils.writeBitmap(bitmap, fileName);
                    ScannerState.getFileByName(fileName, ScannerState.getCropImages()).setCroppedPolygon(contour);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

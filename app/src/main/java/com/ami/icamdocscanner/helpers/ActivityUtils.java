package com.ami.icamdocscanner.helpers;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.activities.ImageCropActivity;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.core.MatOfPoint2f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            String fileName = FileUtils.cropImagePath(context, FileUtils.fileNameFromUri(context, uri));
            if(ScannerState.isFileExist(fileName, ScannerState.getCropImages())) continue;
            RecyclerImageFile file = new RecyclerImageFile(fileName);
            file.setUri(uri);
            ScannerState.getCropImages().add(file);
        }
    }

    public static void processImageFromUri(ImageCropActivity activity) {
        new Thread(() -> {
            FileUtils.ensureTempDir(activity);
            for(int position=0; position<ScannerState.getCropImages().size(); position++) {
                RecyclerImageFile file = ScannerState.getCropImages().get(position);
                Uri uri = file.getUri();
                String fileName = FileUtils.cropImagePath(activity, FileUtils.fileNameFromUri(activity, uri));
                try {
                    Bitmap bitmap = FileUtils.readBitmap(activity, uri);
                    int originW = bitmap.getWidth();
                    int originH = bitmap.getHeight();
                    if(originW > originH) {
                        bitmap = VisionUtils.rotateBitmap(bitmap, 90);
                        originW = bitmap.getWidth();
                        originH = bitmap.getHeight();
                    }

                    MatOfPoint2f contour = VisionUtils.findContours(bitmap, activity);

                    if(contour == null) {
                        contour = VisionUtils.dummyContour(originW, originH);
                    }

                    FileUtils.writeBitmap(bitmap, fileName);
                    ScannerState.getFileByName(fileName, ScannerState.getCropImages()).setCroppedPolygon(contour);
                    ScannerState.getFileByName(fileName, ScannerState.getCropImages()).setOriginWidth(bitmap.getWidth());
                    ScannerState.getFileByName(fileName, ScannerState.getCropImages()).setOriginHeight(bitmap.getHeight());

                    while (ScannerState.holderCropWidth == 0 || ScannerState.holderCropHeight == 0) {
                        FrameLayout holderImageCrop = activity.findViewById(R.id.holderImageCrop);
                        holderImageCrop.post(() -> {
                            ScannerState.holderCropWidth = holderImageCrop.getWidth();
                            ScannerState.holderCropHeight = holderImageCrop.getHeight();
                        });
                    }

                    Bitmap scaledBitmap = VisionUtils.scaledBitmap(bitmap, ScannerState.holderCropWidth, ScannerState.holderCropHeight);
                    ScannerState.getFileByName(fileName, ScannerState.getCropImages()).setScaledBitmap(scaledBitmap);

                    int finalPosition = position;
                    activity.runOnUiThread(() -> {
                        activity.getAdapter().notifyItemChanged(finalPosition);
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

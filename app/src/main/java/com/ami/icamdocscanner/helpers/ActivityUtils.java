package com.ami.icamdocscanner.helpers;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.activities.ImageCropActivity;
import com.ami.icamdocscanner.activities.ImageDoneActivity;
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

        FileUtils.ensureTempDir(context);

        Activity activity = (Activity)context;

        LinearLayout progressBarHolder = activity.findViewById(R.id.progressBarHolder);
        progressBarHolder.setVisibility(View.VISIBLE);
        ProgressBar progressBar = activity.findViewById(R.id.progressBar);
        TextView pbText = activity.findViewById(R.id.pbText);

        new Thread(() -> {
            for(int i=0; i<uris.size(); i++) {
                Uri uri = uris.get(i);
                String fileName = FileUtils.cropImagePath(context, FileUtils.fileNameFromUri(context, uri));
                if(ScannerState.isFileExist(fileName, ScannerState.getCropImages())) continue;
                RecyclerImageFile croppedFile = new RecyclerImageFile(fileName);
                Bitmap bitmap = null;

                try {
                    bitmap = FileUtils.readBitmap(context, uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

                FileUtils.writeBitmap(bitmap, croppedFile.getAbsolutePath());
                croppedFile.setCroppedPolygon(contour);
                croppedFile.setChanged(false);
                ScannerState.getCropImages().add(croppedFile);

                Bitmap croppedBitmap = VisionUtils.getCroppedImage(croppedFile);
                String editImageFilePath =  FileUtils.editImagePath(context, FileUtils.getOriginFileName(croppedFile.getName()));
                String doneImageFilePath =  FileUtils.doneImagePath(context, FileUtils.getOriginFileName(croppedFile.getName()));
                ScannerState.getEditImages().add(new RecyclerImageFile(editImageFilePath));
                ScannerState.getDoneImages().add(new RecyclerImageFile(doneImageFilePath));
                FileUtils.writeBitmap(croppedBitmap, editImageFilePath);
                FileUtils.writeBitmap(croppedBitmap, doneImageFilePath);

                int percent = (i+1) *100/uris.size();
                progressBar.setProgress(percent);

                int finalI = i;
                activity.runOnUiThread(() -> {
                    pbText.setText("Import: " + (finalI +1) + "/" + uris.size() + " files");
                });
            }

            activity.runOnUiThread(() -> progressBarHolder.setVisibility(View.GONE));

            Intent cropIntent = new Intent(context, ImageCropActivity.class);
            context.startActivity(cropIntent);
            activity.finish();

        }).start();

    }
}

package com.ami.icamdocscanner.helpers;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

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
        ProgressBar progressBar = ((Activity)context).findViewById(R.id.progressBar);

        new Thread(() -> {
            for(int i=0; i<uris.size(); i++) {
                Uri uri = uris.get(i);
                String fileName = FileUtils.cropImagePath(context, FileUtils.fileNameFromUri(context, uri));
                if(ScannerState.isFileExist(fileName, ScannerState.getCropImages())) continue;
                RecyclerImageFile file = new RecyclerImageFile(fileName);
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

                FileUtils.writeBitmap(bitmap, file.getAbsolutePath());
                file.setCroppedPolygon(contour);


                ScannerState.getCropImages().add(file);

                int percent = (i+1) *100/uris.size();
                progressBar.setProgress(percent);
            }

            activity.runOnUiThread(() -> progressBarHolder.setVisibility(View.GONE));

            Intent cropIntent = new Intent(context, ImageCropActivity.class);
            context.startActivity(cropIntent);
            activity.finish();

        }).start();

    }
}

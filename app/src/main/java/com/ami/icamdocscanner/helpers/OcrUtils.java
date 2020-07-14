package com.ami.icamdocscanner.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;

public class OcrUtils {
    public static String ocr(Context context, RecyclerImageFile imageFile, String lang) {
        TessBaseAPI baseApi = new TessBaseAPI();
        Log.d("baseApi.getVersion()", ": " + baseApi.getVersion());

        String dataPath = FileUtils.ocrDir(context);
        File data = new File(dataPath);

        Log.d("datapath", dataPath + ": " + data.exists());

        boolean init = baseApi.init(dataPath, lang);
        Log.d("init", "" + init);

        Bitmap bitmap = FileUtils.readBitmap(imageFile.getAbsolutePath());
        baseApi.setImage(bitmap);
        String recognizedText = baseApi.getUTF8Text();
        Log.d("recognizedText",  recognizedText);

        bitmap = VisionUtils.rotateBitmap(bitmap, 90);
        baseApi.setImage(bitmap);
        recognizedText = baseApi.getUTF8Text();
        Log.d("recognizedText",  recognizedText);

        bitmap = VisionUtils.rotateBitmap(bitmap, 90);
        baseApi.setImage(bitmap);
        recognizedText = baseApi.getUTF8Text();
        Log.d("recognizedText",  recognizedText);

        bitmap = VisionUtils.rotateBitmap(bitmap, 90);
        baseApi.setImage(bitmap);
        recognizedText = baseApi.getUTF8Text();
        Log.d("recognizedText",  recognizedText);

        baseApi.end();

        return recognizedText;
    }
}

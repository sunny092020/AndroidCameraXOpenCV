package com.ami.icamdocscanner.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.googlecode.tesseract.android.TessBaseAPI;

// https://www.programcreek.com/java-api-examples/index.php?api=com.googlecode.tesseract.android.TessBaseAPI
public class OcrUtils {
    public static TessBaseAPI baseApi;

    public static String ocr(Context context, RecyclerImageFile imageFile, String lang) {
        baseApi = new TessBaseAPI((TessBaseAPI.ProgressNotifier) context);

        String dataPath = FileUtils.ocrDir(context);

        boolean init = baseApi.init(dataPath, lang);
        Log.d("init", "" + init);

        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
        Bitmap bitmap = FileUtils.readBitmap(imageFile.getAbsolutePath());
        baseApi.setImage(bitmap);

        baseApi.getHOCRText(0);

        String recognizedText = baseApi.getUTF8Text();

        baseApi.end();

        return recognizedText;
    }
}

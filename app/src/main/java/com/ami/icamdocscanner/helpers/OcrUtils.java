package com.ami.icamdocscanner.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Skew;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;

// https://www.programcreek.com/java-api-examples/index.php?api=com.googlecode.tesseract.android.TessBaseAPI
public class OcrUtils {
    public static TessBaseAPI baseApi;

    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }

    public static String ocr(Context context, RecyclerImageFile imageFile, String lang) {
        baseApi = new TessBaseAPI((TessBaseAPI.ProgressNotifier) context);

        String dataPath = FileUtils.ocrDir(context);

        boolean init = baseApi.init(dataPath, lang);
        Log.d("init", "" + init);

        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
        Bitmap bitmap = FileUtils.readBitmap(imageFile.getAbsolutePath());

        Mat origin = new Mat();
        Mat bw = new Mat();

        Utils.bitmapToMat(bitmap, origin);
        VisionUtils.toBw(origin, bw);
        Bitmap bwBitmap = VisionUtils.matToBitmap(bw);

        baseApi.setImage(bwBitmap);

        baseApi.getHOCRText(0);

        String recognizedText = baseApi.getUTF8Text();

        baseApi.end();

        return recognizedText;
    }
}

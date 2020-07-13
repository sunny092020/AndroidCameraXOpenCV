package com.ami.icamdocscanner.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;

import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PdfUtils {
    public static void toPDFMulti(List<RecyclerImageFile> imageFiles, String outPath) {
        toPDFMultiAndroidBuiltIn(imageFiles, outPath);
    }

    public static void toPDFSingle(RecyclerImageFile imgFile, String outPath) {
        toPDFSingleAndroidBuiltIn(imgFile, outPath);
    }

    private static void toPDFMultiAndroidBuiltIn(List<RecyclerImageFile> imageFiles, String outPath) {
        PdfDocument document = new PdfDocument();

        for(RecyclerImageFile imgFile: imageFiles) {
            Bitmap imgBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            PdfDocument.PageInfo pageInfo =new
                    PdfDocument.PageInfo.Builder(imgBitmap.getWidth(), imgBitmap.getHeight(), 1).create();
            PdfDocument.Page  page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            canvas.drawBitmap(imgBitmap, 0f, 0f, null);
            document.finishPage(page);
        }

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outPath + ".pdf");
            document.writeTo(os);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os!=null) {
                try {
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        document.close();
    }

    private static void toPDFSingleAndroidBuiltIn(RecyclerImageFile imgFile, String outPath) {
        PdfDocument document = new PdfDocument();
        Bitmap imgBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

        PdfDocument.PageInfo pageInfo =new
                PdfDocument.PageInfo.Builder(imgBitmap.getWidth(), imgBitmap.getHeight(), 1).create();
        PdfDocument.Page  page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        canvas.drawBitmap(imgBitmap, 0f, 0f, null);
        document.finishPage(page);

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outPath + ".pdf");
            document.writeTo(os);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os!=null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        document.close();
    }
}

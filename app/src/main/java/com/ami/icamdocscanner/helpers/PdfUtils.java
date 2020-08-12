package com.ami.icamdocscanner.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.widget.ProgressBar;

import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PdfUtils {
    public static void toPDFMulti(List<RecyclerImageFile> imageFiles, String outPath, ProgressBar progressBar) {
        toPDFMultiAndroidBuiltIn(imageFiles, outPath, progressBar);
    }

    public static void toPDFSingle(RecyclerImageFile imgFile, String outPath) {
        toPDFSingleAndroidBuiltIn(imgFile, outPath);
    }

    private static void toPDFMultiAndroidBuiltIn(List<RecyclerImageFile> imageFiles, String outPath, ProgressBar progressBar) {
        PDDocument document = new PDDocument();

        for(int i=0;i<imageFiles.size();i++) {
            RecyclerImageFile image = imageFiles.get(i);
            InputStream targetStream = null;
            try {
                targetStream = new FileInputStream(image);
                PDImageXObject pdImage = JPEGFactory.createFromStream(document, targetStream);

                PDPage page = new PDPage();
                PDPageContentStream contentStream = new PDPageContentStream(document, page);

                // https://stackoverflow.com/questions/39948870/pdfbox-convert-image-to-pdf-pdf-resolution
                PDRectangle pageSize = page.getMediaBox();
                float widthRatio = pageSize.getWidth()/pdImage.getWidth();
                float heightRatio = pageSize.getHeight()/pdImage.getHeight();
                float scaleDownRatio = Math.min(widthRatio, heightRatio);
                contentStream.drawImage(pdImage, (pageSize.getWidth() - pdImage.getWidth() * scaleDownRatio) / 2, (pageSize.getHeight() - pdImage.getHeight() * scaleDownRatio) / 2, pdImage.getWidth() * scaleDownRatio, pdImage.getHeight() * scaleDownRatio);

                contentStream.close();
                document.addPage(page);

                int percent = (int) (i+1)*100/imageFiles.size();
                progressBar.setProgress(percent);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(targetStream!=null) {
                    try {
                        targetStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        try {
            document.save(outPath + ".pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

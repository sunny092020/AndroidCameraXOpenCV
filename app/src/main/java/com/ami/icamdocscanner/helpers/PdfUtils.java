package com.ami.icamdocscanner.helpers;

import android.widget.ProgressBar;

import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.FileInputStream;
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
                page.setMediaBox(new PDRectangle(pdImage.getWidth(), pdImage.getHeight()));
                contentStream.drawImage(pdImage, 0, 0);

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
        PDDocument document = new PDDocument();

        InputStream targetStream = null;
        try {
            targetStream = new FileInputStream(imgFile);
            PDImageXObject pdImage = JPEGFactory.createFromStream(document, targetStream);

            PDPage page = new PDPage();
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            page.setMediaBox(new PDRectangle(pdImage.getWidth(), pdImage.getHeight()));
            contentStream.drawImage(pdImage, 0, 0);

            contentStream.close();
            document.addPage(page);

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
}

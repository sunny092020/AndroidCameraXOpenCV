package com.ami.icamdocscanner.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PdfUtils {
    public static void toPDFMulti(List<RecyclerImageFile> imageFiles, String outPath) {
        // step 1: creation of a document-object
        Document document = new Document();

        // step 2:
        // we create a writer that listens to the document
        // and directs a PDF-stream to a file
        PdfWriter instance;
        try {
            instance = PdfWriter.getInstance(document, new FileOutputStream(outPath + ".pdf"));
            instance.getInfo().put(PdfName.CREATOR, new PdfString(Document.getVersion()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < imageFiles.size(); i++) {
            RecyclerImageFile imgFile = imageFiles.get(i);

            Bitmap imgBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            ByteArrayOutputStream stream = new ByteArrayOutputStream();


            imgBitmap.compress(Bitmap.CompressFormat.JPEG, 99, stream);
            byte[] byteArr = stream.toByteArray();

            try {
                Rectangle one = new Rectangle(imgBitmap.getWidth(), imgBitmap.getHeight());
                document.setPageSize(one);
                document.setMargins(0, 0, 0, 0);

                document.open();

                if(i>0) document.newPage();

                Image img = Image.getInstance(byteArr);
                document.add(img);

            } catch (DocumentException | IOException de) {
                System.err.println(de.getMessage());
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        document.close();
    }

    public static void toPDFSingle(RecyclerImageFile imgFile, String outPath) {
        // step 1: creation of a document-object
        Document document = new Document();

        // step 2:
        // we create a writer that listens to the document
        // and directs a PDF-stream to a file
        PdfWriter instance;
        try {
            instance = PdfWriter.getInstance(document, new FileOutputStream(outPath + ".pdf"));
            instance.getInfo().put(PdfName.CREATOR, new PdfString(Document.getVersion()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bitmap imgBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

        ByteArrayOutputStream stream = new ByteArrayOutputStream();


        imgBitmap.compress(Bitmap.CompressFormat.JPEG, 99, stream);
        byte[] byteArr = stream.toByteArray();

        try {
            Rectangle one = new Rectangle(imgBitmap.getWidth(), imgBitmap.getHeight());
            document.setPageSize(one);
            document.setMargins(0, 0, 0, 0);
            document.open();
            Image img = Image.getInstance(byteArr);
            document.add(img);

        } catch (DocumentException | IOException de) {
            System.err.println(de.getMessage());
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        document.close();
    }


}

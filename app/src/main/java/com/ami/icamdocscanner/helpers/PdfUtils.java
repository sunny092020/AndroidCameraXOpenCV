package com.ami.icamdocscanner.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
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
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Document document = getDocument(outPath);
        for (int i = 0; i < imageFiles.size(); i++) {
            RecyclerImageFile imgFile = imageFiles.get(i);
            printpage(imgFile, stream, document);
        }
        document.close();
    }

    public static void toPDFSingle(RecyclerImageFile imageFile, String outPath) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Document document = getDocument(outPath);
        printpage(imageFile, stream, document);
        document.close();
    }

    private static Document getDocument(String outPath) {
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

        return document;
    }

    private static void printpage(RecyclerImageFile imageFile, ByteArrayOutputStream stream, Document document) {
        byte[] byteArr;

        Bitmap imgBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

        imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byteArr = stream.toByteArray();

        try {
            Rectangle one = new Rectangle(imgBitmap.getWidth(), imgBitmap.getHeight());
            document.setPageSize(one);
            document.setMargins(0, 0, 0, 0);

            // step 3: we open the document
            document.open();

            // step 4: we add a paragraph to the document
            Image img = Image.getInstance(byteArr);
            document.add(img);

        } catch (DocumentException | IOException de) {
            System.err.println(de.getMessage());
        }
    }
}

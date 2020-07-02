package com.ami.icamdocscanner;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.ami.icamdocscanner.helpers.ScannerConstants;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageEditActivity extends AppCompatActivity {
    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private ImageView imageView;
    FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);

        imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(ScannerConstants.cropImageBitmap);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        setFrameLayoutRatio();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ScannerConstants.cropImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArr = stream.toByteArray();

        // step 1: creation of a document-object
        Document document = new Document();
        try {
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file
            PdfWriter instance = PdfWriter.getInstance(document, new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "ami.pdf"));

            Rectangle one = new Rectangle(ScannerConstants.cropImageBitmap.getWidth(),ScannerConstants.cropImageBitmap.getHeight());
            document.setPageSize(one);
            document.setMargins(0, 0, 0, 0);

            // step 3: we open the document
            document.open();
            instance.getInfo().put(PdfName.CREATOR, new PdfString(Document.getVersion()));
            // step 4: we add a paragraph to the document
            Image img = Image.getInstance(byteArr);
            document.add(img);

        } catch (DocumentException | IOException de) {
            System.err.println(de.getMessage());
        }

        // step 5: we close the document
        document.close();

    }

    private void setFrameLayoutRatio() {
        frameLayout = findViewById(R.id.frameLayout);

        // Gets the layout params that will allow you to resize the layout
        ViewGroup.LayoutParams params = frameLayout.getLayoutParams();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        // Changes the height and width to the specified *pixels*
        params.width = width;
        params.height = width*4/3;

        frameLayout.setLayoutParams(params);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));
            imageView.setScaleX(mScaleFactor);
            imageView.setScaleY(mScaleFactor);
            return true;
        }
    }
}
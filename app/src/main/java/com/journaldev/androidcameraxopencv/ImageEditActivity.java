package com.journaldev.androidcameraxopencv;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.os.Environment;

import com.journaldev.androidcameraxopencv.helpers.ScannerConstants;

import java.io.FileOutputStream;
import java.io.IOException;

public class ImageEditActivity extends AppCompatActivity {
    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);

        imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(ScannerConstants.cropImageBitmap);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        String filename = Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES) + "CAPTURE.jpg";
        try (FileOutputStream out = new FileOutputStream(filename)) {
            ScannerConstants.cropImageBitmap.compress(Bitmap.CompressFormat.JPEG, 99, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            e.printStackTrace();
        }
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
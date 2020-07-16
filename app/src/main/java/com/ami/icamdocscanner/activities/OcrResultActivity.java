package com.ami.icamdocscanner.activities;

import androidx.appcompat.app.AppCompatActivity;
import com.ami.icamdocscanner.R;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OcrResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_result);

        String ocrText =  getIntent().getStringExtra("ocr_text");

        TextView ocrTextView = findViewById(R.id.ocrText);
        ocrTextView.setText(ocrText);

        Log.d("ocr_text", ocrText);

    }
}
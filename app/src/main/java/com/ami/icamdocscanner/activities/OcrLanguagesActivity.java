package com.ami.icamdocscanner.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.adapters.OcrLanguageAdapter;
import com.ami.icamdocscanner.models.OcrLanguage;

import java.util.ArrayList;
import java.util.List;

public class OcrLanguagesActivity extends AppCompatActivity {
    private OcrLanguageAdapter adapter;
    private static String[][] LANGS = {
        {"eng", "English"},
        {"vie", "Tiếng Việt"},
        {"jpn", "日本語"},
        {"chi_sim", "简化字"},
        {"chi_tra", "繁体字"},
        {"deu", "Deutsch"},
        {"fra", "Français"},
        {"rus", "Русский"},
        {"spa", "Español"},
        {"por", "Português"},
        {"hin", "हिन्दी"},
        {"kor", "한국어"},
        {"ita", "Italiano"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_languages);
        Log.d("onCreate", "onCreate");
        setupAdapter();
    }

    private void setupAdapter() {
        adapter = null;

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.langs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OcrLanguageAdapter(this);

        List<OcrLanguage> langs = new ArrayList<>();

        for(String[] lang: LANGS) {
            OcrLanguage localLang = new OcrLanguage(lang[0], lang[1]);
            langs.add(localLang);
        }
        adapter.setLangs(langs);
        recyclerView.setAdapter(adapter);
    }
}

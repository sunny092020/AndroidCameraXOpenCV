package com.ami.icamdocscanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class ImageDoneActivity extends AppCompatActivity {
    FileRecyclerViewAdapter adapter;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_done);

        if(ContextCompat.checkSelfPermission(ImageDoneActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(ImageDoneActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

        File directory = this.getFilesDir();

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.files);
        int numberOfColumns = 2;
        recyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));

        adapter = new FileRecyclerViewAdapter(this, listFiles(directory));
        recyclerView.setAdapter(adapter);
    }

    private RecyclerImageFile[] listFiles(File directory) {
        File[] files = directory.listFiles(File::isFile);
        Arrays.sort( files, (Comparator) (o1, o2) -> {
            long lastModified1 = ((File)o1).lastModified();
            long lastModified2 = ((File)o2).lastModified();
            return Long.compare(lastModified2, lastModified1);
        });

        RecyclerImageFile[] recyclerImageFiles = new RecyclerImageFile[files.length];

        for(int i = 0; i<files.length; i++) {
            recyclerImageFiles[i] = new RecyclerImageFile(files[i]);
        }

        return recyclerImageFiles;
    }
}

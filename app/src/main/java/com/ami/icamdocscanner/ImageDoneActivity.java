package com.ami.icamdocscanner;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_done);

        context = this;

        if(ContextCompat.checkSelfPermission(ImageDoneActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(ImageDoneActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

        setupAdapter();

        setupButtonListener();
    }

    private void setupAdapter() {
        adapter = null;

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

    private void setupButtonListener() {
        LinearLayout deleteBtn = findViewById(R.id.deleteBtn);

        deleteBtn.setOnClickListener(v -> {
            if(adapter.getSelected().size() == 0) {
                showToast("No Selection");
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.delete_file_message)
                    .setTitle(R.string.delete_file_title);

            // Add the buttons
            builder.setPositiveButton(R.string.ok, (dialog, id) -> {
                // User clicked OK button

                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < adapter.getSelected().size(); i++) {
                    File file = adapter.getSelected().get(i);
                    stringBuilder.append(file);
                    stringBuilder.append("\n");
                    boolean delete = file.delete();
                    Log.d("delete", Boolean.toString(delete));
                }
                showToast(stringBuilder.toString().trim());
                setupAdapter();
            });
            builder.setNegativeButton(R.string.cancel, (dialog, id) -> {});

            AlertDialog dialog = builder.create();

            dialog.show();

        });
    }

    private void showToast(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}

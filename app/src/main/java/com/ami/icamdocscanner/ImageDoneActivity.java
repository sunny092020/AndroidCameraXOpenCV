package com.ami.icamdocscanner;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ImageDoneActivity extends AppCompatActivity {
    FileRecyclerViewAdapter adapter;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    Context context;
    private final int LAUNCH_SECOND_ACTIVITY = 1;

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
        assert files != null;
        Arrays.sort( files, (Comparator<File>) (o1, o2) -> {
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
        setupDeleteButtonListener();
        setupShareButtonListener();
    }

    private void setupShareButtonListener() {
        LinearLayout shareBtn = findViewById(R.id.shareBtn);

        shareBtn.setOnClickListener(v -> {
            if(adapter.getSelected().size() == 0) {
                showToast("No Selection");
                return;
            }

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Here are some files.");
            intent.setType("image/jpeg");

            List<Uri> files = new ArrayList<>();

            for (int i = 0; i < adapter.getSelected().size(); i++) {
                File file = adapter.getSelected().get(i);

                Log.d("file ", file.getAbsolutePath());

                Uri fileUri = FileProvider.getUriForFile(
                        ImageDoneActivity.this,
                        "com.ami.icamdocscanner",
                        file);
                files.add(fileUri);
            }

            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, (ArrayList<? extends Parcelable>) files);
            startActivityForResult(intent, LAUNCH_SECOND_ACTIVITY);
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LAUNCH_SECOND_ACTIVITY) {
            if(resultCode == Activity.RESULT_OK){
                Log.d("result", "OK");
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
                Log.d("result", "cancel");
            }
        }
    }//onActivityResult

    private void setupDeleteButtonListener() {
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

                int numDeleted = 0;
                for (int i = 0; i < adapter.getSelected().size(); i++) {
                    File file = adapter.getSelected().get(i);
                    if(file.delete()) numDeleted++;
                }
                showToast(numDeleted + " files has been deleted.");
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

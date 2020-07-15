package com.ami.icamdocscanner.activities;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.adapters.FileRecyclerViewAdapter;
import com.ami.icamdocscanner.helpers.Downloader;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.OcrUtils;
import com.ami.icamdocscanner.helpers.Preferences;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ImageDoneActivity extends AppCompatActivity {
    FileRecyclerViewAdapter adapter;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    Context context;
    private final int LAUNCH_SECOND_ACTIVITY = 1;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;

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
        setupNavigationDrawer();
    }

    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        NavigationView navigationView = findViewById(R.id.navigationView);

        navigationView.setNavigationItemSelectedListener((NavigationView.OnNavigationItemSelectedListener) menuItem -> {
            int id=menuItem.getItemId();

            if (id==R.id.settings){
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    private List<RecyclerImageFile> listFiles(File directory) {
        File[] files = directory.listFiles(File::isFile);
        assert files != null;
        Arrays.sort( files, (Comparator<File>) (o1, o2) -> {
            long lastModified1 = ((File)o1).lastModified();
            long lastModified2 = ((File)o2).lastModified();
            return Long.compare(lastModified2, lastModified1);
        });

        List<RecyclerImageFile> recyclerImageFiles = new ArrayList<>();

        for (File file : files) {
            recyclerImageFiles.add(new RecyclerImageFile(file));
        }

        return recyclerImageFiles;
    }

    private void setupButtonListener() {
        setupOcrButtonListener();
        setupDeleteButtonListener();
        setupShareButtonListener();
        setupRetakeButtonListener();
        setupPdfButtonListener();
    }

    private void setupOcrButtonListener() {
        LinearLayout ocrBtn = findViewById(R.id.ocrBtn);

        ocrBtn.setOnClickListener(v -> {
            if(adapter.getSelectedByExtension("jpg").size() == 0) {
                showToast("No Images Selection");
                return;
            }

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterByStatus(DownloadManager.STATUS_RUNNING|DownloadManager.STATUS_PENDING|DownloadManager.STATUS_PAUSED);

            Cursor cursor = Downloader.downloadManager.query(query);
            if(cursor.getCount()>0) {
                showToast("Downloading Ocr Data ...");
                return;
            }

            String text = OcrUtils.ocr(context, adapter.getSelected().get(0), Preferences.getUsedLangs(this));
            Log.d("ocr text", text);
        });
    }

    private void setupPdfButtonListener() {
        LinearLayout pdfBtn = findViewById(R.id.pdfBtn);

        pdfBtn.setOnClickListener(v -> {
            if(adapter.getSelectedByExtension("jpg").size() == 0) {
                showToast("No Images Selection");
                return;
            }

            Intent intent = new Intent(this, PdfActivity.class);
            intent.putExtra("selectedFiles", (Serializable) adapter.getSelectedByExtension("jpg"));
            startActivity(intent);
            finish();

        });
    }

    private void setupRetakeButtonListener() {
        ImageView retakeBtn = findViewById(R.id.retakeBtn);

        retakeBtn.setOnClickListener(v -> {
            ScannerState.resetScannerState();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
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
                    RecyclerImageFile file = adapter.getSelected().get(i);
                    if(file.delete()) numDeleted++;
                    FileUtils.removeThumbnail(file);
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

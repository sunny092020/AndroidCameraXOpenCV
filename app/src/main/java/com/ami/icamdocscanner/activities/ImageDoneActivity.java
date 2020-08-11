package com.ami.icamdocscanner.activities;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.ami.icamdocscanner.helpers.ActivityUtils;
import com.ami.icamdocscanner.helpers.Downloader;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.OcrUtils;
import com.ami.icamdocscanner.helpers.Preferences;
import com.ami.icamdocscanner.helpers.ScannerConstant;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.google.android.material.navigation.NavigationView;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImageDoneActivity extends AppCompatActivity implements TessBaseAPI.ProgressNotifier {
    FileRecyclerViewAdapter adapter;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    Context context;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }

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

        navigationView.setNavigationItemSelectedListener(menuItem -> {
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
        if(ScannerState.getDoneImages().size() > 0) {
            displayDoneImages();
        } else {
            displayDir();
        }
    }

    private void displayDoneImages() {
        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.files);
        int numberOfColumns = 2;
        recyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));

        // Get a Calendar and set it to the current time.
        Calendar cal = Calendar.getInstance();
        cal.setTime(Date.from(Instant.now()));

        String folderName = String.format(Locale.US, "AMI_ICAMDOC_SCANNER-%1$tY-%1$tm-%1$td-%1$tk-%1$tS-%1$tp", cal);
        FileUtils.ensureDir(context, folderName);

        for(RecyclerImageFile file: ScannerState.getDoneImages()) {
            String filename = FileUtils.home(context) + "/" + folderName + "/" + FileUtils.getOriginFileName(file.getName());
            ScannerState.getSavedImages().add(new RecyclerImageFile(filename));
        }

        adapter = new FileRecyclerViewAdapter(this, ScannerState.getSavedImages());
        recyclerView.setAdapter(adapter);

        new Thread(() -> {
            for(int position=0; position<ScannerState.getDoneImages().size(); position++) {
                RecyclerImageFile file = ScannerState.getDoneImages().get(position);
                String filename = FileUtils.home(context) + "/" + folderName + "/" + FileUtils.getOriginFileName(file.getName());
                file.waitUntilSaved();
                RecyclerImageFile savedFile = ScannerState.getFileByName(filename, ScannerState.getSavedImages());
                FileUtils.copyFileUsingChannel(file, savedFile);

                String thumbnailPath = savedFile.getParent() + "/thumbnails/" + savedFile.getName();
                Log.d("thumbnailPath", thumbnailPath);
                FileUtils.createThumbnail(savedFile, thumbnailPath);

                if(position==0) {
                    String dirThumbnailPath = FileUtils.home(context) + "/thumbnails/" + folderName + ".jpg";
                    Log.d("dirThumbnailPath", dirThumbnailPath);

                    FileUtils.createThumbnail(savedFile, dirThumbnailPath);
                }

                int finalPosition = position;
                runOnUiThread(() -> {
                    adapter.notifyItemChanged(finalPosition);
                });
            }
            FileUtils.deleteTempDir(context);
            ScannerState.getCropImages().clear();
            ScannerState.getEditImages().clear();
            ScannerState.getDoneImages().clear();
        }).start();
    }

    private void displayDir() {
        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.files);
        int numberOfColumns = 2;
        recyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));

        File directory = (RecyclerImageFile) getIntent().getSerializableExtra("directory");
        if(directory==null) directory = context.getFilesDir();
        adapter = new FileRecyclerViewAdapter(this, FileUtils.listFiles(directory));
        recyclerView.setAdapter(adapter);
    }

    private void setupButtonListener() {
        setupOcrButtonListener();
        setupDeleteButtonListener();
        setupShareButtonListener();
        setupRetakeButtonListener();
        setupChoosePhotoButtonListener();
        setupPdfButtonListener();
    }

    private void setupOcrButtonListener() {
        LinearLayout ocrBtn = findViewById(R.id.ocrBtn);
        LinearLayout progressBarHolder = findViewById(R.id.progressBarHolder);
        progressBarHolder.setVisibility(View.GONE);

        ocrBtn.setOnClickListener(v -> {
            if(adapter.getSelectedByExtension("jpg").size() == 0) {
                showToast("No Images Selection");
                return;
            }

            if(adapter.getSelectedByExtension("jpg").size() > 1) {
                showToast("can only ocr 1 Image at a time");
                return;
            }

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterByStatus(DownloadManager.STATUS_RUNNING|DownloadManager.STATUS_PENDING|DownloadManager.STATUS_PAUSED);

            if(Downloader.downloadManager != null) {
                Cursor cursor = Downloader.downloadManager.query(query);
                if(cursor.getCount()>0) {
                    showToast("Downloading Ocr Data ...");
                    return;
                }
            }

            progressBarHolder.setVisibility(View.VISIBLE);
            new Thread(() -> {
                String text = OcrUtils.ocr(context, adapter.getSelected().get(0), Preferences.getUsedLangs(this));
                runOnUiThread(() -> progressBarHolder.setVisibility(View.GONE));

                Intent intent = new Intent(this, OcrResultActivity.class);
                intent.putExtra("ocr_text", text);
                startActivity(intent);
            }).start();
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

    private void setupChoosePhotoButtonListener() {
        ImageView choosePhotoBtn = findViewById(R.id.choosePhotoBtn);
        choosePhotoBtn.setOnClickListener(v -> {
            ScannerState.resetScannerState();
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            // The MIME data type filter
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            // Only return URIs that can be opened with ContentResolver
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, ScannerConstant.LAUNCH_FILE_PICKER);
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ScannerConstant.LAUNCH_FILE_PICKER) {
            if(resultCode == Activity.RESULT_OK) {
                ActivityUtils.filePickerProcessResult(context, data);
                Intent cropIntent = new Intent(this, ImageCropActivity.class);
                startActivity(cropIntent);
                finish();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
                Log.d("LAUNCH_FILE_PICKER result", "cancel");
            }
        }
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
            startActivityForResult(intent, ScannerConstant.LAUNCH_SECOND_ACTIVITY);
        });
    }

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

                for (int i = 0; i < adapter.getSelected().size(); i++) {
                    RecyclerImageFile file = adapter.getSelected().get(i);
                    FileUtils.removeThumbnail(file);
                    if(file.isFile()) {
                        file.delete();
                    } else {
                        FileUtils.deleteDirectoryStream(file);
                    }
                }
                adapter.exitActionMode();

                File directory = (RecyclerImageFile) getIntent().getSerializableExtra("directory");
                if(directory==null) directory = context.getFilesDir();
                adapter.setFiles(FileUtils.listFiles(directory));
                adapter.notifyDataSetChanged();
                showToast("Files has been deleted.");

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

    @Override
    public void onProgressValues(TessBaseAPI.ProgressValues progressValues) {
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setProgress(progressValues.getPercent());
    }

}

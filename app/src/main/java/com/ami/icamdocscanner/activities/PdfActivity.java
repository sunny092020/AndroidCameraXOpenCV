package com.ami.icamdocscanner.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.adapters.FileRecyclerViewAdapter;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.OnStartDragListener;
import com.ami.icamdocscanner.helpers.PdfUtils;
import com.ami.icamdocscanner.helpers.SimpleItemTouchHelperCallback;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PdfActivity extends AppCompatActivity implements OnStartDragListener {
    FileRecyclerViewAdapter adapter;
    private ItemTouchHelper mItemTouchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        List<RecyclerImageFile> selectedFiles = (List<RecyclerImageFile>) getIntent().getSerializableExtra("selectedFiles");

        setupAdapter(selectedFiles);
        setupButtonListener();
    }

    private void setupButtonListener() {
        setupSingleButtonListener();
        setupMultiButtonListener();
    }

    private void setupSingleButtonListener() {
        LinearLayout singleBtn = findViewById(R.id.singleBtn);

        singleBtn.setOnClickListener(v -> {
            if(adapter.getSelected().size() == 0) {
                showToast("No Selection");
                return;
            }

            String outPath;

            for (int i = 0; i < adapter.getSelected().size(); i++) {
                RecyclerImageFile imgFile = adapter.getSelected().get(i);
//                outPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + FileUtils.fileNameWithoutExtension(imgFile.getName());

                outPath =  imgFile.getParent() + "/" + FileUtils.fileNameWithoutExtension(imgFile.getName());
                PdfUtils.toPDFSingle(imgFile, outPath);

                String thumbnailPath = imgFile.getParent() + "/thumbnails/" + FileUtils.fileNameWithoutExtension(imgFile.getName()) + "-pdf.jpg";
                FileUtils.createThumbnail(imgFile, thumbnailPath);
            }

            Intent intent = new Intent(this, ImageDoneActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupMultiButtonListener() {
        LinearLayout multiBtn = findViewById(R.id.multiBtn);

        multiBtn.setOnClickListener(v -> {
            if(adapter.getSelected().size() == 0) {
                showToast("No Selection");
                return;
            }

            // Get a Calendar and set it to the current time.
            Calendar cal = Calendar.getInstance();
            cal.setTime(Date.from(Instant.now()));

            // Create a filename from a format string.
            // ... Apply date formatting codes.
            String filename = String.format("AMI_ICAMDOC_SCANNER-%1$tY-%1$tm-%1$td-%1$tk-%1$tS-%1$tp", cal);

            String outPath = adapter.getSelected().get(0).getParent() + "/" + filename;
//            String outPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/ami";

            PdfUtils.toPDFMulti(adapter.getSelected(), outPath);

            String thumbnailPath = adapter.getSelected().get(0).getParent() + "/thumbnails/" + filename + "-pdf.jpg";
            FileUtils.createThumbnail(adapter.getSelected().get(0), thumbnailPath);

            Intent intent = new Intent(this, ImageDoneActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupAdapter(List<RecyclerImageFile> selectedFiles) {
        adapter = null;

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.files);
        int numberOfColumns = 2;
        recyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));

        adapter = new FileRecyclerViewAdapter(this, selectedFiles, this);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    private void showToast(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
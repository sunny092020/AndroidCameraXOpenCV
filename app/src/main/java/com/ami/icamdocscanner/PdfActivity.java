package com.ami.icamdocscanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import com.ami.icamdocscanner.helpers.OnStartDragListener;
import com.ami.icamdocscanner.helpers.SimpleItemTouchHelperCallback;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.io.File;
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
}
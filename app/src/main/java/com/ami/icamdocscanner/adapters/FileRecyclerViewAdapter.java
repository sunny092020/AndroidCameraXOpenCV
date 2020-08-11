package com.ami.icamdocscanner.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.activities.ImageDisplayActivity;
import com.ami.icamdocscanner.activities.ImageDoneActivity;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.ItemTouchHelperViewHolder;
import com.ami.icamdocscanner.helpers.OnStartDragListener;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileRecyclerViewAdapter extends RecyclerView.Adapter<FileRecyclerViewAdapter.ViewHolder> implements ItemTouchHelperAdapter {
    private List<RecyclerImageFile> files;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private OnStartDragListener mDragStartListener;
    ActionMode actionMode;

    // data is passed into the constructor
    public FileRecyclerViewAdapter(Context context, List<RecyclerImageFile> data) {
        this.mInflater = LayoutInflater.from(context);
        this.files = data;
    }

    public FileRecyclerViewAdapter(Context context, List<RecyclerImageFile> data, OnStartDragListener dragStartListener) {
        mDragStartListener = dragStartListener;
        this.mInflater = LayoutInflater.from(context);
        this.files = data;
    }

    // inflates the cell layout from xml when needed
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.file_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each cell
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public void onItemDismiss(int position) {
        files.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(files, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return files.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, ItemTouchHelperViewHolder {
        ImageView thumbnail, check;
        TextView fileName;
        View itemView;

        ViewHolder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.fileThumbnail);
            check = itemView.findViewById(R.id.check);
            fileName = itemView.findViewById(R.id.fileName);
            this.itemView = itemView;
            this.itemView.setOnClickListener(this);
        }

        void bind(int position) {
            RecyclerImageFile file = files.get(position);

            check.setVisibility(file.isChecked() ? View.VISIBLE : View.GONE);

            Bitmap thumbnailBitmap = FileUtils.getThumbnailNoCreate(file);
            thumbnail.setImageBitmap(thumbnailBitmap);

            fileName.setText(file.getName());
            itemView.setOnClickListener(view -> {
                if(actionMode == null) {
                    Context context = itemView.getContext();
                    if(file.isDirectory()) {
                        Intent doneIntent = new Intent(context, ImageDoneActivity.class);
                        doneIntent.putExtra("directory", (Serializable) file);
                        context.startActivity(doneIntent);
                    } else if(file.isFile()) {
                        Intent displayIntent = new Intent(context, ImageDisplayActivity.class);
                        RecyclerImageFile parentDir = new RecyclerImageFile(file.getParent());
                        displayIntent.putExtra("directory", (Serializable) parentDir);
                        displayIntent.putExtra("position", position);
                        context.startActivity(displayIntent);
                    }
                    return;
                }

                file.setChecked(!file.isChecked());
                check.setVisibility(file.isChecked() ? View.VISIBLE : View.GONE);
                actionMode.setTitle(getSelected().size() + " selected");
            });

            itemView.setOnLongClickListener(view -> {
                file.setChecked(true);
                check.setVisibility(View.VISIBLE);

                if (actionMode == null) {
                    // Start the contextual action bar
                    // using the ActionMode.Callback.
                    Activity activity = (Activity) itemView.getContext();
                    actionMode = activity.startActionMode(mActionModeCallback);
                    actionMode.setTitle("1 selected");
                }
                return true;
            });
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }

        @Override
        public void onItemSelected() {
            thumbnail.setBackgroundResource(R.drawable.selected_pdf_borders);
        }

        @Override
        public void onItemClear() {
            thumbnail.setBackgroundResource(android.R.color.transparent);
        }
    }

    public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        private boolean selectAll = true;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_context, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
            //do nothing if nothing is clicked
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.select_context:
                    if(selectAll) {
                        item.setTitle("Deselect All");
                    } else {
                        item.setTitle("Select All");
                    }

                    selectAll(selectAll);
                    selectAll = !selectAll;
                    actionMode.setTitle(getSelected().size() + " selected");

                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectAll(false);
            actionMode = null;
        }
    };

    // convenience method for getting data at click position
    RecyclerImageFile getItem(int id) {
        return files.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    public List<RecyclerImageFile> getAll() {
        return files;
    }

    public void selectAll(boolean bool) {
        for(RecyclerImageFile f: files) {
            f.setChecked(bool);
        }
        notifyDataChanged();
    }

    public void notifyDataChanged() {
        this.notifyDataSetChanged();
    }

    public List<RecyclerImageFile> getSelected() {
        List<RecyclerImageFile> selected = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).isChecked()) {
                selected.add(files.get(i));
            }
        }
        return selected;
    }

    public List<RecyclerImageFile> getSelectedByExtension(String extension) {
        List<RecyclerImageFile> selected = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).isChecked() && FileUtils.isFileType(files.get(i).getName(), extension)) {
                selected.add(files.get(i));
            }
        }
        return selected;
    }
}

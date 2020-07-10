package com.ami.icamdocscanner.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.models.RecyclerImageFile;

public class ViewPagerEditAdapter extends RecyclerView.Adapter<ViewPagerEditAdapter.ViewHolder> {
    private LayoutInflater mInflater;

    public ViewPagerEditAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_edit_viewpager, parent, false);
        return new ViewPagerEditAdapter.ViewHolder(view);
    }

    public void onBindViewHolder(ViewPagerEditAdapter.ViewHolder holder, int position) {
        holder.bind(position);
    }

    public int getItemCount() {
        return ScannerState.croppedImages.size();
    }

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        View itemView;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            imageView = itemView.findViewById(R.id.imageView);
        }

        void bind(int currentImagePosition) {
            RecyclerImageFile file = ScannerState.croppedImages.get(currentImagePosition);
            Bitmap bitmap = FileUtils.readBitmap(file.getAbsolutePath());
            imageView.setImageBitmap(bitmap);
        }
    }
}

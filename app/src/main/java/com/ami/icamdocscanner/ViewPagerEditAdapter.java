package com.ami.icamdocscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.models.RecyclerImageFile;

public class ViewPagerEditAdapter {
    private LayoutInflater mInflater;

    ViewPagerEditAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
    }

    public ViewPagerEditAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_crop_viewpager, parent, false);
        return new ViewPagerEditAdapter.ViewHolder(view);
    }

    public void onBindViewHolder(ViewPagerCropAdapter.ViewHolder holder, int position) {
        RecyclerImageFile file = ScannerState.croppedImages.get(position);
        holder.bind(file);
    }

    public int getItemCount() {
        return ScannerState.croppedImages.size();
    }

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout frameLayout;
        ImageView imageView;
        View itemView;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            frameLayout = itemView.findViewById(R.id.frameLayout);
            imageView = itemView.findViewById(R.id.imageView);
        }

        void bind(RecyclerImageFile file) {
            Bitmap bitmap = FileUtils.readBitmap(file.getAbsolutePath());
        }

    }
}

package com.ami.icamdocscanner.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.models.RecyclerImageFile;
import com.ami.icamdocscanner.models.TouchViewPagerImageView;

import java.util.List;

public class ViewPagerEditAdapter extends RecyclerView.Adapter<ViewPagerEditAdapter.ViewHolder> {
    private List<RecyclerImageFile> images;
    private LayoutInflater mInflater;
    private ViewPager2 viewPager;

    public ViewPagerEditAdapter(Context context, List<RecyclerImageFile> images, ViewPager2 viewPager) {
        this.mInflater = LayoutInflater.from(context);
        this.images = images;
        this.viewPager = viewPager;
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_edit_viewpager, parent, false);
        return new ViewPagerEditAdapter.ViewHolder(view, viewPager);
    }

    public void onBindViewHolder(ViewPagerEditAdapter.ViewHolder holder, int position) {
        RecyclerImageFile image = images.get(position);
        holder.bind(image);
    }

    public int getItemCount() {
        return images.size();
    }

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TouchViewPagerImageView imageView;
        View itemView;

        ViewHolder(View itemView, ViewPager2 viewPager) {
            super(itemView);
            this.itemView = itemView;
            imageView = itemView.findViewById(R.id.imageView);
            imageView.setViewPagerCrop(viewPager);
            setFrameLayoutRatio();
        }

        void bind(RecyclerImageFile image) {
            imageView.resetScale();
            imageView.setImageBitmap(null);
            new Thread(() -> {
                Bitmap bitmap;
                if(FileUtils.fileExtension(image.getName()).equalsIgnoreCase("pdf")) {
                    bitmap = FileUtils.getThumbnailNoCreate(image);
                } else {
                    bitmap = FileUtils.readBitmap(image);
                }
                Activity activity = (Activity) itemView.getContext();
                activity.runOnUiThread(() -> {
                    imageView.setImageBitmap(bitmap);
                });
            }).start();
        }

        void setFrameLayoutRatio() {
            FrameLayout frameLayout = itemView.findViewById(R.id.frameLayout);

            // Gets the layout params that will allow you to resize the layout
            ViewGroup.LayoutParams params = frameLayout.getLayoutParams();

            DisplayMetrics displayMetrics = new DisplayMetrics();
            Activity activity = (Activity) itemView.getContext();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;

            // Changes the height and width to the specified *pixels*
            params.width = width;
            params.height = width*4/3;

            frameLayout.setLayoutParams(params);
        }
    }
}

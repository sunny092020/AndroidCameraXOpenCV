package com.ami.icamdocscanner.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.activities.ImageEditActivity;
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
        return ScannerState.editImages.size();
    }

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        View itemView;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            imageView = itemView.findViewById(R.id.imageView);
            setFrameLayoutRatio();
        }

        void bind(int currentImagePosition) {
            RecyclerImageFile file = ScannerState.editImages.get(currentImagePosition);

            while (!file.exists()) {
                try {
                    Thread.sleep(100);
                    file = ScannerState.editImages.get(currentImagePosition);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Bitmap bitmap = FileUtils.readBitmap(file.getAbsolutePath());

            while (bitmap== null) {
                try {
                    Thread.sleep(100);
                    bitmap = FileUtils.readBitmap(file.getAbsolutePath());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            imageView.setImageBitmap(bitmap);
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
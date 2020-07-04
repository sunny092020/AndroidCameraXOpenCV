package com.ami.icamdocscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ami.icamdocscanner.helpers.ScannerConstants;
import com.ami.icamdocscanner.helpers.VisionUtils;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class FileRecyclerViewAdapter extends RecyclerView.Adapter<FileRecyclerViewAdapter.ViewHolder> {
    private File[] mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    FileRecyclerViewAdapter(Context context, File[] data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
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
        File file = mData[position];

        String thumbnailPath = file.getParent() + "/thumbnails/" + file.getName();

        File thumbnail = new File(thumbnailPath);
        if(thumbnail.exists()) {
            Bitmap thumbnailBitmap = BitmapFactory.decodeFile(thumbnail.getAbsolutePath());
            holder.thumbnail.setImageBitmap(thumbnailBitmap);
        }else {
            File directory = new File(file.getParent() + "/thumbnails/");
            if (! directory.exists()){
                directory.mkdir();
            }

            //Instantiating the Imagecodecs class
            Imgcodecs imageCodecs = new Imgcodecs();

            //Reading the Image from the file
            Mat origin = imageCodecs.imread(file.getAbsolutePath());
            double DOWNSCALE_IMAGE_SIZE = 200f;

            // Downscale image for better performance.
            double ratio = DOWNSCALE_IMAGE_SIZE / Math.max(origin.width(), origin.height());

            Mat smallOrigin = VisionUtils.downscaleMat(origin, ratio);
            Bitmap smallOriginBitmap = VisionUtils.matToBitmap(smallOrigin);

            holder.thumbnail.setImageBitmap(smallOriginBitmap);
            try (FileOutputStream out = new FileOutputStream(thumbnailPath)) {
                smallOriginBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        holder.fileName.setText(file.getName());
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return mData.length;
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView thumbnail;
        TextView fileName;

        ViewHolder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.fileThumbnail);
            fileName = itemView.findViewById(R.id.fileName);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    File getItem(int id) {
        return mData[id];
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

}

package com.ami.icamdocscanner.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.helpers.FileUtils;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.helpers.VisionUtils;
import com.ami.icamdocscanner.libraries.PolygonView;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ViewPagerCropAdapter extends RecyclerView.Adapter<ViewPagerCropAdapter.ViewHolder> {
    private LayoutInflater mInflater;
    private ViewPager2 viewPagerCrop;

    public ViewPagerCropAdapter(Context context, ViewPager2 viewPagerCrop) {
        this.mInflater = LayoutInflater.from(context);
        this.viewPagerCrop = viewPagerCrop;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_crop_viewpager, parent, false);
        return new ViewHolder(view, viewPagerCrop);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return ScannerState.getCropImages().size();
    }

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout frameLayout, holderImageCrop;
        RelativeLayout relativeLayout;
        ImageView imageView;
        PolygonView polygonView;
        View itemView;

        ViewHolder(View itemView, ViewPager2 viewCrop) {
            super(itemView);
            this.itemView = itemView;
            frameLayout = itemView.findViewById(R.id.frameLayout);
            relativeLayout = itemView.findViewById(R.id.container);
            imageView = itemView.findViewById(R.id.imageView);
            holderImageCrop = itemView.findViewById(R.id.holderImageCrop);
            polygonView = itemView.findViewById(R.id.polygonView);
            polygonView.setViewPagerCrop(viewCrop);
            polygonView.setHolderImageCrop(holderImageCrop);
        }

        void bind(int position) {
            Log.d("#################>>>>>> position", "" + position);

            Log.d("1", "" + new Timestamp(System.currentTimeMillis()));
            RecyclerImageFile file = ScannerState.getCropImages().get(position);
            MatOfPoint2f croppedPolygon = file.getCroppedPolygon();
            Log.d("2", "" + new Timestamp(System.currentTimeMillis()));

            while (croppedPolygon== null) {
                try {
                    Thread.sleep(100);
                    croppedPolygon = file.getCroppedPolygon();
                    Log.d("croppedPolygon", "" + croppedPolygon);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d("3", "" + new Timestamp(System.currentTimeMillis()));


            Bitmap bitmap = FileUtils.readBitmap(file.getAbsolutePath());
            Log.d("4", "" + new Timestamp(System.currentTimeMillis()));

            polygonView.setOriginSize(bitmap.getWidth(), bitmap.getHeight());
            Log.d("5", "" + new Timestamp(System.currentTimeMillis()));

            drawPolygonAsync(bitmap, file);
            Log.d("#################<<<<<<<<<< position", "" + position);
        }

        private void drawPolygonAsync(Bitmap bitmap, RecyclerImageFile file) {
            drawPolygon(bitmap, file);
        }

        private void drawPolygon(Bitmap originBitmap, RecyclerImageFile file) {
            // For those curious why this works, when onCreate is executed in your Activity, the UI has not been drawn to the screen yet,
            // so nothing has dimensions yet since they haven't been laid out on the screen. When setContentView is called,
            // a message is posted to the UI thread to draw the UI for your layout, but will happen in the future after onCreate
            // finishes executing. Posting a Runnable to the UI thread will put the Runnable at the end of the message queue for the UI thread,
            // so will be executed after the screen has been drawn, thus everything has dimensions
            holderImageCrop.post(() -> {
                Log.d("6", "" + new Timestamp(System.currentTimeMillis()));

                Bitmap scaledBitmap = VisionUtils.scaledBitmap(originBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                Log.d("7", "" + new Timestamp(System.currentTimeMillis()));

                imageView.setImageBitmap(scaledBitmap);
                Log.d("8", "" + new Timestamp(System.currentTimeMillis()));


                Bitmap tempBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                Log.d("9", "" + new Timestamp(System.currentTimeMillis()));

                Map<Integer, PointF> pointFs;
                try {
                    Log.d("10", "" + new Timestamp(System.currentTimeMillis()));

                    pointFs = getEdgePoints(tempBitmap, originBitmap, file.getCroppedPolygon());

                    Log.d("11", "" + new Timestamp(System.currentTimeMillis()));

                    polygonView.setPoints(pointFs);
                    polygonView.setVisibility(View.VISIBLE);

                    Log.d("12", "" + new Timestamp(System.currentTimeMillis()));

                    int padding = (int) itemView.getResources().getDimension(R.dimen.scanPadding);

                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
                    layoutParams.gravity = Gravity.CENTER;

                    Log.d("13", "" + new Timestamp(System.currentTimeMillis()));

                    polygonView.setLayoutParams(layoutParams);
                    polygonView.setPointColor(itemView.getResources().getColor(R.color.orange));
                    Log.d("14", "" + new Timestamp(System.currentTimeMillis()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap, Bitmap originBitmap, MatOfPoint2f croppedPolygon) {
            List<PointF> pointFs = getContourEdgePoints(originBitmap, croppedPolygon);
            return orderedValidEdgePoints(tempBitmap, pointFs);
        }

        private List<PointF> getContourEdgePoints(Bitmap originBitmap, MatOfPoint2f croppedPolygon) {
            MatOfPoint2f point2f = croppedPolygon;
            if (point2f == null)
                point2f = new MatOfPoint2f();
            List<Point> points = Arrays.asList(point2f.toArray());
            List<PointF> result = new ArrayList<>();

            float kx = (float) holderImageCrop.getWidth()/originBitmap.getWidth();
            float ky = (float) holderImageCrop.getHeight()/originBitmap.getHeight();
            float k = (Math.min(kx, ky));

            for (int i = 0; i < points.size(); i++) {
                result.add(new PointF(((float) points.get(i).x*k), ((float) points.get(i).y*k)));
            }
            return result;
        }

        private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
            Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);
            if (!polygonView.isValidShape(orderedPoints)) {
                orderedPoints = VisionUtils.getOutlinePoints(tempBitmap);
            }
            return orderedPoints;
        }
    }
}

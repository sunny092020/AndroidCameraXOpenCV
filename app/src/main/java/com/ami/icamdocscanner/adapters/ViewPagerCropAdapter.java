package com.ami.icamdocscanner.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

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
        return ScannerState.getOriginImages().size();
    }

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout frameLayout;
        ImageView imageView;
        PolygonView polygonView;
        View itemView;

        ViewHolder(View itemView, ViewPager2 viewCrop) {
            super(itemView);
            this.itemView = itemView;
            frameLayout = itemView.findViewById(R.id.frameLayout);
            imageView = itemView.findViewById(R.id.imageView);
            polygonView = itemView.findViewById(R.id.polygonView);
            polygonView.setViewPagerCrop(viewCrop);
        }

        void bind(int position) {
            RecyclerImageFile file = ScannerState.getOriginImages().get(position);
            new Thread(() -> {
                FrameLayout holderImageCrop = itemView.findViewById(R.id.holderImageCrop);

                holderImageCrop.post(() -> {
                    int holderCropWidth, holderCropHeight;
                    holderCropWidth = holderImageCrop.getWidth();
                    holderCropHeight = holderImageCrop.getHeight();
                    polygonView.setHolderCropWidth(holderCropWidth);
                    polygonView.setHolderCropHeight(holderCropHeight);

                    Bitmap bitmap = FileUtils.readBitmap(file);
                    Bitmap scaledBitmap = VisionUtils.scaledBitmap(bitmap, holderCropWidth, holderCropHeight);
                    imageView.setImageBitmap(scaledBitmap);
                    drawPolygon(file, holderCropWidth, holderCropHeight);
                });
            }).start();
        }

        private void drawPolygon(RecyclerImageFile file, int holderCropWidth, int holderCropHeight) {
            Size originSize = VisionUtils.size(file);
            polygonView.setOriginSize(originSize.getWidth(), originSize.getHeight());
            Bitmap tempBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

            Map<Integer, PointF> pointFs;
            try {
                pointFs = getEdgePoints(tempBitmap, file, originSize.getWidth(), originSize.getHeight(), holderCropWidth, holderCropHeight);
                polygonView.setPoints(pointFs);
                polygonView.setVisibility(View.VISIBLE);
                int padding = (int) itemView.getResources().getDimension(R.dimen.scanPadding);

                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
                layoutParams.gravity = Gravity.CENTER;
                polygonView.setLayoutParams(layoutParams);
                polygonView.setPointColor(itemView.getResources().getColor(R.color.orange));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap, RecyclerImageFile file, int originWidth, int originHeight, int holderCropWidth, int holderCropHeight) {
            List<PointF> pointFs = getContourEdgePoints(file, originWidth, originHeight, holderCropWidth, holderCropHeight);
            return orderedValidEdgePoints(tempBitmap, pointFs);
        }

        private List<PointF> getContourEdgePoints(RecyclerImageFile file, int originWidth, int originHeight,  int holderCropWidth, int holderCropHeight) {
            MatOfPoint2f point2f = file.getCroppedPolygon();
            if (point2f == null)
                point2f = new MatOfPoint2f();
            List<Point> points = Arrays.asList(point2f.toArray());

            List<PointF> result = new ArrayList<>();

            float kx = (float) holderCropWidth/originWidth;
            float ky = (float) holderCropHeight/originHeight;
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

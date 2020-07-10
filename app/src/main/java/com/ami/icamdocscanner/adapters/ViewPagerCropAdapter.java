package com.ami.icamdocscanner.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ViewPagerCropAdapter extends RecyclerView.Adapter<ViewPagerCropAdapter.ViewHolder> {
    private LayoutInflater mInflater;
    private ViewPager2 viewPager2;

    public ViewPagerCropAdapter(Context context, ViewPager2 viewPager2) {
        this.mInflater = LayoutInflater.from(context);
        this.viewPager2 = viewPager2;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_crop_viewpager, parent, false);
        return new ViewHolder(view, viewPager2);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RecyclerImageFile file = ScannerState.capturedImages.get(position);
        holder.bind(file);
    }

    @Override
    public int getItemCount() {
        return ScannerState.capturedImages.size();
    }

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout frameLayout, holderImageCrop;
        RelativeLayout relativeLayout;
        ImageView imageView;
        PolygonView polygonView;
        ProgressBar progressBar;
        View itemView;

        ViewHolder(View itemView, ViewPager2 viewPager2) {
            super(itemView);
            this.itemView = itemView;
            frameLayout = itemView.findViewById(R.id.frameLayout);
            relativeLayout = itemView.findViewById(R.id.container);
            imageView = itemView.findViewById(R.id.imageView);
            holderImageCrop = itemView.findViewById(R.id.holderImageCrop);
            polygonView = itemView.findViewById(R.id.polygonView);
            polygonView.setViewPager2(viewPager2);
            polygonView.setHolderImageCrop(holderImageCrop);
            progressBar = itemView.findViewById(R.id.progressBar);
            if (progressBar.getIndeterminateDrawable() != null && ScannerState.progressColor != null)
                progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(ScannerState.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
            else if (progressBar.getProgressDrawable() != null && ScannerState.progressColor != null)
                progressBar.getProgressDrawable().setColorFilter(Color.parseColor(ScannerState.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        }

        void bind(RecyclerImageFile file) {
            Bitmap bitmap = FileUtils.readBitmap(file.getAbsolutePath());
            polygonView.setOriginSize(bitmap.getWidth(), bitmap.getHeight());
            drawPolygonAsync(bitmap, file);
        }

        private void drawPolygonAsync(Bitmap bitmap, RecyclerImageFile file) {
            setProgressBar(true);
            drawPolygon(bitmap, file);
        }

        private void drawPolygon(Bitmap originBitmap, RecyclerImageFile file) {
            // For those curious why this works, when onCreate is executed in your Activity, the UI has not been drawn to the screen yet,
            // so nothing has dimensions yet since they haven't been laid out on the screen. When setContentView is called,
            // a message is posted to the UI thread to draw the UI for your layout, but will happen in the future after onCreate
            // finishes executing. Posting a Runnable to the UI thread will put the Runnable at the end of the message queue for the UI thread,
            // so will be executed after the screen has been drawn, thus everything has dimensions
            holderImageCrop.post(() -> {
                Bitmap scaledBitmap = VisionUtils.scaledBitmap(originBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                imageView.setImageBitmap(scaledBitmap);

                Bitmap tempBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                Map<Integer, PointF> pointFs;
                try {
                    pointFs = getEdgePoints(tempBitmap, originBitmap, file.getCroppedPolygon());

                    polygonView.setPoints(pointFs);
                    polygonView.setVisibility(View.VISIBLE);

                    int padding = (int) itemView.getResources().getDimension(R.dimen.scanPadding);

                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
                    layoutParams.gravity = Gravity.CENTER;

                    polygonView.setLayoutParams(layoutParams);
                    polygonView.setPointColor(itemView.getResources().getColor(R.color.orange));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    setProgressBar(false);
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

        private void showProgressBar() {
            RelativeLayout rlContainer = itemView.findViewById(R.id.container);
            setViewInteract(rlContainer, false);
            progressBar.setVisibility(View.VISIBLE);
        }

        private void hideProgressBar() {
            RelativeLayout rlContainer = itemView.findViewById(R.id.container);
            setViewInteract(rlContainer, true);
            progressBar.setVisibility(View.GONE);
        }

        private void setProgressBar(boolean isShow) {
            if (isShow)
                showProgressBar();
            else
                hideProgressBar();
        }

        private void setViewInteract(View view, boolean canDo) {
            view.setEnabled(canDo);
            if (view instanceof ViewGroup) {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                    setViewInteract(((ViewGroup) view).getChildAt(i), canDo);
                }
            }
        }

    }
}

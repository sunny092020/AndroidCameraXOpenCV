package com.ami.icamdocscanner.libraries;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Magnifier;

import androidx.viewpager2.widget.ViewPager2;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.helpers.ScannerState;
import com.ami.icamdocscanner.models.RecyclerImageFile;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jhansi on 28/03/15.
 */
public class PolygonView extends FrameLayout {

    protected Context context;
    private Paint paint;
    private ImageView pointer1;
    private ImageView pointer2;
    private ImageView pointer3;
    private ImageView pointer4;
    private ImageView midPointer13;
    private ImageView midPointer12;
    private ImageView midPointer34;
    private ImageView midPointer24;
    private PolygonView polygonView;
    private Magnifier magnifier;

    private ViewPager2 viewPagerCrop;
    private int holderCropWidth;

    public void setHolderCropWidth(int holderCropWidth) {
        this.holderCropWidth = holderCropWidth;
    }

    public void setHolderCropHeight(int holderCropHeight) {
        this.holderCropHeight = holderCropHeight;
    }

    public int getHolderCropWidth() {
        return holderCropWidth;
    }

    public int getHolderCropHeight() {
        return holderCropHeight;
    }

    private int holderCropHeight;
    private int originWidth=0, originHeight=0;

    public PolygonView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public PolygonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public PolygonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    public void setViewPagerCrop(ViewPager2 viewPagerCrop) {
        this.viewPagerCrop = viewPagerCrop;
    }

    public void setOriginSize(int originWidth, int originHeight) {
        this.originWidth = originWidth;
        this.originHeight = originHeight;
    }

    private void updatePolygon() {
        RecyclerImageFile file = ScannerState.getOriginImages().get(viewPagerCrop.getCurrentItem());
        file.setCroppedPolygon(getCroppedPolygon());
        file.setChanged(true);
    }

    private MatOfPoint2f getCroppedPolygon() {
        Map<Integer, PointF> mapPoints = getPoints();
        List<PointF> pointFs = new ArrayList<>(mapPoints.values());

        if(pointFs.size() != 4) return null;

        float kx = (float) holderCropWidth/originWidth;
        float ky = (float) holderCropHeight/originHeight;
        float k = (Math.min(kx, ky));

        Point[] points = {
                new Point(pointFs.get(0).x/k, pointFs.get(0).y/k),
                new Point(pointFs.get(1).x/k, pointFs.get(1).y/k),
                new Point(pointFs.get(2).x/k, pointFs.get(2).y/k),
                new Point(pointFs.get(3).x/k, pointFs.get(3).y/k),
        };
        return new MatOfPoint2f(points);
    }


    private void init() {
        polygonView = this;
        pointer1 = getImageView(0, 0);
        pointer2 = getImageView(getWidth(), 0);
        pointer3 = getImageView(0, getHeight());
        pointer4 = getImageView(getWidth(), getHeight());
        midPointer13 = getImageView(0, getHeight() / 2);
        midPointer13.setOnTouchListener(new MidPointTouchListenerImpl(pointer1, pointer3));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            magnifier = new Magnifier(polygonView);
        midPointer12 = getImageView(0, getWidth() / 2);
        midPointer12.setOnTouchListener(new MidPointTouchListenerImpl(pointer1, pointer2));

        midPointer34 = getImageView(0, getHeight() / 2);
        midPointer34.setOnTouchListener(new MidPointTouchListenerImpl(pointer3, pointer4));

        midPointer24 = getImageView(0, getHeight() / 2);
        midPointer24.setOnTouchListener(new MidPointTouchListenerImpl(pointer2, pointer4));

        addView(pointer1);
        addView(pointer2);
        addView(midPointer13);
        addView(midPointer12);
        addView(midPointer34);
        addView(midPointer24);
        addView(pointer3);
        addView(pointer4);
        initPaint();
    }

    @Override
    protected void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
        super.attachViewToParent(child, index, params);
    }

    private void initPaint() {
        paint = new Paint();
        paint.setColor(getResources().getColor(R.color.blue));
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
    }

    public Map<Integer, PointF> getPoints() {

        List<PointF> points = new ArrayList<PointF>();
        points.add(new PointF(pointer1.getX(), pointer1.getY()));
        points.add(new PointF(pointer2.getX(), pointer2.getY()));
        points.add(new PointF(pointer3.getX(), pointer3.getY()));
        points.add(new PointF(pointer4.getX(), pointer4.getY()));

        return getOrderedPoints(points);
    }

    public Map<Integer, PointF> getOrderedPoints(List<PointF> points) {

        PointF centerPoint = new PointF();
        int size = points.size();
        for (PointF pointF : points) {
            centerPoint.x += pointF.x / size;
            centerPoint.y += pointF.y / size;
        }

        // make crop square smaller a little bit
        int step = 0;

        Map<Integer, PointF> orderedPoints = new HashMap<>();

        for (PointF pointF : points) {
            int index = -1;
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0;
                pointF.x = pointF.x + step;
                pointF.y = pointF.y + step;
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1;
                pointF.x = pointF.x - step;
                pointF.y = pointF.y + step;
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2;
                pointF.x = pointF.x + step;
                pointF.y = pointF.y - step;
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3;
                pointF.x = pointF.x - step;
                pointF.y = pointF.y - step;
            }
            orderedPoints.put(index, pointF);
        }
        return orderedPoints;
    }

    public void setPoints(Map<Integer, PointF> pointFMap) {
        if (pointFMap.size() == 4) {
            setPointsCoordinates(pointFMap);
        }
    }

    public void setPointColor(int color)
    {
        if (paint!=null)
            paint.setColor(color);
    }

    private void setPointsCoordinates(Map<Integer, PointF> pointFMap) {
        pointer1.setX(pointFMap.get(0).x);
        pointer1.setY(pointFMap.get(0).y);

        pointer2.setX(pointFMap.get(1).x);
        pointer2.setY(pointFMap.get(1).y);

        pointer3.setX(pointFMap.get(2).x);
        pointer3.setY(pointFMap.get(2).y);

        pointer4.setX(pointFMap.get(3).x);
        pointer4.setY(pointFMap.get(3).y);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        canvas.drawLine(pointer1.getX() + (pointer1.getWidth() / 2), pointer1.getY() + (pointer1.getHeight() / 2), pointer3.getX() + (pointer3.getWidth() / 2), pointer3.getY() + (pointer3.getHeight() / 2), paint);
        canvas.drawLine(pointer1.getX() + (pointer1.getWidth() / 2), pointer1.getY() + (pointer1.getHeight() / 2), pointer2.getX() + (pointer2.getWidth() / 2), pointer2.getY() + (pointer2.getHeight() / 2), paint);
        canvas.drawLine(pointer2.getX() + (pointer2.getWidth() / 2), pointer2.getY() + (pointer2.getHeight() / 2), pointer4.getX() + (pointer4.getWidth() / 2), pointer4.getY() + (pointer4.getHeight() / 2), paint);
        canvas.drawLine(pointer3.getX() + (pointer3.getWidth() / 2), pointer3.getY() + (pointer3.getHeight() / 2), pointer4.getX() + (pointer4.getWidth() / 2), pointer4.getY() + (pointer4.getHeight() / 2), paint);
        midPointer13.setX(pointer3.getX() - ((pointer3.getX() - pointer1.getX()) / 2));
        midPointer13.setY(pointer3.getY() - ((pointer3.getY() - pointer1.getY()) / 2));
        midPointer24.setX(pointer4.getX() - ((pointer4.getX() - pointer2.getX()) / 2));
        midPointer24.setY(pointer4.getY() - ((pointer4.getY() - pointer2.getY()) / 2));
        midPointer34.setX(pointer4.getX() - ((pointer4.getX() - pointer3.getX()) / 2));
        midPointer34.setY(pointer4.getY() - ((pointer4.getY() - pointer3.getY()) / 2));
        midPointer12.setX(pointer2.getX() - ((pointer2.getX() - pointer1.getX()) / 2));
        midPointer12.setY(pointer2.getY() - ((pointer2.getY() - pointer1.getY()) / 2));
    }

    private void drawMag(float x,float y)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && magnifier!=null) {
            magnifier.show(x, y);
        }
    }

    private void dismissMag()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && magnifier!=null) {
            magnifier.dismiss();
        }
    }

    private ImageView getImageView(int x, int y) {
        ImageView imageView = new ImageView(context);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(R.drawable.circle);
        imageView.setX(x);
        imageView.setY(y);
        imageView.setOnTouchListener(new TouchListenerImpl());
        return imageView;
    }

    private class MidPointTouchListenerImpl implements OnTouchListener {

        PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF StartPT = new PointF(); // Record Start Position of 'img'

        private ImageView mainPointer1;
        private ImageView mainPointer2;

        public MidPointTouchListenerImpl(ImageView mainPointer1, ImageView mainPointer2) {
            this.mainPointer1 = mainPointer1;
            this.mainPointer2 = mainPointer2;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    viewPagerCrop.setUserInputEnabled(false);
                    PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);

                    if (Math.abs(mainPointer1.getX() - mainPointer2.getX()) > Math.abs(mainPointer1.getY() - mainPointer2.getY())) {
                        if (((mainPointer2.getY() + mv.y + v.getHeight() < polygonView.getHeight()) && (mainPointer2.getY() + mv.y > 0))) {
                            v.setX((int) (StartPT.y + mv.y));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer2.setY((int) (mainPointer2.getY() + mv.y));
                        }
                        if (((mainPointer1.getY() + mv.y + v.getHeight() < polygonView.getHeight()) && (mainPointer1.getY() + mv.y > 0))) {
                            v.setX((int) (StartPT.y + mv.y));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer1.setY((int) (mainPointer1.getY() + mv.y));
                        }
                    } else {
                        if ((mainPointer2.getX() + mv.x + v.getWidth() < polygonView.getWidth()) && (mainPointer2.getX() + mv.x > 0)) {
                            v.setX((int) (StartPT.x + mv.x));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer2.setX((int) (mainPointer2.getX() + mv.x));
                        }
                        if ((mainPointer1.getX() + mv.x + v.getWidth() < polygonView.getWidth()) && (mainPointer1.getX() + mv.x > 0)) {
                            v.setX((int) (StartPT.x + mv.x));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer1.setX((int) (mainPointer1.getX() + mv.x));
                        }
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    DownPT.x = event.getX();
                    DownPT.y = event.getY();
                    StartPT = new PointF(v.getX(), v.getY());
                    viewPagerCrop.setUserInputEnabled(true);
                    updatePolygon();
                    break;
                case MotionEvent.ACTION_UP:
                    int color = 0;
                    if (isValidShape(getPoints())) {
                        color = getResources().getColor(R.color.blue);
                    } else {
                        color = getResources().getColor(R.color.orange);
                    }
                    paint.setColor(color);
                    dismissMag();
                    viewPagerCrop.setUserInputEnabled(true);
                    updatePolygon();
                    break;
                default:
                    break;
            }
            polygonView.invalidate();
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public boolean isValidShape(Map<Integer, PointF> pointFMap) {
        return pointFMap.size() == 4;
    }

    private class TouchListenerImpl implements OnTouchListener {

        PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF StartPT = new PointF(); // Record Start Position of 'img'

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    viewPagerCrop.setUserInputEnabled(false);
                    PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);
                    if (((StartPT.x + mv.x + v.getWidth()) < polygonView.getWidth() && (StartPT.y + mv.y + v.getHeight() < polygonView.getHeight())) && ((StartPT.x + mv.x) > 0 && StartPT.y + mv.y > 0)) {
                        v.setX((int) (StartPT.x + mv.x));
                        v.setY((int) (StartPT.y + mv.y));
                        StartPT = new PointF(v.getX(), v.getY());
                        drawMag(StartPT.x+50,StartPT.y+50);
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    DownPT.x = event.getX();
                    DownPT.y = event.getY();
                    StartPT = new PointF(v.getX(), v.getY());
                    viewPagerCrop.setUserInputEnabled(true);
                    updatePolygon();
                    break;
                case MotionEvent.ACTION_UP:
                    int color = 0;
                    if (isValidShape(getPoints())) {
                        color = getResources().getColor(R.color.blue);
                    } else {
                        color = getResources().getColor(R.color.orange);
                    }
                    paint.setColor(color);
                    dismissMag();
                    viewPagerCrop.setUserInputEnabled(true);
                    updatePolygon();
                    break;
                default:
                    break;
            }
            polygonView.invalidate();
            return true;
        }

    }

}

package com.journaldev.androidcameraxopencv.libraries;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class SimpleDrawingView extends View {
    // setup initial color
    private final int paintColor = Color.RED;
    // defines paint and canvas
    private Paint drawPaint;

    // Store circles to draw each time the user touches down
    private List<Point> circlePoints;

    public SimpleDrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);
        circlePoints = new ArrayList<Point>();
        setupPaint();
    }

    private void setupPaint() {
        // Setup paint with color and stroke styles
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(5);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (Point p : circlePoints) {
            canvas.drawCircle((float) p.x, (float)p.y, 30, drawPaint);
        }
    }

    public void setPoints(List<Point> points) {
        circlePoints = points;
    }
}
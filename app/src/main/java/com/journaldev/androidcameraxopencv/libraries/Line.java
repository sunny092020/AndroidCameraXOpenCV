package com.journaldev.androidcameraxopencv.libraries;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import com.journaldev.androidcameraxopencv.MainActivity;

import org.opencv.core.Point;

public class Line {
    public Point _p1;
    public Point _p2;
    public Point _center;

    public Line(Point p1, Point p2) {
        _p1 = p1;
        _p2 = p2;
        _center = new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
    }

    public void draw(Canvas canvas, Paint drawPaint) {
        canvas.drawLine((float) _p1.x, (float)_p1.y,(float) _p2.x, (float) _p2.y, drawPaint );
    }

    public double distance() {
        return  Math. sqrt((_p2.x-_p1.x)*(_p2.x-_p1.x) + (_p2.y-_p1.y)*(_p2.y-_p1.y));
    }

    public LinePolar toLinePolar() {
        // y = ax+b
        double x1 = this._p1.x, y1 = this._p1.y,
                x2 = this._p2.x, y2 = this._p2.y;
        double a = (y2-y1)/(x2-x1);

        if (x2==x1) return new LinePolar(x1, 0);
        if (y2==y1) return new LinePolar(y1, 90);

//        Log.d("a", Double.toString(a));

        double b = y1 - a*x1;
//        Log.d("b", Double.toString(b));

        double x0 = -b/a;
        double y0 = b;
//        Log.d("x0", Double.toString(x0));
//        Log.d("y0", Double.toString(y0));

        double r     = x0*y0/Math.sqrt(x0*x0 + y0*y0);
        double theta = Math.atan2(x0, y0)*180/Math.PI;

        if(theta>90) theta=theta-180;
        if(theta<-90) theta=theta+180;

        return new LinePolar(r,theta);

    }
}

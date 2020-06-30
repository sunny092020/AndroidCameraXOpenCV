package com.ami.icamdocscanner.libraries;

import org.opencv.core.Point;

import java.util.List;
import java.util.Objects;

public class LinePolar {
    public double _theta;
    public double _r;

    public LinePolar(double r, double theta) {
        _theta = theta;
        _r = r;
    }

    public int hashCode() {
        return Objects.hash(getSigFields());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LinePolar other = (LinePolar) o;
        return ((this._theta == other._theta) &&
                (this._r == other._r));
    }

    public double getBeta() {
        if(_theta<=0) return _theta+90;
        else return _theta-90;
    }

    public double deltaTheta(LinePolar otherLp) {
        return Math.abs(otherLp._theta - this._theta);
    }

    public double deltaR(LinePolar otherLp) {
        return Math.abs(otherLp._r - this._r);
    }

    public static LinePolar average(List<Line> lines) {
        double sumTheta = 0, sumR = 0;
        for (Line l : lines) {
            LinePolar lp = l.toLinePolar();
            sumTheta = sumTheta+lp._theta;
            sumR = sumR+lp._r;
        }
        return new LinePolar(sumR/lines.size(), sumTheta/lines.size());
    }

    public static Line averageLine(List<Line> lines) {
        double sumX1 = 0, sumY1 = 0, sumX2 = 0, sumY2 = 0;
        for (Line l : lines) {
            sumX1 = sumX1+l._p1.x;
            sumY1 = sumY1+l._p1.y;

            sumX2 = sumX2+l._p2.x;
            sumY2 = sumY2+l._p2.y;
        }
        int size = lines.size();
        return new Line(new Point(sumX1/size, sumY1/size),
                        new Point(sumX2/size, sumY2/size));
    }

    private Object[] getSigFields(){
        return new Object[]{_theta, _r};
    }
}

package com.journaldev.androidcameraxopencv.libraries;

import com.journaldev.androidcameraxopencv.MainActivity;

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

    private Object[] getSigFields(){
        return new Object[]{_theta, _r};
    }
}

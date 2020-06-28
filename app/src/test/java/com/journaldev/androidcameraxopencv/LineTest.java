package com.ami.icamdocscanner;

import com.ami.icamdocscanner.libraries.Line;
import com.ami.icamdocscanner.libraries.LinePolar;

import org.junit.Test;
import org.opencv.core.Point;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class LineTest {
    @Test
    public void toLinePolar_isCorrect() {
        Line l1 = new Line(new Point(100, 0), new Point(200, 0));
        LinePolar lp1 = l1.toLinePolar();
        assertEquals(90, lp1._theta, 0);
        assertEquals(0, lp1._r, 0);

        Line l2 = new Line(new Point(200, 0), new Point(100, 0));
        LinePolar lp2 = l2.toLinePolar();
        assertEquals(90, lp2._theta, 0);
        assertEquals(0, lp2._r, 0);

        Line l3 = new Line(new Point(100, 200), new Point(200, 200));
        LinePolar lp3 = l3.toLinePolar();
        assertEquals(90, lp3._theta, 0);
        assertEquals(200, lp3._r, 0);

        Line l4 = new Line(new Point(100, 210), new Point(220, 230));
        LinePolar lp4 = l4.toLinePolar();
        assertEquals(-80, lp4._theta, 1);
        assertEquals(-190, lp4._r, 1);

        Line l5 = new Line(new Point(152, 774), new Point(307, 774));
        LinePolar lp5 = l5.toLinePolar();
        assertEquals(90, lp5._theta, 1);
        assertEquals(774, lp5._r, 1);
        assertEquals(0, lp5.getBeta(), 1);


    }

}
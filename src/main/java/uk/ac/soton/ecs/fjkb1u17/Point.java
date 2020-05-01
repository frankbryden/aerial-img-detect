package uk.ac.soton.ecs.fjkb1u17;

import org.openimaj.math.geometry.point.Point2dImpl;

public class Point {
    public int x, y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point2dImpl toPoint2dImpl(){
        return new Point2dImpl((float) this.x, (float) this.y);
    }
}

package uk.ac.soton.ecs.fjkb1u17;

import org.openimaj.math.geometry.point.Point2dImpl;

public class Vertex {
    Point2dImpl pos;
    boolean alive = true;

    public Vertex(Point2dImpl pos) {
        this.pos = pos;
    }

    public Vertex (int x, int y){
        this.pos = new Point2dImpl(x, y);
    }

    public Point2dImpl getPos(){
        return this.pos;
    }

    public int dist(Vertex otherVertex){
        return (int) Math.sqrt(Math.pow(this.pos.x - otherVertex.pos.x, 2) + Math.pow(this.pos.y - otherVertex.pos.y, 2));
    }
}

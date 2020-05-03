package uk.ac.soton.ecs.fjkb1u17;

import org.openimaj.math.geometry.line.Line2d;

public class Edge {
    Vertex v1, v2;
    private Line2d line;

    public Edge(Vertex v1, Vertex v2) {
        this.v1 = v1;
        this.v2 = v2;
        this.line = new Line2d(v1.pos, v2.pos);
    }

    public Line2d getLine(){
        return this.line;
    }

    public double getAngle() {
        return Math.atan2(this.v1.pos.y - this.v2.pos.y, this.v1.pos.x - this.v2.pos.x);
    }
}

package uk.ac.soton.ecs.fjkb1u17;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.renderer.MultiBandRenderer;
import org.openimaj.math.geometry.line.Line2d;
import org.openimaj.math.geometry.point.Point2dImpl;

import java.util.ArrayList;
import java.util.List;

public class SpokeWheel {
    private Point p;
    private int spokeCount;
    private double angleStep;
    private int spokeRadius;
    private Line2d[] spokes;
    private FImage target;
    private float centerIntensity;
    private float standardDeviation;
    private float k; //Controls sensitivity

    public SpokeWheel(Point2dImpl p, int spokeCount, int spokeRadius, FImage targetImg, float sensitivity) {
        this.p = new Point((int) p.x, (int) p.y);
        this.spokeCount = spokeCount;
        this.angleStep = (2*Math.PI)/this.spokeCount;
        this.spokeRadius = spokeRadius;
        this.k = sensitivity;
        this.spokes = new Line2d[spokeCount];
        this.target = targetImg;
        this.centerIntensity = this.target.pixels[this.p.y][this.p.x];
        this.initSpokes();
        this.standardDeviation = getStandardDeviation();
    }

    private void initSpokes(){
        for (int i = 0; i < this.spokeCount; i++){
            double angle = i*this.angleStep;
            int x = (int) (this.p.x + Math.cos(angle)*this.spokeRadius);
            int y = (int) (this.p.y + Math.sin(angle)*this.spokeRadius);
            this.spokes[i] = new Line2d(p.x, p.y, x, y);
        }
    }

    private List<Float> getPixelsInSpoke(){
        List<Float> pixels = new ArrayList<>();
        //Credits : https://stackoverflow.com/questions/15856411/finding-all-the-points-within-a-circle-in-2d-space
        //get all pixels in top left quadrant of circle
        for (int x = this.p.x - this.spokeRadius; x <= this.p.x; x++) {
            for (int y = this.p.y - this.spokeRadius; y <= this.p.y; y++) {
                if ((x - this.p.x)*(x - this.p.x) + (y - this.p.y)*(y - this.p.y) <= this.spokeRadius*this.spokeRadius) {
                    //obtain their x and y symmetry
                    int xSym = this.p.x - (x - this.p.x);
                    int ySym = this.p.y - (y - this.p.y);

                    boolean yInBounds = y >= 0 && y < this.target.pixels.length;
                    boolean xInBounds = x >= 0 && x < this.target.pixels[0].length;
                    boolean xSymInBounds = xSym < this.target.pixels[0].length && xSym >= 0;
                    boolean ySymInbounds = ySym < this.target.pixels.length && ySym >= 0;
                    //determine the 4 resulting pixels - one in each quadrant
                    if (yInBounds && xInBounds)
                        pixels.add(this.target.pixels[y][x]);
                    if (ySymInbounds && xInBounds)
                        pixels.add(this.target.pixels[ySym][x]);
                    if (yInBounds && xSymInBounds)
                        pixels.add(this.target.pixels[y][xSym]);
                    if (ySymInbounds && xSymInBounds)
                        pixels.add(this.target.pixels[ySym][xSym]);
                }
            }
        }
        return pixels;
    }

    private float getStandardDeviation(){
        // Credits : https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Two-pass_algorithm
        int n = 0;
        float sum1 = 0;
        float sum2 = 0;

        List<Float> pixels = this.getPixelsInSpoke();

        for (Float p : pixels){
            sum1 += p;
        }

        n = pixels.size();

        float mean = sum1 / n;

        for (Float p : pixels) {
            sum2 += (p - mean) * (p - mean);
        }

        return (float) Math.sqrt(sum2 / (n-1));
    }

    public List<Point2dImpl> determineIntersections(){
        List<Point2dImpl> cutOffPoints = new ArrayList<>();
        for (int spokeID = 0; spokeID < this.spokeCount; spokeID++){
            cutOffPoints.add(this.intersectSpoke(spokeID));
        }
        return cutOffPoints;
    }

    public Point2dImpl intersectSpoke(int spokeID){ //spokeID is an int in the range [0 : spokeCount - 1]
        float spokeAngle = (float) (spokeID * this.angleStep);
        Point2dImpl cutOffPoint = null;
        for (int i = 0; i < this.spokeRadius; i++){
            int x = this.p.x + (int) (Math.cos(spokeAngle) * i);
            int y = this.p.y + (int) (Math.sin(spokeAngle) * i);
            if (!(x >= 0 && x < this.target.pixels[0].length && y >= 0 && y < this.target.pixels.length)){
                //if point is out of bounds, skip to next iteration
                continue;
            }
            float intensity = this.target.pixels[y][x];
            // We are looking for the cut-off point, defined in [10], as "|I(Ci) − I(p)| ≥ σ (W(p, n, m)), 0 ≤ i < 4n"
            // where I(x) is the intensity of pixel x
            // and σ (W(p, n, m)) is the standard deviation of intensities contained within spoke wheel with radius m and centered
            // around p
            if (Math.abs(intensity - centerIntensity) > k*this.standardDeviation) {
                cutOffPoint = new Point2dImpl(x, y);
                break;
            }
        }
        if (cutOffPoint == null){
            // if no cutoff point was found, it usually means spoke extends along road, and road
            // edge was not met as a result. Meaning we set the cutoff point as the endPoint of the spoke.
            cutOffPoint = new Point2dImpl(this.p.x + (int) (Math.cos(spokeAngle) * this.spokeRadius),this.p.y + (int) (Math.sin(spokeAngle) * this.spokeRadius));
        }
        return cutOffPoint;
    }


    public void render(MBFImage target){
        MultiBandRenderer<Float, MBFImage, FImage> renderer = new MyRenderer(target);
        Float[] col = new Float[3];
        col[0] = 0.9f;
        col[1] = 0.3f;
        col[2] = 0.1f;
        for (Line2d spoke : this.spokes){
            target.drawLine((int) spoke.begin.getX(), (int) spoke.begin.getY(), (int) spoke.end.getX(), (int) spoke.end.getY(), 1, col);
        }
    }

    public Point2dImpl getP() {
        return new Point2dImpl(p.x, p.y);
    }
}

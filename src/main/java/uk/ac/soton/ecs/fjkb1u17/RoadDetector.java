package uk.ac.soton.ecs.fjkb1u17;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.math.geometry.shape.RotatedRectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RoadDetector {
    public static final int SPOKE_COUNT = 64;
    public static final int SPOKE_RADIUS = 32;
    private FImage target;
    private List<List<Point2dImpl>> cutOffPointsList = new ArrayList<>();
    private List<List<Point2dImpl>> peakPointsList = new ArrayList<>();
    private List<Polygon> cutOffShapes = new ArrayList<>();
    private List<Vertex> roadSeeds;
    private List<SpokeWheel> spokeWheels = new ArrayList<>();
    private boolean spawnedRandomly = false;

    public RoadDetector(FImage target, List<Vertex> roadSeeds){
        this.target = target;
        this.roadSeeds = roadSeeds;
    }

    public RoadDetector(FImage target, Vertex roadSeed){
        this.roadSeeds = new ArrayList<>();
        this.roadSeeds.add(roadSeed);
        this.target = target;
    }

    public RoadDetector(FImage target){
        this.target = target;
        this.spawnRoadSeeds(250);
    }

    private void spawnRoadSeeds(int count){
        this.spawnedRandomly = true;
        Random r = new Random(System.currentTimeMillis());
        this.roadSeeds = new ArrayList<>();
        for (int i = 0; i < count; i++){
            this.roadSeeds.add(new Vertex(new Point2dImpl(r.nextInt(target.width - 2 * SPOKE_RADIUS) + SPOKE_RADIUS,
                    r.nextInt(target.height - 2 * SPOKE_RADIUS) + SPOKE_RADIUS)));
        }
    }


    public void process(){
        for (Vertex v : this.roadSeeds){
            List<Point2dImpl> cutOffPoints = applySpoke(v);
            if (cutOffPoints == null){
                continue;
            }
            Polygon shape = new Polygon(cutOffPoints);
            if (spawnedRandomly){ //if seeds were random, then only add rectangular road sections to the list of detected points and shapes
                if (isRectangular(shape) || true){
                    cutOffPointsList.add(cutOffPoints);
                    cutOffShapes.add(shape);
                }
            } else {
                cutOffPointsList.add(cutOffPoints);
                ToeFinding toeFinding = new ToeFinding(new Point2dImpl(v.pos.x, v.pos.y), cutOffPoints);
                toeFinding.run();
                //System.exit(1);
                peakPointsList.add(toeFinding.getPeaks());
                cutOffShapes.add(shape);
            }
        }
    }

    private boolean isRectangular(Polygon p){
        //From [10], we can determine if a polygon is approximately a rectangle
        // "AREA(F)/AREA (MOBB(F)) > 85% (2) where MOBB is the miniaml oriented bounding box and F is the footprint of the road seed (p in our case)
        // and
        // (length of longer edge of MOBB(F)) / (length of shorter edge of MOBB(F)) > 2"
        double polygonArea = p.calculateArea();
        RotatedRectangle mobb = p.minimumBoundingRectangle();
        double mobbArea = mobb.calculateArea();
        if (polygonArea/mobbArea <= 0.70){
            //System.out.println("Not rectangular as less than 85% of the inner area fills the rectangle -> " + (polygonArea/mobbArea));
            return false;
        }

        // first condition is true, now check if length of longer edge of mobb is at least twice as long as the shorter edge
        double width = mobb.width;
        double height = mobb.height;
        double ratio = Math.max(width, height)/Math.min(width, height);
        return ratio > 2;
    }

    public void render(MBFImage image){
        spokeWheels.forEach(s -> {
            image.drawPoint(s.getP(), RGBColour.MAGENTA, 5);
            s.render(image);
        });
        System.out.println("Rendering road detection with " + this.roadSeeds.get(0).pos + " seeds");
        image.drawPoint(this.roadSeeds.get(0).pos, RGBColour.MAGENTA, 5);
        for (int i = 0; i < cutOffPointsList.size(); i++){
            image.drawPoints(cutOffPointsList.get(i), RGBColour.GREEN, 2);
            Float[] shapeCol;
            if (this.isRectangular(cutOffShapes.get(i))){
                shapeCol = RGBColour.GREEN;
            } else {
                shapeCol = RGBColour.RED;
            }
            //image.drawPolygon(cutOffShapes.get(i), shapeCol);
            image.drawPoints(peakPointsList.get(i), RGBColour.YELLOW, 2);
            //image.drawShape(new Circle(roadSeeds.get(i).x, roadSeeds.get(i).y, SPOKE_RADIUS), 1, RGBColour.CYAN);
            //image.drawShape(cutOffShapes.get(i).minimumBoundingRectangle(), shapeCol);
        }

    }

    public List<List<Point2dImpl>> getToes(){
        return this.peakPointsList;
    }

    public List<List<Point2dImpl>> getCutOffPoints(){
        return this.cutOffPointsList;
    }

    private boolean withinTarget(Point2dImpl p){
        return (p.x >= 0 && p.x < this.target.pixels[0].length && p.y >= 0 && p.y < this.target.pixels.length);
    }

    private List<Point2dImpl> applySpoke(Vertex v){
        if (!withinTarget(v.pos)){
            return null;
        }
        SpokeWheel spokeWheel = new SpokeWheel(v.pos, SPOKE_COUNT, SPOKE_RADIUS, this.target, 0.5f);
        spokeWheels.add(spokeWheel);
        return spokeWheel.determineIntersections();
    }
}

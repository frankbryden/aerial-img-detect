package uk.ac.soton.ecs.fjkb1u17.buildingDetection;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.renderer.RenderHints;
import org.openimaj.image.typography.FontStyle;
import org.openimaj.image.typography.general.GeneralFont;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openjena.atlas.iterator.Iter;
import uk.ac.soton.ecs.fjkb1u17.SpokeWheel;

import org.openimaj.image.FImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.math.geometry.shape.RotatedRectangle;
import uk.ac.soton.ecs.fjkb1u17.ToeFinding;
import uk.ac.soton.ecs.fjkb1u17.Vertex;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class SpokeDetection {
    public static final int SPOKE_COUNT = 64;
    public static final int SPOKE_RADIUS = 20;
    private static final int MIN_BUILDING_AREA = 300;
    private static final float MIN_BUILDING_SATURATION = 0.6f;
    private final FImage target;
    private List<List<Point2dImpl>> peakPointsList = new ArrayList<>();
    private List<Polygon> cutOffShapes = new ArrayList<>();
    private List<Vertex> buildingSeeds;
    private List<SpokeWheel> spokeWheels = new ArrayList<>();
    private List<Polygon> roadPolys = new ArrayList<>(); //Keep track of detected roads
                                                        // Will be needed later to remove FP buildings which lie on roads
    private FImage binaryRoads; //Used as a map to determine if a point is on a road or not.
    private boolean spawnedRandomly = false;

    public SpokeDetection(FImage target, List<Vertex> buildingSeeds, FImage binaryRoads){
        this.target = target;
        this.buildingSeeds = buildingSeeds;
        this.binaryRoads = binaryRoads;
    }

    private void spawnRoadSeeds(int count){
        this.spawnedRandomly = true;
        Random r = new Random(System.currentTimeMillis());
        this.buildingSeeds = new ArrayList<>();
        for (int i = 0; i < count; i++){
            this.buildingSeeds.add(new Vertex(new Point2dImpl(r.nextInt(target.width - 2 * SPOKE_RADIUS) + SPOKE_RADIUS,
                    r.nextInt(target.height - 2 * SPOKE_RADIUS) + SPOKE_RADIUS)));
        }
    }


    public void process(){
        for (Vertex v : this.buildingSeeds){
            List<Point2dImpl> cutOffPoints = applySpoke(v);
            if (cutOffPoints == null){
                continue;
            }
            Polygon shape = new Polygon(cutOffPoints);
            if (spawnedRandomly){ //if seeds were random, then only add rectangular road sections to the list of detected points and shapes
                if (isRectangular(shape) || true){
                    cutOffShapes.add(shape);
                }
            } else {
                cutOffShapes.add(shape);
            }
        }
    }

    //TODO make private and add to process
    public void filterOnRoadSeeds() {
        int areaRemovalCount = 0;
        List<Polygon> removedBuildings = new ArrayList<>();
        Iterator<Polygon> iter = this.cutOffShapes.iterator();
        while (iter.hasNext()){
            Polygon poly = iter.next();
            if (poly.minimumBoundingRectangle().calculateArea() < MIN_BUILDING_AREA){
                removedBuildings.add(poly);
                iter.remove();
                areaRemovalCount++;
                continue;
            }
            for (Point2d point: poly.points){
                int x = (int) point.getX();
                int y = (int) point.getY();
                if (x >= 0 && x < binaryRoads.width && y >= 0 && y < binaryRoads.height){
                    if (binaryRoads.getPixel((int) point.getX(), (int) point.getY()) == 1f){
                        iter.remove();
                        break;
                    }
                }
            }
        }
        System.out.println("Removed " + areaRemovalCount + " buildings by looking at MOBB area.");
    }

    public List<Polygon> filterSaturation(FImage saturation){
        //Filter out buildings above a certain mean saturation
        List<Polygon> removedFromSat = new ArrayList<>();
        Iterator<Polygon> iter = cutOffShapes.iterator();
        while (iter.hasNext()){
            Polygon poly = iter.next();
            if (meanSaturation(saturation, getContainingPoints(poly)) > MIN_BUILDING_SATURATION){
                iter.remove();
                removedFromSat.add(poly);
            }

        }
        return removedFromSat;
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
        /*spokeWheels.forEach(s -> {
            image.drawPoint(s.getP(), RGBColour.MAGENTA, 5);
            s.render(image);
        });*/
        final FontStyle<Float[]> gfs = new GeneralFont("Aerial", Font.PLAIN).createStyle(image.createRenderer(RenderHints.ANTI_ALIASED));

        gfs.setFontSize(16);
        gfs.setHorizontalAlignment(FontStyle.HorizontalAlignment.HORIZONTAL_CENTER);
        for (int i = 0; i < cutOffShapes.size(); i++){
            Float[] shapeCol;
            Polygon poly = cutOffShapes.get(i);
            if (this.isRectangular(poly)){
                shapeCol = RGBColour.GREEN;
            } else {
                shapeCol = RGBColour.RED;
            }
            image.drawPolygon(poly, shapeCol);
            /*if (i % 5 == 0){
                Rectangle polyRect = poly.calculateRegularBoundingBox();
                image.drawText(String.format("%2f", getMOBBRatio(poly)*100), (int) (polyRect.x + polyRect.width/2)
                        , (int) (polyRect.y + polyRect.height/2), gfs);
            }*/

            //image.drawPoints(peakPointsList.get(i), RGBColour.YELLOW, 2);
            //image.drawShape(new Circle(roadSeeds.get(i).x, roadSeeds.get(i).y, SPOKE_RADIUS), 1, RGBColour.CYAN);
            //image.drawShape(cutOffShapes.get(i).minimumBoundingRectangle(), shapeCol);
        }

    }

    private double getMOBBRatio(Polygon p){
        double polygonArea = p.calculateArea();
        RotatedRectangle mobb = p.minimumBoundingRectangle();
        double mobbArea = mobb.calculateArea();
        return polygonArea/mobbArea;
    }

    public List<Point2dImpl> getContainingPoints(Polygon poly){
        List<Point2dImpl> points = new ArrayList<>();
        Rectangle rect = poly.calculateRegularBoundingBox();
        for (int x = (int) poly.minX(); x < poly.maxX(); x++){
            for (int y = (int) poly.minY(); y < poly.maxY(); y++){
                Point2dImpl point = new Point2dImpl(x, y);
                if (poly.isInside(point)){
                    points.add(point);
                }
            }
        }
        return points;
    }

    private float meanSaturation(FImage saturation, List<Point2dImpl> points){
        double totalSat = 0;
        for (Point2dImpl point: points){
            try {
                totalSat += saturation.getPixel((int) point.x, (int) point.y);
            } catch (ArrayIndexOutOfBoundsException e){

            }

        }
        return (float) totalSat/points.size();
    }


    public List<List<Point2dImpl>> getToes(){
        return this.peakPointsList;
    }

    private boolean withinTarget(Point2dImpl p){
        return (p.x >= 0 && p.x < this.target.pixels[0].length && p.y >= 0 && p.y < this.target.pixels.length);
    }

    private List<Point2dImpl> applySpoke(Vertex v){
        if (!withinTarget(v.getPos())){
            return null;
        }
        SpokeWheel spokeWheel = new SpokeWheel(v.getPos(), SPOKE_COUNT, SPOKE_RADIUS, this.target, 1.5f);
        spokeWheels.add(spokeWheel);
        return spokeWheel.determineIntersections();
    }
}

package uk.ac.soton.ecs.fjkb1u17;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processor.PixelProcessor;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.math.geometry.shape.RotatedRectangle;

import java.awt.color.ColorSpace;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class SeedGeneration {
    private final int GEN_POINT_COUNT = 10000;
    private final float SATURATION_THRESHOLD = 0.2f;//0.15f;
    private MBFImage image;
    private FImage flattened;
    private ArrayList<Point> seeds;

    public SeedGeneration(MBFImage image) {
        this.image = image;
        this.flattened = image.flatten();
        this.seeds = new ArrayList<>();
    }

    public void run(){
        this.genRandSeedPoints();

        int startingPointCount = this.seeds.size();

        System.out.println("Starting with " + startingPointCount + " seeds.");

        this.thresholdTest();
        System.out.println("After threshold test, " + (startingPointCount - this.seeds.size()) + " seeds removed. "
                + this.seeds.size() + " seeds remaining.");
        startingPointCount = this.seeds.size();

        this.rectangularityTest();
        System.out.println("After rectangularity test, " + (startingPointCount - this.seeds.size()) + " seeds removed. "
                + this.seeds.size() + " seeds remaining.");
        startingPointCount = this.seeds.size();

        this.networkExpansionTest();
        System.out.println("After network expansion test, " + (startingPointCount - this.seeds.size()) + " seeds removed. "
                + this.seeds.size() + " seeds remaining.");
        startingPointCount = this.seeds.size();

        MBFImage clone = image.clone();
        this.seeds.forEach(seed -> clone.drawPoint(seed.toPoint2dImpl(), RGBColour.YELLOW, 8));
        DisplayUtilities.display(clone, "Before network growth (" + seeds.size() + " seeds)");

        this.grow();

    }

    public void genRandSeedPoints(){
        Random rand = new Random();
        for (int i = 0; i < GEN_POINT_COUNT; i++){
            int x = rand.nextInt(image.getWidth());
            int y = rand.nextInt(image.getHeight());
            seeds.add(new Point(x, y));
        }
    }

    public void thresholdTest(){
        Iterator<Point> iter = seeds.iterator();
        FImage sat = Transforms.RGB_TO_HSV(image).getBand(1);
        while (iter.hasNext()){
            Point seed = iter.next();
            if (sat.getPixel(seed.x, seed.y) > SATURATION_THRESHOLD){
                iter.remove();
            }
        }
    }

    public void rectangularityTest(){
        Iterator<Point> iter = seeds.iterator();
        FImage sat = Transforms.RGB_TO_HSV(image).getBand(1);
        while (iter.hasNext()){
            Point seed = iter.next();
            SpokeWheel spokeWheel = new SpokeWheel(seed.toPoint2dImpl(), RoadDetector.SPOKE_COUNT, RoadDetector.SPOKE_RADIUS, flattened, 0.5f);
            List<Point2dImpl> cutOffPoints = spokeWheel.determineIntersections();
            if (cutOffPoints == null){
                continue;
            }
            Polygon shape = new Polygon(cutOffPoints);
            if (!isRectangular(shape)){
                iter.remove();
            }
        }
    }

    public void networkExpansionTest(){
        // From [11] a network expansion test must be performed which checks the following two features:
        // - Potential test: number of generations a footprint can generate
        // - Mean Stretch Distance MSD: on-road distance covered by footprint expansion
        Iterator<Point> iter = seeds.iterator();
        ArrayList<Point> removedSeeds = new ArrayList<>();
        ArrayList<Point> removedMSD = new ArrayList<>();
        int c = 0;

        while (iter.hasNext()){
            Point seed = iter.next();
            ArrayList<Vertex> seeds = new ArrayList<>();
            Vertex centre = new Vertex(seed.toPoint2dImpl());
            seeds.add(centre);
            RoadNetworkExtraction rne = new RoadNetworkExtraction(image, seeds);
            rne.run(true);
            if (rne.getIterCount() < 4){
                removedSeeds.add(seed);
                iter.remove();
            } else {
                if (rne.getMSD(centre) < 60){
                    removedMSD.add(seed);
                    iter.remove();
                    c++;
                }
                //System.out.println(rne.getMSD(centre));
            }
        }
        System.out.println("Removed " + c + " seeds with MSD test");

        MBFImage clone = image.clone();
        removedSeeds.forEach(seed -> clone.drawPoint(seed.toPoint2dImpl(), RGBColour.CYAN, 8));
        removedMSD.forEach(seed -> clone.drawPoint(seed.toPoint2dImpl(), RGBColour.YELLOW, 8));
        DisplayUtilities.display(clone, "Removed by network expansion (" + removedSeeds.size() + " seeds)");

    }

    public void grow(){
        System.out.println("Growing network...");
        RoadNetworkExtraction rne = new RoadNetworkExtraction(image, this.seeds.stream().map(Point::toPoint2dImpl).map(Vertex::new).collect(Collectors.toList()));
        rne.run(false);
        rne.filterNearNeighbours(16);
        MBFImage cl = image.clone();
        rne.render(cl);
        DisplayUtilities.display(cl, "After filtering");
        FImage output = rne.getRoadsBinaryImage();
        try {
            ImageUtilities.write(output, new File("binaryRoadsUS.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        DisplayUtilities.display(output, "Output");
        MBFImage withoutRoads = image.clone();
        withoutRoads.multiplyInplace(output.inverse());
        DisplayUtilities.display(withoutRoads, "Without Roads");
        System.out.println("Done.");
    }

    private boolean isRectangular(Polygon p){
        //From [10], we can determine if a polygon is approximately a rectangle
        // "AREA(F)/AREA (MOBB(F)) > 85% (2) where MOBB is the miniaml oriented bounding box and F is the footprint of the road seed (p in our case)
        // and
        // (length of longer edge of MOBB(F)) / (length of shorter edge of MOBB(F)) > 2"
        double polygonArea = p.calculateArea();
        RotatedRectangle mobb = p.minimumBoundingRectangle();
        double mobbArea = mobb.calculateArea();
        if (polygonArea/mobbArea <= 0.60){
            return false;
        }

        // first condition is true, now check if length of longer edge of mobb is at least twice as long as the shorter edge
        double width = mobb.width;
        double height = mobb.height;
        double ratio = Math.max(width, height)/Math.min(width, height);
        return ratio > 2;
    }

    public void display(){
        MBFImage cpy = image.clone();
        for (Point p: seeds){
            cpy.drawPoint(p.toPoint2dImpl(), RGBColour.RED, 2);
        }
        DisplayUtilities.display(cpy);
    }

    public List<Point2dImpl> toSeedsPoint2dImpl(){
        return this.seeds.stream().map(Point::toPoint2dImpl).collect(Collectors.toList());
    }


}

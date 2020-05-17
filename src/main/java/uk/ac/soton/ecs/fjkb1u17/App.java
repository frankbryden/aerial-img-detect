package uk.ac.soton.ecs.fjkb1u17;

import org.jfree.chart.plot.dial.DialPlot;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.processor.PixelProcessor;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.math.statistics.distribution.MultivariateKernelDensityEstimate;
import org.openimaj.ml.clustering.FloatCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.FloatKMeans;
import org.openimaj.ml.clustering.meanshift.ExactMeanShift;
import org.w3c.dom.css.RGBColor;
import uk.ac.soton.ecs.fjkb1u17.buildingDetection.Invariants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * OpenIMAJ Hello world!
 *
 */
public class App {
    public static void main( String[] args ) {
    	//Create an image
        MBFImage image = null;
        System.out.println(Arrays.toString(args));
        System.out.println("Starting with 5000 seeds.");
        System.out.println("After threshold test, 2167 seeds were removed. 2833 seeds remaining.");
        System.out.println("After rectangularity test, 2610 seeds were removed. 223 seeds remaining.");
        System.out.println("After network expansion test, 19 seeds removed. 204 seeds remaining.");
        try {
            image = ImageUtilities.readMBF(new File("easiestRoad.png"));
            //image = ImageUtilities.readMBF(new File("img3.png"));
            image = ImageUtilities.readMBF(new File("LA.png"));
            FImage filtered = new FImage(image.getWidth(), image.getHeight());
            FImage redBand = image.getBand(0);
            FImage greenBand = image.getBand(1);
            FImage blueBand = image.getBand(2);

            CannyEdgeDetector canny = new CannyEdgeDetector(4f);
            FImage cannyImg = image.flatten();
            /*canny.processImage(cannyImg);
            DisplayUtilities.display(cannyImg, "Canny");*/

            //MultivariateKernelDensityEstimate kde = new MultivariateKernelDensityEstimate();
            //ExactMeanShift meanShift = new ExactMeanShift();

            SeedGeneration seedGeneration = new SeedGeneration(image);
            seedGeneration.run();

            /*MBFImage hsv = Transforms.RGB_TO_HSV(image);
            for (int i = 0; i < 3; i++){
                DisplayUtilities.display(hsv.getBand(i), "Band " + i);
            }
            FImage greenInvariantB = invariants.greenInvariantB();
            DisplayUtilities.display(greenInvariant, "Green invariant");
            DisplayUtilities.display(greenInvariantB, "Green invariant with BLue");
            DisplayUtilities.display(image.getBand(1), "Green band");*/


            List<Point2dImpl> targetRoadSeeds = new ArrayList<>();
            /*targetRoadSeeds.add(new Point2dImpl(210, 520));
            targetRoadSeeds.add(new Point2dImpl(90, 472));
            targetRoadSeeds.add(new Point2dImpl(1239, 364));
            targetRoadSeeds.add(new Point2dImpl(1334, 94));
            targetRoadSeeds.add(new Point2dImpl(227, 530));
            targetRoadSeeds.add(new Point2dImpl(663, 388));
            targetRoadSeeds.add(new Point2dImpl(849, 193));*/
            targetRoadSeeds.add(new Point2dImpl(646, 355));
            targetRoadSeeds.add(new Point2dImpl(678, 355));

            targetRoadSeeds.add(new Point2dImpl(713, 372));
            //targetRoadSeeds.add(new Point(1535, 46));

            targetRoadSeeds = seedGeneration.toSeedsPoint2dImpl();


            List<List<Point2dImpl>> cutOffPointsList = new ArrayList<>();
            List<Polygon> cutOffShapes = new ArrayList<>();
            FImage flattened = image.flatten();

            boolean mode = true;

            List<Vertex> startingSeeds = targetRoadSeeds.stream().map(Vertex::new).collect(Collectors.toList());

            if (mode){
                RoadDetector roadDetector = new RoadDetector(flattened, startingSeeds);
                //RoadDetector roadDetector = new RoadDetector(flattened);
                roadDetector.process();
                //spokeWheel.render(image);
                roadDetector.render(image);
            } else {
                RoadNetworkExtraction rd = new RoadNetworkExtraction(image, startingSeeds);
                rd.run(false);
                rd.render(image);
            }

            ImageUtilities.write(image, new File("processed.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static FImage invariant(FImage band1, FImage band2){
        FImage inner = band1.subtract(band2).divide(band1.add(band2));
        inner.processInplace(new PixelProcessor<Float>() {
            @Override
            public Float processPixel(Float pixel) {
                return (float) ((4 / Math.PI) * Math.atan(pixel));
            }
        });
        return inner;
    }
}

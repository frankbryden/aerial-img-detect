package uk.ac.soton.ecs.fjkb1u17.buildingDetection;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.math.geometry.point.Point2dImpl;
import uk.ac.soton.ecs.fjkb1u17.Vertex;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BuildingMain {
    public static void main(String[] args){
        try {
            MBFImage image = ImageUtilities.readMBF(new File("img2.png"));

            DetectionFromShadowAndColor detection = new DetectionFromShadowAndColor(image);

            List<Vertex> buildingSeeds = new ArrayList<>();
            buildingSeeds.add(new Vertex(new Point2dImpl(740, 380)));
            buildingSeeds.add(new Vertex(new Point2dImpl(763, 390)));
            buildingSeeds.add(new Vertex(new Point2dImpl(587, 22)));
            buildingSeeds.add(new Vertex(new Point2dImpl(444, 253)));

            SpokeDetection spokeDetection = new SpokeDetection(image.flatten(), buildingSeeds);
            spokeDetection.process();
            MBFImage buildingRes = image.clone();
            spokeDetection.render(buildingRes);
            DisplayUtilities.display(buildingRes, "Buildings");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

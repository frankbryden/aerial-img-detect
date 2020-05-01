package uk.ac.soton.ecs.fjkb1u17.buildingDetection;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.processing.threshold.OtsuThreshold;

public class DetectionFromShadowAndColor {
    private MBFImage image;
    private Invariants invariants;

    public DetectionFromShadowAndColor(MBFImage image){
        this.image = image;
        this.invariants = new Invariants(image);
    }

    public void run(){
        FImage redInvariant = this.invariants.redInvariant();
        FImage shadowInvariant = this.invariants.blueInvariant();

        FImage redThresholded = redInvariant.clone().clipMin(0f);
        FImage shadowThresholded = shadowInvariant.clone().clipMin(0f);

        OtsuThreshold threshold = new OtsuThreshold();
        threshold.processImage(redThresholded);
        threshold.processImage(shadowThresholded);
        DisplayUtilities.display(redInvariant, "Red");
        DisplayUtilities.display(redThresholded, "Red Thresh");
        DisplayUtilities.display(shadowInvariant, "Shadow");
        DisplayUtilities.display(shadowThresholded, "Shadow Thresh");

        CannyEdgeDetector edgeDetector = new CannyEdgeDetector(0.2f);
        FImage target = image.flatten();
        edgeDetector.processImage(target);
        DisplayUtilities.display(target, "Edges");
    }
}

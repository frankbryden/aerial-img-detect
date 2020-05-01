package uk.ac.soton.ecs.fjkb1u17.meanShift;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MeanMain {
    public static void main(String[] args) {
        MBFImage image = null;
        try {
            image = ImageUtilities.readMBF(new File("img4.png"));
            DisplayUtilities.display(image, "Before");
            System.out.println(image.bands.get(0).pixels[0][0]);
            ArrayList<MBFImage> images = new ArrayList<>();
            for (int i = 0; i < 2; i++){
                System.out.println(i);
                MeanShift meanShift = new MeanShift(image, 16, 24);
                image = meanShift.filterRGBImage();
                int nClusters = meanShift.cluster();
                System.out.println("Found " + nClusters);
                DisplayUtilities.display(image);
                images.add(image.clone());
                break;
            }

            DisplayUtilities.display("Images", images);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

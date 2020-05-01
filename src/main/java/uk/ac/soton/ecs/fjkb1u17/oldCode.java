package uk.ac.soton.ecs.fjkb1u17;

import org.openimaj.image.FImage;

public class oldCode {
    /*
    image.processInplace(new PixelProcessor<Float[]>() {
                @Override
                public Float[] processPixel(Float[] pixel) {
                    float distance = 0f;
                    for (int i = 0; i < pixel.length; i++){
                        distance += Math.abs(targetCol[i] - pixel[i]);
                    }
                    if (distance < 0.4f){
                        return pixel;
                    } else {
                        Float[] black = new Float[3];
                        black[0] = 0f;
                        black[1] = 0f;
                        black[2] = 0f;
                        return black;
                    }
                }
            });
            DisplayUtilities.display(image, "here");
            FImage averaged = image.flatten();
            DisplayUtilities.display(averaged, "averaged");
            averaged.threshold(0.1f).inverse();
            DisplayUtilities.display(averaged, "thresholded 0.4f");

            CannyEdgeDetector ced = new CannyEdgeDetector(8);
            FImage bw = image.flatten();
            FImage orig = image.flatten();

            ced.processImage(bw);
            orig = orig.add(bw);

            DisplayUtilities.display(bw, "Canny edge detected");
            DisplayUtilities.display(orig, "Original multiplied by bw");*/
    //(B-R)/(B+R)
    //filtered = blueBand.subtract(redBand).divide(blueBand.add(redBand));

    //color invariant
            /*
            ψb = 4
π arctan( (B − G)/(B + G) )

             */
            /*
            Map<String, FImage> bands = new HashMap<>();
            bands.put("RED", redBand);
            bands.put("GREEN", greenBand);
            bands.put("BLUE", blueBand);
            String[] cols = {"RED", "GREEN", "BLUE"};
            for (int i = 0; i < cols.length; i++){
                for (int j = i + 1; j < cols.length; j++){
                    FImage invariant = invariant(bands.get(cols[i]), bands.get(cols[j]));
                    DisplayUtilities.display(invariant, cols[i] + " + " + cols[j]);
                    ced.processImage(invariant);
                    DisplayUtilities.display(invariant, cols[i] + " + " + cols[j]);
                }
            }*/

    //FImage invariant = invariant(greenBand, blueBand);
    //DisplayUtilities.display(invariant, "GREEN + BLUE");
            /*OtsuThreshold otsuThreshold = new OtsuThreshold(1);
            otsuThreshold.processImage(invariant);*/
            /*invariant.threshold(0.01f);
            DisplayUtilities.display(invariant, "thresholded");
            FImage flattened = image.flatten();
            DisplayUtilities.display(flattened, "flattened");
            FImage result = flattened.subtract(invariant);
            DisplayUtilities.display(result, "RESULT");

            filtered.threshold(0.02f);

            DisplayUtilities.display(filtered);*/
            /*
            ψb = 4
π arctan( (B − G)/(B + G) )

             */



    //Display the image
    //DisplayUtilities.display(filtered);

}

package uk.ac.soton.ecs.fjkb1u17.buildingDetection;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processor.PixelProcessor;

public class Invariants {
    private MBFImage image;

    public Invariants(MBFImage image){
        this.image = image;
    }


    private FImage createInvariant(FImage c1, FImage c2){
        FImage result = c1.subtract(c2).divide(c1.add(c2));
        final double MULT = 4/Math.PI;
        result.processInplace(pixel -> (float) (MULT * Math.atan(pixel)));
        return result;
    }

    public FImage redInvariant(){
        return createInvariant(image.getBand(0), image.getBand(1));
    }

    public FImage blueInvariant(){
        return createInvariant(image.getBand(2), image.getBand(1));
    }
}

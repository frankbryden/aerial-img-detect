package uk.ac.soton.ecs.fjkb1u17;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.renderer.MultiBandRenderer;

public class MyRenderer extends MultiBandRenderer<Float, MBFImage, FImage> {
    private MBFImage target;

    public MyRenderer(MBFImage target){
        super(target);
        this.target = target;
    }

    @Override
    protected void drawHorizLine(int x1, int x2, int y, Float[] col) {

    }

    @Override
    public void drawLine(int x0, int y0, int x1, int y1, int thickness, Float[] grey) {
        System.out.println("bands : " + this.target.bands.size());
        System.out.println("grey : " + grey.length);
        super.drawLine(x0, y0, x1, y1, thickness, grey);
    }

    @Override
    public Float[] defaultForegroundColour() {
        return new Float[0];
    }

    @Override
    public Float[] defaultBackgroundColour() {
        return new Float[0];
    }

    @Override
    protected Float[] sanitise(Float[] colour) {
        return colour;
    }
}

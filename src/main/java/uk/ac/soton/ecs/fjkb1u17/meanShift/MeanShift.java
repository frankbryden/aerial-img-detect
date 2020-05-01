package uk.ac.soton.ecs.fjkb1u17.meanShift;

import org.apache.batik.util.DoublyLinkedList;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.pixel.ConnectedComponent;
import org.openimaj.image.pixel.Pixel;
import org.openimaj.image.processor.ImageProcessor;
import org.openimaj.math.geometry.shape.Circle;
import uk.ac.soton.ecs.fjkb1u17.Point;

import java.util.*;
import java.util.stream.IntStream;

public class MeanShift {
    private MBFImage image;
    private int radius, radius_squared;
    private int coldiff, coldiff_squared;
    private float[][] convergencePoints;
    private int minRegion = 10;
    private int maxDist = 500; //if color dist is more than this, don't merge

    public MeanShift(MBFImage image, int radius, int coldiff) {
        this.image = image;//ColourSpace.convert(image, ColourSpace.CIE_Lab);;
        this.radius = radius;
        this.radius_squared = radius * radius;
        this.coldiff = coldiff;
        this.coldiff_squared = coldiff*coldiff;
    }

    public MBFImage runMeanShift(){
        for (int y = 0; y < image.getHeight(); y++){
            for (int x = 0; x < image.getWidth(); x++){
                ConnectedComponent cc = new ConnectedComponent(new Circle(x, y, radius));
                Set<Pixel> pixels = cc.pixels;
                Float[] meanOfNeighbours = computeMean(pixels);
                //System.out.println(colorDist(meanOfNeighbours, image.getPixel(x, y)));
                //if (colorDist(meanOfNeighbours, image.getPixel(x, y)) < coldiff){
                    /*System.out.println("Before: " + Arrays.toString(image.getPixel(x, y)));
                    System.out.println("After: " + Arrays.toString(meanOfNeighbours));*/
                //    image.setPixel(x, y, meanOfNeighbours);
                //}
            }
        }
        return ColourSpace.convert(image, ColourSpace.RGB);
    }

    public MBFImage filterRGBImage() {
        //adapted from: https://imagej.nih.gov/ij/plugins/download/Mean_Shift.java
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.toPackedARGBPixels();
        System.out.println("Starting : " + pixels.length);
        float[][] pixelsf = new float[width*height][3];
        convergencePoints = new float[width*height][3];

        for (int i = 0; i < pixelsf.length; i++) {
            int argb = pixels[i];

            int r = (argb >> 16) & 0xff;
            int g = (argb >>  8) & 0xff;
            int b = (argb) & 0xff;

            pixelsf[i][0] = 0.299f  *r + 0.587f *g + 0.114f  *b; // Y
            pixelsf[i][1] = 0.5957f *r - 0.2744f*g - 0.3212f *b; // I
            pixelsf[i][2] = 0.2114f *r - 0.5226f*g + 0.3111f *b; // Q
        }

        float shift = 0;
        int iters = 0;
        for (int y=0; y<height; y++) {
            if (y%20==0) {
                System.out.println(y + " / " + height);
            }
            for (int x=0; x<width; x++) {

                int xc = x;
                int yc = y;
                int xcOld, ycOld;
                float YcOld, IcOld, QcOld;
                int pos = y*width + x;
                float[] yiq = pixelsf[pos];
                float Yc = yiq[0];
                float Ic = yiq[1];
                float Qc = yiq[2];

                iters = 0;
                do {
                    xcOld = xc;
                    ycOld = yc;
                    YcOld = Yc;
                    IcOld = Ic;
                    QcOld = Qc;

                    float mx = 0;
                    float my = 0;
                    float mY = 0;
                    float mI = 0;
                    float mQ = 0;
                    int num=0;

                    for (int ry=-radius; ry <= radius; ry++) {
                        int y2 = yc + ry;
                        if (y2 >= 0 && y2 < height) {
                            for (int rx=-radius; rx <= radius; rx++) {
                                int x2 = xc + rx;
                                if (x2 >= 0 && x2 < width) {
                                    if (ry*ry + rx*rx <= radius_squared) {
                                        yiq = pixelsf[y2*width + x2];

                                        float Y2 = yiq[0];
                                        float I2 = yiq[1];
                                        float Q2 = yiq[2];

                                        float dY = Yc - Y2;
                                        float dI = Ic - I2;
                                        float dQ = Qc - Q2;

                                        if (dY*dY+dI*dI+dQ*dQ <= coldiff_squared) {
                                            mx += x2;
                                            my += y2;
                                            mY += Y2;
                                            mI += I2;
                                            mQ += Q2;
                                            num++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    float num_ = 1f/num;
                    Yc = mY*num_;
                    Ic = mI*num_;
                    Qc = mQ*num_;
                    xc = (int) (mx*num_+0.5);
                    yc = (int) (my*num_+0.5);
                    int dx = xc-xcOld;
                    int dy = yc-ycOld;
                    float dY = Yc-YcOld;
                    float dI = Ic-IcOld;
                    float dQ = Qc-QcOld;

                    shift = dx*dx+dy*dy+dY*dY+dI*dI+dQ*dQ;
                    iters++;
                }
                while (shift > 3 && iters < 100);

                convergencePoints[y*width + x][0] = Yc;
                convergencePoints[y*width + x][1] = Ic;
                convergencePoints[y*width + x][2] = Qc;

                int r_ = (int)(Yc + 0.9563f*Ic + 0.6210f*Qc);
                int g_ = (int)(Yc - 0.2721f*Ic - 0.6473f*Qc);
                int b_ = (int)(Yc - 1.1070f*Ic + 1.7046f*Qc);

                pixels[pos] = (0xFF<<24)|(r_<<16)|(g_<<8)|b_;
            }

        }

        return new MBFImage(pixels, width, height, false);
    }

    //function adapted from http://www.ipol.im/pub/art/2019/255/article_lr.pdf
    int cluster(){
        System.out.println("Running cluster");
        int width = image.getWidth();
        int height = image.getHeight();
        int regCount = 0;
        int lbl = -1;
        final int[][] dxdy = { {-1,-1} , {-1,0} , {-1,1} , {0,-1} , {0,1} , {1,-1} , {1,0} , {1,1} };
        int[] labels = new int[height*width];
        Map<Integer, int[]> labelColMap = new HashMap<>();
        for (int i = 0; i < height; i++){
            for (int j = 0; j < width; j++){
                labels[i*height+j] = -1;
            }
        }

        for (int j = 0; j < height; j++){
            for(int i = 0; i < width; i++){
                if(labels[j*height + i] < 0){   // if label is not assigned
                    labels[j*height + i] = ++lbl;
                    regCount++;

                    float[] col = convergencePoints[j*height + i];

                    int r = (int)(col[0] + 0.9563f*col[1] + 0.6210f*col[2]);
                    int g = (int)(col[0] - 0.2721f*col[1] - 0.6473f*col[2]);
                    int b = (int)(col[0] - 1.1070f*col[1] + 1.7046f*col[2]);

                    labelColMap.put(lbl, new int[]{r, g, b});

                    int pos = j*height + i;
                    float L = convergencePoints[pos][0];  // L
                    float U = convergencePoints[pos][1];  // u
                    float V = convergencePoints[pos][2];  // v

                    Stack<Point> stack = new Stack<>();
                    stack.push(new Point(i, j));

                    while(!stack.empty()){
                        Point point = stack.pop();

                        for(int k = 0; k < 8; k++) // calculate for 8 connected pixels
                        {
                            int ii = point.x + dxdy[k][0];
                            int jj = point.y + dxdy[k][1];

                            if(ii >= 0 && jj >= 0 && jj < height && ii < width && labels[jj*height + ii] < 0 && colorDist(convergencePoints[j*height + i], convergencePoints[jj*height + ii]) < coldiff_squared) {
                                labels[jj * height + ii] = lbl;
                                stack.push(new Point(ii, jj));
                            }
                        }
                    }
                }
            }
        }


        int current = labels[0];
        int currentCount = 0;
        Map<Integer, Integer> labelCount = new HashMap<>();
        LinkedList<Integer> regionSequence = new LinkedList<>();
        regionSequence.add(current);
        for (int i = 0; i < labels.length; i++){
            if (current == labels[i]){
                currentCount++;
            } else {
                labelCount.put(current, currentCount);
                currentCount = 1;
                current = labels[i];
                regionSequence.add(current);
            }
        }
        labelCount.put(current, currentCount);

        for (int key: labelCount.keySet()){
            //System.out.printf("%d -> %d\n", key, labelCount.get(key));
        }
        System.out.println("Fresh region sequence");
        System.out.println(regionSequence);
        //labels = mergeSmallRegions(labels, regionSequence, labelCount, labelColMap);



        MBFImage img = reconstruct(labels, labelColMap, width, height);
        DisplayUtilities.display(img, "We are here");
        return regCount;
    }

    private int[] mergeSmallRegions(int[] labels, LinkedList<Integer> regionSequence, Map<Integer, Integer> labelCount, Map<Integer, int[]> labelColMap){
        //merge small regions with the closest nearby (in terms of colour) region
        boolean done = false;
        System.out.println("Starting with " + regionSequence.size() + " regions");
        while (!done){
            System.out.println("Not done yet");
            done = true;
            Iterator<Integer> it = regionSequence.iterator();
            while(it.hasNext()){
                //System.out.println("Iteration - on labelIndex " + labelIndex);


                final int label = it.next();

                if (labelCount.get(label) < minRegion){
                    //done = false;
                    //System.out.println(label + " has " + labelCount.get(label) + " points and thus needs to be merged");
                    //this region needs to be merged
                    int regionIndex = regionSequence.indexOf(label);
                    if (regionIndex == -1){
                        System.out.println("We have a problem, apparently " + label + " is not in regionSeq");
                        //System.out.println(regionSequence.toString());
                    }



                    int[] currentColour = labelColMap.get(label);
                    int prevRegionDist, nextRegionDist; //The colour distances to the previous and next regions. The smallest one will be picked for merging.
                    if (regionIndex == 0){
                        prevRegionDist = 100000; // No previous region, set to high number
                    } else {
                        //System.out.println(regionSequence.get(regionIndex - 1));
                        int[] prevRegionColour = labelColMap.get(regionSequence.get(regionIndex - 1));
                        prevRegionDist = colorDist(currentColour, prevRegionColour);
                        //System.out.println("One possible candidate is the previous region, " + regionSequence.get(regionIndex - 1));
                    }
                    if (regionIndex == regionSequence.size() - 1) {
                        nextRegionDist = 100000; // No following region, set to high number
                    } else {
                        int[] nextRegionColour = labelColMap.get(regionSequence.get(regionIndex - 1));
                        nextRegionDist = colorDist(currentColour, nextRegionColour);
                        //System.out.println("One possible candidate is the following region, " + regionSequence.get(regionIndex + 1));
                    }
                    if (nextRegionDist > maxDist && prevRegionDist > maxDist){
                        //continue;
                    }
                    int labelToMerge;
                    if (nextRegionDist < prevRegionDist){
                        //we need to merge with next
                        labelToMerge = regionSequence.get(regionIndex + 1);
                        System.out.println(nextRegionDist);
                        //System.out.println("Choosing following region");
                    } else {
                        System.out.println(prevRegionDist);
                        labelToMerge = regionSequence.get(regionIndex - 1);
                        //System.out.println("Choosing previous region");
                    }
                    //actual merging
                    for (int i = 0; i < labels.length; i++){
                        if (labels[i] == label){
                            labels[i] = labelToMerge;
                        }
                    }
                    //System.out.println("Merged " + label + " with " + labelToMerge);

                    it.remove();
                }
            }
        }
        System.out.println("Ending with " + regionSequence.size() + " regions");
        return labels;

    }

    private MBFImage reconstruct(int[] labels, Map<Integer, int[]> labelColMap, int width, int height) {
        int[] pixels = new int[labels.length];
        System.out.println("Final length: " + pixels.length);
        System.out.println(Arrays.toString(labels));
        System.out.println(labels.length);
        for (int j = 0; j < 10; j++){
            System.out.println(Arrays.toString(labelColMap.get(j)));
        }
        for (int i = 0; i < labels.length; i++){
            int[] col = labelColMap.get(labels[i]);
            final int rgb = 0xff << 24 | col[0] << 16 | col[1] << 8 | col[2];


            pixels[i] = rgb;
        }
        return new MBFImage(pixels, width, height);
    }


    private Float[] computeMean(Set<Pixel> pixels){
        Float[] total = {0f, 0f, 0f};

        for (Pixel p: pixels){
            if (p.x < 0 || p.y < 0 || p.x >= image.getWidth() || p.y >= image.getHeight()){
                continue;
            }
            Float[] cur = image.getPixel(p);
            for (int i = 0; i < cur.length; i++){
                total[i] += cur[i];
            }
        }
        //divide by length to obtain mean
        for (int i = 0; i < total.length; i++){
            total[i] /= pixels.size();
        }
        return total;
    }

    private double colorDist(float[] c1, float[] c2) {
        float dist = 0;
        for (int i = 0; i < c1.length; i++){
            dist += Math.pow(c1[i] - c2[i], 2);
        }
        return dist;

    }

    private int colorDist(int[] c1, int[] c2) {
        int dist = 0;
        for (int i = 0; i < c1.length; i++){
            dist += Math.pow(c1[i] - c2[i], 2);
        }
        return dist;

    }

}

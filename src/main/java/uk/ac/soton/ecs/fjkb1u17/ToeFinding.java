package uk.ac.soton.ecs.fjkb1u17;

import org.openimaj.math.geometry.point.Point2dImpl;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ToeFinding {
    private final double EPSILON_1 = 0.8;
    private final int EPSILON_2 = RoadDetector.SPOKE_COUNT/8;
    private final double EPSILON_3 = 0.8;
    private Point2dImpl center;
    private List<Point2dImpl> cutOffPoints;
    private List<Double> distances; // Distances from center point to cutoff point
    private List<Integer> peakIndices; //Indices of peaks. This list will be shrunk throughout the alg as peaks are filtered.
    private double distanceShift; // distance function needs to be shifted as per algorithm description
    private double avgDistance;

    public ToeFinding(Point2dImpl center, List<Point2dImpl> cutOffPoints) {
        this.center = center;
        this.cutOffPoints = cutOffPoints;

        this.init();
    }

    private void init(){
        this.initDistances();
        this.initDeltaFunction();
        this.peakIndices = new ArrayList<>();
    }

    public void run(){
        this.findAllPeaks();

        int peakCount = this.peakIndices.size();
        //System.out.println("Starting off with " + this.peakIndices.size());

        this.removeSmallPeaks();
        checkAndReport(peakCount, "remove small peaks");
        peakCount = this.peakIndices.size();


        this.mergeNearPeaks();
        checkAndReport(peakCount, "merge near peaks");
        peakCount = this.peakIndices.size();

        /*this.cutShortValleys();
        checkAndReport(peakCount, "cut short valleys");
        peakCount = this.peakIndices.size();
        System.out.println("we end up with " + peakCount + " peaks");*/
    }

    private void checkAndReport(int count, String step){
        if (count != this.peakIndices.size()){
            //System.out.println(step + " removed " + (count - this.peakIndices.size()) + " peaks");
        }
    }

    private void initDistances(){
        this.distances = new ArrayList<Double>();
        double totalDistance = 0;
        for (Point2dImpl p : this.cutOffPoints){
            double distance = this.euclidianDist(p, center);
            distances.add(distance);
            totalDistance += distance;
        }
        this.avgDistance = totalDistance/distances.size();
    }

    private void initDeltaFunction(){
        this.distanceShift = (int) this.avgDistance - this.distances.get(0);
    }

    private double euclidianDist(Point2dImpl p1, Point2dImpl p2){
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    private double delta(int i){
        return distances.get(i) + distanceShift;
    }

    private void    findAllPeaks(){
        for (int i = 0; i < distances.size(); i++){
            // i is a peak if it is larger than (i - 1) and (i + 1)
            if (delta(i) > delta((i + 1) % distances.size()) && delta(i) > delta((i - 1 + distances.size()) % distances.size())){
                peakIndices.add(i);
            }
        }
    }

    private void removeSmallPeaks() {
        double highestPeak = -1;
        for (int peakIndex : this.peakIndices) {
            if (delta(peakIndex) > highestPeak) {
                highestPeak = delta(peakIndex);
            }
        }

        List<Integer> newIndices = new ArrayList<>(); //only add non-small peaks
        for (int peakIndex : this.peakIndices) {
            //Remove small peaks and peaks which are smaller than the average distance
            if (delta(peakIndex) * 1.0f / highestPeak >= EPSILON_1){// || delta(peakIndex) > avgDistance) {
                newIndices.add(peakIndex);
            }
        }
        this.peakIndices = newIndices;
    }

    private void merge2Peaks(List<Integer> peaks){
        for (int i1: peaks){
            for (int i2 : peaks.subList(peaks.indexOf(i1) + 1, peaks.size())){
                if (Math.abs(i2 - i1) < EPSILON_2 || Math.abs(this.cutOffPoints.size() - i2 + i1) < EPSILON_2) {
                    if (delta(i1) > delta(i2)){
                        peaks.remove((Integer) i2);
                    } else {
                        peaks.remove((Integer) i1);
                    }
                    return;
                }
            }
        }
    }

    private void mergeNearPeaks(){
        int peakCount = this.peakIndices.size();
        this.merge2Peaks(this.peakIndices);
        //Repeat process while current size is smaller than previous size - ie. list is getting smaller
        while (this.peakIndices.size() < peakCount){
            peakCount = this.peakIndices.size();
            //System.out.println("Here with newpeaks size " + peakCount);
            this.merge2Peaks(this.peakIndices);
        }
        //System.out.println("Done merging peaks, we now have " + this.peakIndices.size() + " peaks");
    }

    private void cutShortValleys(){
        List<Integer> newPeaks = new ArrayList<>();
        for (Integer i1 : this.peakIndices){
            for (int i2 : this.peakIndices.subList(this.peakIndices.indexOf(i1) + 1, this.peakIndices.size())){

                double deltaAvg = 0;
                for (int j = i1; j <= i2; j++){
                    deltaAvg += delta(j);
                }
                deltaAvg /= (i2  - i1 + 1);
                if ((2*deltaAvg)/(delta(i1) + delta(i2)) > EPSILON_3){
                    //remove the smaller of the two peaks, therefore add the larger one of the two
                    if (delta(i1) > delta(i2)){
                        System.out.println("(" + i1 + "; " + i2 + ")");
                        newPeaks.add(i1);
                    }
                } else {
                    //No peaks to remove - add smaller index back to the newPeaks list
                    System.out.println("(" + i1 + "; " + i2 + ")");
                    newPeaks.add(i1);
                }
            }
        }
        System.out.println("newpeaks size : " + newPeaks.size());
        this.peakIndices = newPeaks;
    }

    public List<Point2dImpl> getPeaks(){
        List<Point2dImpl> peakPoints = new ArrayList<>();
        for (int peakIndex : peakIndices){
            peakPoints.add(cutOffPoints.get(peakIndex));
        }
        return peakPoints;
    }
}

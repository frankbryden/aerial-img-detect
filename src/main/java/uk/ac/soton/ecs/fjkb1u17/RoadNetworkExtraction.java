package uk.ac.soton.ecs.fjkb1u17;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Polygon;

import java.security.Policy;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RoadNetworkExtraction {
    private final int BUILDING_DIST = 40; //Distance of houses from road, in pixels
    private MBFImage target;
    private FImage flattened, hue, saturation;
    private List<Point2dImpl> roadCandidates;
    private List<Polygon> footprints;
    private RoadTreeNode tree;
    private int iterations = 0;

    public RoadNetworkExtraction(MBFImage target, List<Vertex> startingSeeds){
        this.target = target;
        this.flattened = target.flatten();
        MBFImage tempHSV = Transforms.RGB_TO_HSV(target);
        this.hue = tempHSV.getBand(0);
        this.saturation = tempHSV.getBand(1);
        this.flattened = target.flatten();
        this.roadCandidates = new ArrayList<>();
        this.footprints = new ArrayList<>();
        this.tree = new RoadTreeNode(new ArrayList<>(), startingSeeds);
        //this.init();
    }

    private void init(){
        //TODO From a footprint, spawn footprints at yellow points
        // and do it repeatedly until not possible (as we already do)
        // count number of possible iterations (or number of spawned yellow points?)
        // From that number, decide to accept or reject that initial footprint.
        // Idea is to eliminate false positives like small patches of concrete-like pixels.
        List<Vertex> startingSeeds = new ArrayList<>();
        /*startingSeeds.add(new Vertex(new Point2dImpl(639, 110)));
        startingSeeds.add(new Vertex(new Point2dImpl(1297, 408)));
        startingSeeds.add(new Vertex(new Point2dImpl(1215, 346)));
        startingSeeds.add(new Vertex(new Point2dImpl(894, 421)));
        startingSeeds.add(new Vertex(new Point2dImpl(618, 497)));
        startingSeeds.add(new Vertex(new Point2dImpl(525, 359)));*/
        //Values for "texas.png"
        /*startingSeeds.add(new Vertex(new Point2dImpl(646, 355)));
        startingSeeds.add(new Vertex(new Point2dImpl(451, 595)));*/
        //Values for "easiestRoad.png"
        startingSeeds.add(new Vertex(new Point2dImpl(212, 378)));
        this.tree = new RoadTreeNode(new ArrayList<>(), startingSeeds);
    }

    public void run(boolean networkExpansionTest){
        while (tree.hasLiveVertex() && this.iterations < 520){
            this.iter();
            this.iterations++;
            if (networkExpansionTest && this.iterations > 4){
                break;
            }
            /*MBFImage copy = this.target.clone();
            this.render(copy);
            DisplayUtilities.displayName(copy, "render");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            //System.out.println(this.iterations);
        }
        //tree.vertices.removeIf(v -> this.saturation.pixels[(int) v.pos.y][(int) v.pos.y] > 0.2);*
        if (networkExpansionTest)
            return;
        MBFImage clone = this.target.clone();
        this.render(clone);
        DisplayUtilities.display(clone, "Filtered");
        //DisplayUtilities.display(saturation, "sat");
    }

    public void prune(){
        //This is a highly important step!
        //The idea is to generate as many candidate roads as possible, then prune our tree to remove
        //unwanted candidates.
        //Our pruning conditions:
        //  - After n steps, toe is still within k pixels of its parent (need to empirically determine n and k)
        //  - Mean saturation less than 50
        Stack<RoadTreeNode> fringe = new Stack<>();
        fringe.add(tree);
        while (!fringe.empty()) {
            RoadTreeNode node = fringe.pop();
        }

    }

    public void filterNearNeighbours(int dist){
        //Go through every seed. Remove all of its neighbours within a radius of 'dist'.
        System.out.println("Starting with " + this.tree.vertices.size() + " seeds.");
        Iterator<Vertex> iter = this.tree.vertices.iterator();
        while (iter.hasNext()){
            Vertex seed = iter.next();
            for (int i = 0; i < this.tree.vertices.size(); i++){
                Vertex neighbour = this.tree.vertices.get(i);
                if (neighbour == seed){
                    continue;
                }
                if (seed.dist(neighbour) < dist){
                    iter.remove();
                    break;
                }
            }
        }
        this.tree.edges.removeIf(edge -> !this.tree.vertices.contains(edge.v1) || !this.tree.vertices.contains(edge.v2));
        this.recreateLocalEdges();
        System.out.println("After filtering, we have " + this.tree.vertices.size() + " seeds.");
    }

    private void recreateLocalEdges(){
        //The filter neighbours method has the effect of removing at least one vertex in 80+% of edges.
        //We must therefore connect each seed with its nearest neighbour
        for (int i = 0; i < this.tree.vertices.size(); i++){
            Vertex v1 = this.tree.vertices.get(i);
            int minDist = 10000;
            Vertex closestVertex = null;
            for (int j = 0; j < this.tree.vertices.size(); j++){
                if (i != j) {
                    Vertex v2 = this.tree.vertices.get(j);
                    int dist = v1.dist(v2);
                    if (dist < minDist){
                        boolean foundEdge = false;
                        for (Edge edge: this.tree.edges){
                            if ((edge.v1 == v1 && edge.v2 == v2) || (edge.v1 == v2 && edge.v2 == v1)) {
                                foundEdge = true;
                                break;
                            }
                        }
                        if (!foundEdge){
                            minDist = dist;
                            closestVertex = v2;
                        }
                    }
                }

            }
            if (closestVertex != null)
                this.tree.edges.add(new Edge(v1, closestVertex));
        }
    }

    public void render(MBFImage target){
        //target.drawPoints(roadCandidates, RGBColour.RED, 2);
        //this.footprints.forEach(p -> target.drawShape(p, RGBColour.CYAN));
        this.tree.edges.forEach(e -> target.drawLine(e.getLine(), 2, RGBColour.MAGENTA));
        this.tree.vertices.forEach(v -> target.drawPoint(v.pos, RGBColour.CYAN, 2));
        //this.tree.get(this.tree.size() - 1).vertices.forEach(v -> target.drawPoint(v.pos, RGBColour.CYAN, 2));
    }

    private void iter(){
        List<Vertex> liveVertices = tree.getLiveVertices();
        liveVertices.forEach(v -> {
            //We're going to explore this vertex now - set it to dead
            v.alive = false;
            //For each vertex spawn a road detector, retrieve toes, and create edges from those
            //then add toes to tree
            List<Vertex> vs = new ArrayList<>();
            vs.add(v);
            //I'm feeling using hue does not give great results - using flattened for now
            RoadDetector detector = new RoadDetector(this.flattened, vs);
            detector.process();
            List<Point2dImpl> toes = detector.getToes().get(0);
            toes.forEach(t -> {
                Vertex toeVertex = new Vertex(t);
                if (footprints.stream().map(f -> f.isInside(toeVertex.pos)).collect(Collectors.toList()).contains(true)){
                    toeVertex.alive = false;
                }
                tree.edges.add(new Edge(v, toeVertex));
                if (inBounds(toeVertex)){
                    tree.vertices.add(toeVertex);
                    tree.addChild(new RoadTreeNode(toeVertex));
                }
            });
            detector.getCutOffPoints().forEach(ps -> {
                roadCandidates.addAll(ps);
                footprints.add(new Polygon(ps));
            });
        });
        /*
        RoadDetector detector = new RoadDetector(this.flattened, liveVertices);
        detector.process();
        List<List<Point2dImpl>> toes = detector.getToes();
        int i = 0;
        for (List<Point2dImpl> footprintToes : toes){
            List<Vertex> vertices = footprintToes.stream().map(Vertex::new).collect(Collectors.toList());
            for (Vertex vertex : vertices){
                tree.edges.add(new Edge(liveVertices.get(i), vertex));
                if (footprints.stream().map(f -> f.isInside(vertex.pos)).collect(Collectors.toList()).contains(true)){
                    vertex.alive = false;
                }
            }
            tree.vertices.addAll(vertices);
            i++;
            /*RoadTreeNode node = new RoadTreeNode(vertices);
            tree.add(node);
        }
        detector.getCutOffPoints().forEach(ps -> {
            roadCandidates.addAll(ps);
            footprints.add(new Polygon(ps));
        });*/
    }

    public float getMSD(Vertex centre){
        float totalDistance = 0.0f;
        for (Vertex v: this.tree.vertices){
            totalDistance += Math.sqrt(Math.pow(v.pos.x - centre.pos.x, 2) + Math.pow(v.pos.y - centre.pos.y, 2));
        }
        return totalDistance/this.tree.vertices.size();
    }

    public List<Vertex> possibleBuildingSeeds(){
        List<Vertex> buildingSeeds = new ArrayList<>();
        for (Edge edge: this.tree.edges){
            double angle = edge.getAngle();
            Vertex b1Seed = getBuildingSeed(edge.v1, angle + Math.PI/2);
            Vertex b2Seed = getBuildingSeed(edge.v1, angle - Math.PI/2);
            buildingSeeds.add(b1Seed);
            buildingSeeds.add(b2Seed);
        }
        return buildingSeeds;
    }

    private Vertex getBuildingSeed(Vertex start, double angle){
        return new Vertex((int) (start.pos.x + Math.cos(angle)*BUILDING_DIST),
                (int) (start.pos.y + Math.sin(angle)*BUILDING_DIST));
    }

    public FImage getRoadsBinaryImage(){
        //Returns a binary image where 1 is a road and 0 is off-road
        long start = System.currentTimeMillis();
        FImage output = new FImage(this.target.getWidth(), this.target.getHeight());
        for (int x = 0; x < output.getWidth(); x++){
            for (int y = 0; y < output.getHeight(); y++){
                for (Polygon footprint: footprints){
                    if (footprint.isInside(new Point2dImpl(x, y))){
                        output.setPixel(x, y, 1f);
                    }
                }
            }
        }
        System.out.println("Took " + (System.currentTimeMillis() - start)/1000 + "s to compute image");
        return output;
    }

    public FImage getRoadsBinaryImagePolys(){
        long start = System.currentTimeMillis();
        FImage output = new FImage(this.target.getWidth(), this.target.getHeight());
        for (Polygon poly: this.footprints){
            output.drawPolygonFilled(poly, 1f);
        }
        System.out.println("Took " + (System.currentTimeMillis() - start) + "s to compute image using fill polys");
        return output;
    }

    public Polygon getRoadsAsPoly(){
        Polygon wholeRoad = this.footprints.get(0).clone();
        this.footprints.subList(1, this.footprints.size()).forEach(wholeRoad::addInnerPolygon);
        return wholeRoad;
    }

    private boolean inBounds(Vertex v){
        return v.pos.x >= 0 && v.pos.x < this.target.getWidth() && v.pos.y >= 0 && v.pos.y < this.target.getHeight();
    }

    public int getIterCount(){
        return this.iterations;
    }

}

package uk.ac.soton.ecs.fjkb1u17;

import java.util.ArrayList;
import java.util.List;

public class RoadTreePruning {
    private RoadTreeNode tree;
    private List<Vertex> trueRoadVertices, falseRoadVertices;

    public RoadTreePruning(RoadTreeNode tree) {
        this.tree = tree;
        this.trueRoadVertices = new ArrayList<>();
        this.falseRoadVertices = new ArrayList<>();
    }


}

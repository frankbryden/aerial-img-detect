package uk.ac.soton.ecs.fjkb1u17;

import org.openimaj.math.geometry.line.Line2d;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RoadTreeNode {
    public List<Edge> edges;
    public List<Vertex> vertices;
    Vertex vertex;
    Edge edge;
    private ArrayList<RoadTreeNode> children;
    private int level;

    public RoadTreeNode(List<Edge> edges, List<Vertex> vertices) {
        this.edges = edges;
        this.vertices = vertices;
        this.children = new ArrayList<>();
    }

    public RoadTreeNode(Vertex v){
        this.vertex = v;
        this.children = new ArrayList<>();
    }

    public RoadTreeNode(List<Vertex> vertices) {
        this.vertices = vertices;
    }

    public boolean hasLiveVertex(){
        for (Vertex vertex : this.vertices){
            if (vertex.alive){
                return true;
            }
        }
        return false;
    }

    public void addChild(RoadTreeNode node){
        this.children.add(node);
    }

    public List<Vertex> getLiveVertices(){
        return this.vertices.stream().filter(v -> v.alive).collect(Collectors.toList());
    }
}

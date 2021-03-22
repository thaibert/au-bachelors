package pathfinding.standard;

import graph.*;
import graph.Vertex;
import pathfinding.framework.*;
import utility.*;

import java.util.*;

public class BidirectionalALT implements PathfindingAlgo{

    private static final double INF_DIST = Double.MAX_VALUE;

    private Graph graph;

    private Map<Vertex, Map<Vertex, Double>> distanceToLandmark;
    private Map<Vertex, Map<Vertex, Double>> distanceFromLandmark;
    private Collection<Vertex> reachableLandmarks;

    private Map<Vertex, Double> dist;
    private Map<Vertex, Vertex> parent;

    public BidirectionalALT(Graph graph, int noLandmarks) {
        this.graph = graph;

        
        Graph ginv = GraphUtils.invertGraph(graph);

        List<Vertex> landmarks = landmark(graph, noLandmarks);
        // List<Vertex> landmarks = new ArrayList<>();
        // landmarks.add(GraphUtils.findNearestVertex(graph, 56.21684389259911, 9.517964491806737));
        // landmarks.add(new Vertex(56.0929669, 10.0084564));

        distanceToLandmark = new HashMap<>();
        distanceFromLandmark = new HashMap<>();

        landmarks.forEach( l -> {
            System.out.print(".");
            Map<Vertex, Double> normal = dijkstra(graph, l);
            Map<Vertex, Double> inv = dijkstra(ginv, l);

            distanceFromLandmark.put(l, normal);
            distanceToLandmark.put(l, inv);
        });
    }


    @Override
    public Solution shortestPath(Vertex start, Vertex goal) {
        // TODO Auto-generated method stub
        return null;
    }

    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("denmark-all-roads.csv");

        Vertex a = new Vertex(56.0440049,9.9025227);
        Vertex b = new Vertex(56.1814955,10.2042923);

        PathfindingAlgo d = new BidirectionalALT(graph, 5);
        Solution solution = d.shortestPath(Location.Skagen, Location.CPH);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.Denmark);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize("ALT");

    }
    
}

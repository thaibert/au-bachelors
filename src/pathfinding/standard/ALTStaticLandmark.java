package pathfinding.standard;

import graph.*;
import graph.Vertex;
import pathfinding.framework.*;
import utility.*;

import java.util.*;

public class ALT implements PathfindingAlgo {
    private static final double INF_DIST = Double.MAX_VALUE;

    private Graph graph;

    LandmarkSelector landmarkSelector;

    private Map<Vertex, Double> dist;
    private Map<Vertex, Vertex> parent;

    // For visual
    private List<Edge> edgesConsidered;

    public ALT(Graph graph, LandmarkSelector landmarkSelector) {
        this.graph = graph;

        // List<Vertex> landmarks = new ArrayList<>();
        // landmarks.add(GraphUtils.findNearestVertex(graph, 56.21684389259911, 9.517964491806737));
        // landmarks.add(new Vertex(56.0929669, 10.0084564));

        this.landmarkSelector = landmarkSelector;


    }

    @Override
    public Solution shortestPath(Vertex start, Vertex goal) {
        landmarkSelector.updateLandmarks(start, goal, 2);


        Map<Vertex, Double> dist = new HashMap<>();
        Map<Vertex, Vertex> parent = new HashMap<>(); 
        List<Edge> edgesConsidered = new ArrayList<>();
        
        dist.put(start, 0.0);

        DistComparator distComparator = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(distComparator);

        pq.add(new Pair(start, 0));

        Set<Vertex> settled = new HashSet<>();

        int iterations = 0;
        while(pq.size() > 0){

            iterations++;
            Pair head = pq.poll();

            settled.add(head.v);

            if (iterations % 1000 == 0) {
                //System.out.println("    --> " + iterations + ",   pq size: " + pq.size());
            }

            if (head.v.equals(goal)) {
                System.out.println("  --> Finished early at " + iterations);
                break;
            }

            for (Neighbor n : graph.getNeighboursOf(head.v)) {
                // RELAX
                double maybeNewBestDistance = dist.get(head.v) + n.distance; // dist(s,v) + len(v,u)
                double previousBestDistance = dist.getOrDefault(n.v, INF_DIST); // dist(s,u)

                edgesConsidered.add(new Edge(head.v, n.v, maybeNewBestDistance));

                if (maybeNewBestDistance < previousBestDistance) {
                    dist.put(n.v, maybeNewBestDistance);
                    parent.put(n.v, head.v);

                    if (! settled.contains(n.v)) {
                        pq.add(new Pair(n.v, maybeNewBestDistance + landmarkSelector.pi(n.v, goal))); 
                    }
                }
            }
        }

        // Get out the shortest path
        System.out.println("  --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        if (parent.get(goal) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered, null);
        }

        Vertex temp = goal;
        while (! start.equals(temp)) {
            out.add(temp);
            temp = parent.get(temp);
        }
        out.add(start);
        System.out.println("      " + out.size() + " nodes");
        System.out.println("      " + edgesConsidered.size() + " edges considered");
        System.out.println("      " + distComparator.getComparisons() + " comparisons");
        System.out.println("      " + dist.get(goal));

        Solution solution = new Solution(out, edgesConsidered, null);

        return solution;
    }





    
    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

        Vertex a = new Vertex(56.0440049,9.9025227);
        Vertex b = new Vertex(56.1814955,10.2042923);

        a = GraphUtils.pickRandomVertex(graph);
        b = GraphUtils.pickRandomVertex(graph);

        LandmarkSelector landmarkSelector = new LandmarkSelector(graph, 16, 1);

        ALT d = new ALT(graph, landmarkSelector);
        Solution solution = d.shortestPath(a, b);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
        vis.drawPoint(landmarkSelector.getAllLandmarks(), landmarkSelector.getActiveLandmarks());
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize("ALT");

    }

}




package pathfinding.standard;

import pathfinding.framework.*;
import utility.*;
import graph.*;
import java.util.*;

public class Astar implements PathfindingAlgo {
    private final double INF_DIST = Double.MAX_VALUE;

    private Graph g;

    private Map<Vertex, Double> dist;
    private Map<Vertex, Vertex> pred; // S in the algo is pred.keySet()

    // For visual
    private List<Edge> edgesConsidered;

    public Astar(Graph g) {
        this.g = g;
    }

    public Solution shortestPath(Vertex start, Vertex goal){

        dist = new HashMap<>();
        pred = new HashMap<>();

        //Purely for visualising
        edgesConsidered = new ArrayList<>();

        dist.put(start, 0.0);

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        pq.add(new Pair(start, heuristic(start, goal)));


        while (pq.size() > 0) {
            Pair min = pq.poll(); // Should retrieve the lowest Fscore

            if (min.v.equals(goal)) {
                System.out.println("  --> Finished early");
                break;
            }

            g.getNeighboursOf(min.v).forEach(n -> {
                double tent_gScore = dist.getOrDefault(min.v, INF_DIST) + n.distance;
                double potentialNewFscore = tent_gScore + heuristic(n.v, goal);

                edgesConsidered.add(new Edge(min.v, n.v, potentialNewFscore));
                if (tent_gScore < dist.getOrDefault(n.v, INF_DIST)) {
                    pred.put(n.v, min.v);
                    dist.put(n.v, tent_gScore);

                    pq.add(new Pair(n.v, potentialNewFscore));
                }
            });

        }

        // Get out the shortest path
        System.out.println("    --> backtracking solution");

        if (pred.get(goal) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered);
        }
        List<Vertex> out = new ArrayList<>();

        Vertex temp = goal;
        while (! start.equals(temp)) {
            out.add(temp);
            temp = pred.get(temp);
        }
        out.add(start);
        System.out.println("      " + out.size() + " nodes");
        System.out.println("      " + edgesConsidered.size() + " edges considered");
        System.out.println("      " + comp.getComparisons() + " comparisons");
        System.out.println("      " + dist.getOrDefault(goal, INF_DIST));

        Solution solution = new Solution(out, edgesConsidered);

        System.out.println(out);


        return solution;

    }


    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("denmark-intersections.csv");

        Vertex a = new Vertex(56.0999867,9.582729);
        Vertex b = new Vertex(56.1636085,10.1177374);

        Astar d = new Astar(graph);
        Solution solution = d.shortestPath(Location.Viborgvej, Location.Randersvej);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.intersection);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize("A*");
    }

    private double heuristic(Vertex a, Vertex b) {
        return GraphUtils.haversineDist(a, b);
    }

}

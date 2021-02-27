package pathfinding.standard;

import pathfinding.framework.*;
import utility.*;
import graph.*;
import java.util.*;

public class Astar implements PathfindingAlgo {
    private final double INF_DIST = Double.MAX_VALUE;


    private Map<Vertex, Double> dist;
    private Map<Vertex, Double> fscore;

    private Map<Vertex, Vertex> pred; // S in the algo is pred.keySet()

    // For visual
    private List<Edge> edgesConsidered;

    public Solution shortestPath(Graph g, Vertex start, Vertex goal){

        dist = new HashMap<>();
        pred = new HashMap<>();
        fscore = new HashMap<>();

        //Purely for visualising
        edgesConsidered = new ArrayList<>();

        // TODO kan man skip det her og så bruge dist.getOrDefault(...)?
        g.getAllVertices().stream().forEach( v -> {dist.put(v, INF_DIST);
                                                   fscore.put(v, INF_DIST);} );
        dist.put(start, 0.0);
        fscore.put(start, heuristic(start, goal));
        DistComparator comp = new DistComparator();
        PriorityQueue<Vertex> pq = new PriorityQueue<>(comp);
        pq.add(start);


        while (pq.size() > 0) {
            Vertex min = pq.poll(); // Should retrieve the lowest Fscore

            if (min.equals(goal)) {
                System.out.println("  --> Finished early");
                break;
            }

            g.getNeighboursOf(min).forEach(n -> {
                double tent_gScore = dist.get(min) + n.distance;
                double potentialNewFscore = tent_gScore + heuristic(n.v, goal);

                edgesConsidered.add(new Edge(min, n.v, potentialNewFscore));
                if (tent_gScore < dist.get(n.v)) {
                    pred.put(n.v, min);
                    dist.put(n.v, tent_gScore);
                    fscore.put(n.v, potentialNewFscore);
                    //if (!pq.contains(n.v)){
                        pq.add(n.v);
                    //}
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
        System.out.println("        " + out.size() + " nodes");
        System.out.println("        " + comp.getComparisons() + " comparisons");

        Solution solution = new Solution(out, edgesConsidered);

        return solution;

    }


    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("denmark-intersections.csv");

        Astar d = new Astar();
        Solution solution = d.shortestPath(graph, Location.Lolland, Location.Thisted);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.Denmark);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize();
    }


    // TODO update what is compared on
    class DistComparator implements Comparator<Vertex> {
        
        public long comparisons = 0;

        @Override
        public int compare(Vertex a, Vertex b) {
            this.comparisons++;
            return Double.compare(fscore.getOrDefault(a, INF_DIST), fscore.getOrDefault(b, INF_DIST));
        }

        public long getComparisons() {
            return this.comparisons;
        }
    }

    private double heuristic(Vertex a, Vertex b) {
        return GraphUtils.haversineDist(a, b);
    }

}

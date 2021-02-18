package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;
import java.util.*;

public class DijkstraTraditional implements PathfindingAlgo {
    private final double INF_DIST = Double.MAX_VALUE;
 
    /**
     * Implemented based on the description in the book
     * 
     * Talks about directed graph, but does it even matter, it's never going to be better to travel the same edge twice?
     * I think at least
    */
    private Map<Vertex, Double> dist;
    private Map<Vertex, Vertex> pred; // S in the algo is pred.keySet()
    
    // For visual
    private List<Edge> edgesConsidered;

    public Solution shortestPath(Graph g, Vertex start, Vertex goal){
        System.out.println("--> Running \"traditional\" Dijkstra");
        return null;
    }

    private boolean relax(Vertex u, Neighbor n) {
        return false;
    }



    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

        // Vertex a = new Vertex(56.1634686,10.1722176); // Viborgvej
        Vertex a = new Vertex(56.1723636,9.5538336); // Silkeborg
        Vertex b = new Vertex(56.1828308,10.2037825); // O2/Randersvej

        PathfindingAlgo d = new DijkstraTraditional();
        Solution solution = d.shortestPath(graph, a, b);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize();
    }


    class DistComparator implements Comparator<Vertex> {
        
        public long comparisons = 0;

        @Override
        public int compare(Vertex a, Vertex b) {
            this.comparisons++;
            return Double.compare(dist.getOrDefault(a, INF_DIST), dist.getOrDefault(b, INF_DIST));
        }

        public long getComparisons() {
            return this.comparisons;
        }
    }

}

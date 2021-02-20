package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;
import java.util.*;

public class BidirectionalDijkstra implements PathfindingAlgo {
    private final double INF_DIST = Double.MAX_VALUE;

    //_f means forward algo, _b means backwards algo
    private Map<Vertex, Double> dist_f;
    private Map<Vertex, Vertex> pred_f; // S in the algo is pred.keySet()
    private Map<Vertex, Double> dist_b;
    private Map<Vertex, Vertex> pred_b; // S in the algo is pred.keySet()


    // For visual
    private List<Edge> edgesConsidered;
    


    @Override // TODO problems in taking 2 graphs as input :/
    public Solution shortestPath(Graph graph, Vertex start, Vertex goal) {

        dist_f = new HashMap<>();
        pred_f = new HashMap<>();
        dist_b = new HashMap<>();
        pred_b = new HashMap<>();

        //Purely for visualising
        edgesConsidered = new ArrayList<>();

        // Main source of inspiration:
        // https://www.ucl.ac.uk/~ucahmto/math/2020/05/30/bidirectional-dijkstra.html
        
        dist_f.put(start, 0.0);
        dist_b.put(goal, 0.0);


        // Main algo
        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq_f = new PriorityQueue<>(comp);
        pq_f.add(new Pair(start, 0));

        PriorityQueue<Pair> pq_b = new PriorityQueue<>(comp);
        pq_b.add(new Pair(goal, 0));

        while (pq_f.size() > 0 && pq_b.size() > 0) {
            Pair head_f = pq_f.poll();
            Pair head_b = pq_b.poll();

            



        }

        // TODO Auto-generated method stub
        return null;
    }


    private boolean relax(Vertex u, Neighbor n, Map<Vertex, Double> dist, Map<Vertex, Vertex> pred) {
        // for visualising all considered edges
        edgesConsidered.add(new Edge(u, n.v));

        if (dist.getOrDefault(n.v, INF_DIST) > dist.getOrDefault(u, INF_DIST) + n.distance) {
            dist.put(n.v, dist.get(u) + n.distance);
            pred.put(n.v, u);

            return true;
        }
        return false;
    }
    
    public static void main(String[] args) {
        // We need to be able to utilize the inverted graph, so for now we ignore space efficiency and just create 2 graphs
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv", false);
        Graph invertedGraph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv", true);

        // Vertex a = new Vertex(56.1634686,10.1722176); // Viborgvej
        Vertex b = new Vertex(56.1723636,9.5538336); // Silkeborg
        Vertex a = new Vertex(56.1828308,10.2037825); // O2/Randersvej

        BidirectionalDijkstra d = new BidirectionalDijkstra();
        Solution solution = d.shortestPath(graph, a, b);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize();
    }

    class DistComparator implements Comparator<Pair> {

        public long comparisons = 0;

        @Override
        public int compare(Pair p1, Pair p2) {
            this.comparisons++;
            return Double.compare(p1.dist, p2.dist);
        }

        public long getComparisons() {
            return this.comparisons;
        }
        
    }

    class Pair {
        public final Vertex v;
        public final double dist;

        public Pair(Vertex v, double dist) {
            this.v = v;
            this.dist = dist;
        }
    }

}

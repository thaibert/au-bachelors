package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;
import java.util.*;

public class Dijkstra implements PathfindingAlgo {
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
    private Map<Vertex, Vertex> edgesConsidered;

    public Solution shortestPath(Graph g, Vertex a, Vertex b){
        System.out.println("--> Running Dijkstra");
        //  Psudokode from the book
        //  Initialize-Single-Source(G, s) (s = source)
        //  S = Ø
        //  Q = G.V
        //  While Q != Ø
        //      u = Extract-Min(Q)    
        //      S = S U {u}
        //      for each vertex v in G.Adj[u]
        //          Relax(u,v,w)   

        dist = new HashMap<>();
        pred = new HashMap<>();
        
        //Purely for visualising
        edgesConsidered = new HashMap<>();

        g.getAllVertices().stream().forEach( v -> dist.put(v, INF_DIST) );
        dist.put(a, 0.0);


        // Main algo
        DistComparator comp = new DistComparator();
        PriorityQueue<Vertex> pq = new PriorityQueue<>(comp);
        pq.add(a);

        System.out.println("vertices: " + g.getAllVertices().size() + ",    pq: " + pq.size());

        int num = 0;
        while (pq.size() > 0) {
            num++;
            if (num % 1000 == 0) {
                System.out.println("  --> " + num + ",   pq size: " + pq.size());
            }

            Vertex u = pq.poll();

            if (u.equals(b)) {
                System.out.println("  --> Finished early");
                break;
            }

            g.getNeighboursOf(u).forEach(n -> {
                boolean relaxed = relax(u, n);
                if (relaxed) {
                    pq.add(n.v);
                }
            });
        }

        // Get out the shortest path
        System.out.println("    --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        Vertex temp = b;
        int i = 0;
        while (! a.equals(temp)) {
            out.add(temp);
            temp = pred.get(temp);
            i++;
        }
        out.add(a);
        System.out.println("        " + out.size() + " nodes");
        System.out.println("        " + comp.getComparisons() + " comparisons");

        Solution solution = new Solution(out, edgesConsidered);

        return solution;
    }

    private boolean relax(Vertex u, Neighbor n) {
        // for visualising all considered edges
        // TODO currently it only paint one edge going out of each node. I can't think of a way to do it, without a list of pairs
        // But i can't get pairs to work currently
        edgesConsidered.put(u, n.v);

        if (dist.get(n.v) > dist.get(u) + n.distance) {
            dist.put(n.v, dist.get(u) + n.distance);
            pred.put(n.v, u);

            return true;
        }
        return false;
    }



    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

        // Vertex a = new Vertex(56.1634686,10.1722176); // Viborgvej
        Vertex b = new Vertex(56.1723636,9.5538336); // Silkeborg
        Vertex a = new Vertex(56.1828308,10.2037825); // O2/Randersvej

        Dijkstra d = new Dijkstra();
        Solution solution = d.shortestPath(graph, a, b);

        GraphVisualiser vis = new GraphVisualiser(graph);
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

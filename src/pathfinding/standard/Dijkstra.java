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
    private List<Edge> edgesConsidered;

    public Solution shortestPath(Graph g, Vertex start, Vertex goal){
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
        edgesConsidered = new ArrayList<>();

        //g.getAllVertices().stream().forEach( v -> dist.put(v, INF_DIST) );
        dist.put(start, 0.0);


        // Main algo
        DistComparator comp = new DistComparator();
        PriorityQueue<Vertex> pq = new PriorityQueue<>(comp);
        pq.add(start);

        System.out.println("vertices: " + g.getAllVertices().size() + ",    pq: " + pq.size());

        int num = 0;
        while (pq.size() > 0) {
            num++;
            if (num % 1000 == 0) {
                System.out.println("    --> " + num + ",   pq size: " + pq.size());
            }

            Vertex head = pq.poll();

            if (head.equals(goal)) {
                System.out.println("  --> Finished early at " + num);
                break;
            }

            g.getNeighboursOf(head).forEach(n -> {
                boolean relaxed = relax(head, n);
                if (relaxed) {
                    pq.add(n.v);
                }
            });
        }

        // Get out the shortest path
        System.out.println("  --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        if (pred.get(goal) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered);
        }

        Vertex temp = goal;
        while (! start.equals(temp)) {
            out.add(temp);
            temp = pred.get(temp);
        }
        out.add(start);
        System.out.println("      " + out.size() + " nodes");
        System.out.println("      " + edgesConsidered.size() + " edges considered");
        System.out.println("      " + comp.getComparisons() + " comparisons");

        Solution solution = new Solution(out, edgesConsidered);

        return solution;
    }

    private boolean relax(Vertex u, Neighbor n) {
        // for visualising all considered edges
        // TODO currently it only paint one edge going out of each node. I can't think of a way to do it, without a list of pairs
        // But i can't get pairs to work currently
        edgesConsidered.add(new Edge(u, n.v));

        if (dist.getOrDefault(n.v, INF_DIST) > dist.getOrDefault(u, INF_DIST) + n.distance) {
            dist.put(n.v, dist.get(u) + n.distance);
            pred.put(n.v, u);

            return true;
        }
        return false;
    }



    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv", false);

        // Vertex a = new Vertex(56.1634686,10.1722176); // Viborgvej
        Vertex a = new Vertex(56.1723636,9.5538336); // Silkeborg
        Vertex b = new Vertex(56.1828308,10.2037825); // O2/Randersvej

        PathfindingAlgo d = new Dijkstra();
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

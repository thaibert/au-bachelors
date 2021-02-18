package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;
import java.util.*;

public class DijkstraTraditional implements PathfindingAlgo {
    private final double INF_DIST = Double.MAX_VALUE;

    public Solution shortestPath(Graph g, Vertex start, Vertex goal){
        System.out.println("--> Running \"traditional\" Dijkstra");
        //  Pseudocode from CLRS
        //  Initialize-Single-Source(G, s) (s = source)
        //  S = Ø
        //  Q = G.V
        //  While Q != Ø
        //      u = Extract-Min(Q)    
        //      S = S U {u}
        //      for each vertex v in G.Adj[u]
        //          Relax(u,v,w)   



        Map<Vertex, Double> bestDist = new HashMap<>();
        Map<Vertex, Vertex> predecessor = new HashMap<>(); 
        List<Edge> edgesConsidered = new ArrayList<>();

        Comparator<Pair> comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        pq.add(new Pair(start, 0));

        g.getAllVertices().stream()
            .map(v -> new Pair(v, INF_DIST))
            .forEach(pq::add);
        
        int iterations = 0;
        while (pq.size() > 0) {
            iterations++;

            if (iterations % 1000 == 0) {
                System.out.println("  " + iterations);
            }

            Pair head = pq.poll();

            g.getNeighboursOf(head.v)
                .forEach(n -> {
                    // RELAX
                    edgesConsidered.add(new Edge(head.v, n.v));

                    double maybeNewBestDistance = head.dist + n.distance;
                    double previousBestDistance = bestDist.getOrDefault(n.v, INF_DIST);

                    if (maybeNewBestDistance < previousBestDistance) {
                        bestDist.put(n.v, maybeNewBestDistance);
                        predecessor.put(n.v, head.v);

                        // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                        pq.add(new Pair(n.v, maybeNewBestDistance)); 
                    }
                });
        }



        // Get out the shortest path
        System.out.println("  --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        Vertex temp = goal;
        while (! start.equals(temp)) {
            out.add(temp);
            temp = predecessor.get(temp);
        }
        out.add(start);
        System.out.println("      " + out.size() + " nodes");
        System.out.println("      " + edgesConsidered.size() + " edges considered");

        Solution solution = new Solution(out, new ArrayList<>()); // edgesConsidered);

        return solution;
    }



    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

        // Vertex a = new Vertex(56.1634686,10.1722176); // Viborgvej
        Vertex b = new Vertex(56.1723636,9.5538336); // Silkeborg
        Vertex a = new Vertex(56.1828308,10.2037825); // O2/Randersvej

        PathfindingAlgo d = new DijkstraTraditional();
        Solution solution = d.shortestPath(graph, a, b);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize();
    }


    class DistComparator implements Comparator<Pair> {

        @Override
        public int compare(Pair p1, Pair p2) {
            return Double.compare(p1.dist, p2.dist);
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

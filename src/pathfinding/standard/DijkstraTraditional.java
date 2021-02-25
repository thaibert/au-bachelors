package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;
import java.util.*;
import utility.*;

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

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        pq.add(new Pair(start, 0));

        //g.getAllVertices().stream()
        //    .map(v -> new Pair(v, INF_DIST))
        //    .forEach(pq::add);
        
        int iterations = 0;
        while (pq.size() > 0) {
            iterations++;

            if (iterations % 1000 == 0) {
                System.out.println("    --> " + iterations + ",   pq size: " + pq.size());
            }

            Pair head = pq.poll();

            if (head.v.equals(goal)) {
                System.out.println("  --> Finished early at " + iterations);
                break;
            }

            g.getNeighboursOf(head.v)
                .forEach(n -> {
                    // RELAX

                    double maybeNewBestDistance = head.dist + n.distance;
                    double previousBestDistance = bestDist.getOrDefault(n.v, INF_DIST);

                    edgesConsidered.add(new Edge(head.v, n.v, maybeNewBestDistance));

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

        if (predecessor.get(goal) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered);
        }

        Vertex temp = goal;
        while (! start.equals(temp)) {
            out.add(temp);
            temp = predecessor.get(temp);
        }
        out.add(start);
        System.out.println("      " + out.size() + " nodes");
        System.out.println("      " + edgesConsidered.size() + " edges considered");
        System.out.println("      " + comp.getComparisons() + " comparisons");

        Solution solution = new Solution(out, edgesConsidered); // edgesConsidered);

        return solution;
    }



    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("denmark-intersections.csv");

        PathfindingAlgo d = new DijkstraTraditional();
        Solution solution = d.shortestPath(graph, Location.Lolland, Location.Thisted);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.Denmark);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize();
    }

}

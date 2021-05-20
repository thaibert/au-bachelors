package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;
import java.util.*;
import utility.*;

public class DijkstraTraditional implements PathfindingAlgo {
    private final double INF_DIST = Double.MAX_VALUE;

    private Graph g;

    public DijkstraTraditional(Graph g) {
        this.g = g;
    }

    public Solution shortestPath(Vertex start, Vertex goal){
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

        /* Invariants:
            All edges are non-negative
            Q = V - S at the start of each iteration of the while loop
            Each edge is extracted from Q and added to S exactly once 
        */

        Collection<Vertex> VminusS = new HashSet<>(g.getAllVertices()); 

        Map<Vertex, Double> bestDist = new HashMap<>();
        bestDist.put(start, 0.0);
        Map<Vertex, Vertex> predecessor = new HashMap<>(); 
        List<Edge> edgesConsidered = new ArrayList<>();

        Set<Vertex> closed = new HashSet<>();

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        Collection<Vertex> Q = new HashSet<>(g.getAllVertices()); // Initially all vertices are added
        g.getAllVertices().stream()
            .map(p -> new Pair(p, INF_DIST))
            .forEach(p -> {
                pq.add(p);
                assert(p.dist >= 0); // All weights are non-negative
            });
        pq.remove(new Pair(start, INF_DIST)); // Update start's weight
        pq.add(new Pair(start, 0));

        assert(pq.peek() != null && pq.peek().dist >= 0); // All weights are non-negative

        
        int iterations = 0;
        while (pq.size() > 0) {
            {
                // INVARIANT CHECK: Q = V - S
                // Q: vertices in the priority queue
                // V: set of all vertices in the graph
                // S: set of all vertices whose shortest path has been found

                // Q = V - S   equiv to    Q \subset V - S   &&   V - S \subset Q
                assert(Q.containsAll(VminusS)); // V-S \subset Q
                assert(VminusS.containsAll(Q)); // Q \subset V-S
            }

            iterations++;

            if (iterations % 1000 == 0) {
                System.out.println("    --> " + iterations + ",   pq size: " + pq.size());
            }

            Pair head = pq.poll();
            Q.remove(head.v);
            assert(head != null && head.dist >= 0); // All weights are non-negative

            assert(VminusS.contains(head.v)); // Each edge is only extracted once; v \in V-S ==> v \notin S
            VminusS.remove(head.v); // Put v in S ==> V-S loses v

            closed.add(head.v);

            if (head.v.equals(goal)) {
                System.out.println("  --> Finished early at " + iterations);
                break;
            }

            g.getNeighboursOf(head.v)
                .forEach(n -> {
                    // RELAX
                    if (closed.contains(n.v)){
                        return;
                    }

                    double maybeNewBestDistance = head.dist + n.distance;
                    double previousBestDistance = bestDist.getOrDefault(n.v, INF_DIST);

                    edgesConsidered.add(new Edge(head.v, n.v, maybeNewBestDistance));

                    if (maybeNewBestDistance < previousBestDistance) {
                        // Update v.d and v.pi
                        bestDist.put(n.v, maybeNewBestDistance);
                        predecessor.put(n.v, head.v);
                        
                        // Remove pair of (v, oldDist) and insert (v, newDist) instead
                        pq.remove(new Pair(n.v, previousBestDistance));

                        Pair newPair = new Pair(n.v, maybeNewBestDistance);
                        pq.add(newPair);
                        assert(newPair.dist >= 0); 
                    }
                });
        }



        // Get out the shortest path
        System.out.println("  --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        if (predecessor.get(goal) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered, null);
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
        System.out.println("      " + bestDist.get(goal) + " distance");

        Solution solution = new Solution(out, edgesConsidered, null);

        return solution;
    }



    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

        Vertex a = new Vertex(56.1336391,9.7235112);
        Vertex b = new Vertex(56.1906785,10.0880127);

        PathfindingAlgo d = new DijkstraTraditional(graph);
        Solution solution = d.shortestPath(a, b);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize("Dijkstra traditional");
    }

}

package pathfinding.standard;

import graph.*;
import graph.Vertex;
import pathfinding.framework.*;
import utility.*;

import java.util.*;

public class ALT implements PathfindingAlgo {
    private static final double INF_DIST = Double.MAX_VALUE;

    private Graph graph;

    private Map<Vertex, Map<Vertex, Double>> distanceToLandmark;
    private Map<Vertex, Map<Vertex, Double>> distanceFromLandmark;
    private Collection<Vertex> reachableLandmarks;

    private Map<Vertex, Double> dist;
    private Map<Vertex, Vertex> parent;

    // For visual
    private List<Edge> edgesConsidered;

    public ALT(Graph graph) {
        this.graph = graph;

        Graph ginv = GraphUtils.invertGraph(graph);

        List<Vertex> landmarks = landmark(graph, 5);
        // List<Vertex> landmarks = new ArrayList<>();
        // landmarks.add(GraphUtils.findNearestVertex(graph, 56.21684389259911, 9.517964491806737));
        // landmarks.add(new Vertex(56.0929669, 10.0084564));

        distanceToLandmark = new HashMap<>();
        distanceFromLandmark = new HashMap<>();

        landmarks.forEach( l -> {
            System.out.print(".");
            Map<Vertex, Double> normal = dijkstra(graph, l);
            Map<Vertex, Double> inv = dijkstra(ginv, l);

            distanceFromLandmark.put(l, normal);
            distanceToLandmark.put(l, inv);
        });
    }

    @Override
    public Solution shortestPath(Vertex start, Vertex goal) {

        reachableLandmarks = findReachableTo(start, goal);
        System.out.println(reachableLandmarks.size());
        if (reachableLandmarks.size() == 0) {
            // bailout early
            return new Solution(new ArrayList<>(), new ArrayList<>());
        }

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
                System.out.println("    --> " + iterations + ",   pq size: " + pq.size());
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
                        pq.add(new Pair(n.v, maybeNewBestDistance + pi_t(n.v, goal))); 
                    }
                }
            }
        }

        // Get out the shortest path
        System.out.println("  --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        if (parent.get(goal) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered);
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

        Solution solution = new Solution(out, edgesConsidered);

        return solution;
    }

    private double pi_t(Vertex curr, Vertex goal) {

        double max = -INF_DIST;
        for (Vertex l : reachableLandmarks) {
            Map<Vertex, Double> distTo = distanceToLandmark.get(l);
            Map<Vertex, Double> distFrom = distanceFromLandmark.get(l);

            if (! distTo.containsKey(curr)
             || ! distFrom.containsKey(curr)) {
                 // This node either cannot reach l, or cannot be reached by l.
                 // So skip l, since the calculations wouldn't make sense.
                continue;
             }

            // pi^l+ := dist(v, l) - dist(t, l)
            double dist_vl = distTo.get(curr);
            double dist_tl = distTo.get(goal);
            double pi_plus = dist_vl - dist_tl;

            // pi^l- := dist(l, t) - dist(l, v)
            double dist_lt = distFrom.get(goal);
            double dist_lv = distFrom.get(curr);
            double pi_minus = dist_lt - dist_lv;

            // System.out.println(dist_vl + "\n" + dist_tl + "\n" + dist_lt + "\n" + dist_lv + "\n\n");

            max = Math.max(max, Math.max(pi_plus, pi_minus));
        }
        return max;
    }

    private Collection<Vertex> findReachableTo(Vertex start, Vertex goal) {
        // Return all landmarks that can reach both start and goal.
        // If it can't reach one of them;
        //   - it either can't reach the other either, or
        //   - there is no path between start and goal.
        Collection<Vertex> reachable = new ArrayList<>();

        for (Vertex l : distanceToLandmark.keySet()) {
            if ( ! distanceToLandmark.get(l).containsKey(start)
            || ! distanceFromLandmark.get(l).containsKey(start) ) {
                System.out.println("Landmark " + l + " cannot reach start");
                continue; 
            }
            if ( ! distanceToLandmark.get(l).containsKey(goal)
            || ! distanceFromLandmark.get(l).containsKey(goal) ) {
                System.out.println("Landmark " + l + " cannot reach goal");
                continue;
            }
            reachable.add(l);
        }

        return reachable;
    }

    
    

    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("denmark-intersections.csv");

        Vertex a = new Vertex(56.1102309,10.2295427);
        Vertex b = new Vertex(56.0429021,10.2634393);

        PathfindingAlgo d = new ALT(graph);
        Solution solution = d.shortestPath(a, b);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize("ALT");

    }

    public static List<Vertex> landmark(Graph g, int k){
        List<Vertex> landmarks = new ArrayList<>();  

        for (int i = 0; i < k; i++) {
            landmarks.add(GraphUtils.pickRandomVertex(g));
        }

        return landmarks;
    }

    public static Map<Vertex, Double> dijkstra(Graph g, Vertex start){

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

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        pq.add(new Pair(start, 0));
        
        Map<Vertex, Double> shortest = new HashMap<>();

        while (pq.size() > 0) {

            Pair head = pq.poll();
            if (head.dist < shortest.getOrDefault(head.v, INF_DIST)) {
                shortest.put(head.v, head.dist);
            }


            g.getNeighboursOf(head.v)
                .forEach(n -> {
                    // RELAX

                    double maybeNewBestDistance = head.dist + n.distance;
                    double previousBestDistance = bestDist.getOrDefault(n.v, INF_DIST);

                    if (maybeNewBestDistance < previousBestDistance) {
                        bestDist.put(n.v, maybeNewBestDistance);

                        // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                        pq.add(new Pair(n.v, maybeNewBestDistance)); 
                    }
                });
        }
        return shortest;
    }

}

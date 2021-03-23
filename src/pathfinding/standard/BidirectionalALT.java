package pathfinding.standard;

import graph.*;
import graph.Vertex;
import pathfinding.framework.*;
import utility.*;

import java.util.*;

public class BidirectionalALT implements PathfindingAlgo{

    private static final double INF_DIST = Double.MAX_VALUE;

    private Graph graph;
    private Graph ginv; 

    private Map<Vertex, Map<Vertex, Double>> distanceToLandmark;
    private Map<Vertex, Map<Vertex, Double>> distanceFromLandmark;
    private Collection<Vertex> reachableLandmarks;

    private Map<Vertex, Double> dist_f;
    private Map<Vertex, Vertex> pred_f; // S in the algo is pred.keySet()
    private Map<Vertex, Double> dist_b;
    private Map<Vertex, Vertex> pred_b; // S in the algo is pred.keySet()

    private Set<Vertex> s_f;
    private Set<Vertex> s_b;


    private List<Edge> edgesConsidered;

    private Vertex bestVertex; 
    private double mu;

    public BidirectionalALT(Graph graph, int noLandmarks) {
        this.graph = graph;

        
        Graph ginv = GraphUtils.invertGraph(graph);
        this.ginv = ginv;

        List<Vertex> landmarks = landmark(graph, noLandmarks);
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
        mu = INF_DIST;
        bestVertex = null;

        // Bailout and optimization of landmarks.
        reachableLandmarks = findReachableTo(start, goal);
        System.out.println(reachableLandmarks.size());
        if (reachableLandmarks.size() == 0) {
            // bailout early
            return new Solution(new ArrayList<>(), new ArrayList<>());
        }

        dist_f = new HashMap<>();
        pred_f = new HashMap<>();
        dist_b = new HashMap<>();
        pred_b = new HashMap<>();
        s_f = new HashSet<>();
        s_b = new HashSet<>();

        edgesConsidered = new ArrayList<>();

        dist_f.put(start, 0.0);
        dist_b.put(goal, 0.0);

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq_f = new PriorityQueue<>(comp);
        PriorityQueue<Pair> pq_b = new PriorityQueue<>(comp);

        pq_f.add(new Pair(start, 0));
        pq_b.add(new Pair(goal, 0));

        int num = 0;
        while(pq_f.size() > 0 && pq_b.size() > 0){
            num++;
            if (num % 1000 == 0) {
                System.out.println("    --> " + num);
            }

            Pair min_f = pq_f.poll();
            Pair min_b = pq_b.poll();

            s_f.add(min_f.v); 
            s_b.add(min_b.v);

            //TODO is this early stop right???
            if (dist_f.get(min_f.v) + dist_b.get(min_b.v) >= mu*2) {
                System.out.println("Entered exit");
                break;
            }

            for (Neighbor n : graph.getNeighboursOf(min_f.v)) {
                // RELAX
                double maybeNewBestDistance = dist_f.get(min_f.v) + n.distance; // dist(s,v) + len(v,u)
                double previousBestDistance = dist_f.getOrDefault(n.v, INF_DIST); // dist(s,u)

                edgesConsidered.add(new Edge(min_f.v, n.v, maybeNewBestDistance));

                if (maybeNewBestDistance < previousBestDistance) {
                    dist_f.put(n.v, maybeNewBestDistance);
                    pred_f.put(n.v, min_f.v);

                    if (! s_f.contains(n.v)) {
                        pq_f.add(new Pair(n.v, maybeNewBestDistance + pi_t(n.v, goal))); 
                    }
                }

                if (s_b.contains(n.v) && maybeNewBestDistance + dist_b.get(n.v) < mu) {
                    mu = maybeNewBestDistance + dist_b.get(n.v);
                    bestVertex = n.v;
                }
            }

            for (Neighbor n : ginv.getNeighboursOf(min_b.v)) {
                // RELAX
                double maybeNewBestDistance = dist_b.get(min_b.v) + n.distance; // dist(s,v) + len(v,u)
                double previousBestDistance = dist_b.getOrDefault(n.v, INF_DIST); // dist(s,u)

                edgesConsidered.add(new Edge(min_b.v, n.v, maybeNewBestDistance));

                if (maybeNewBestDistance < previousBestDistance) {
                    dist_b.put(n.v, maybeNewBestDistance);
                    pred_b.put(n.v, min_b.v);

                    if (! s_b.contains(n.v)) {
                        pq_b.add(new Pair(n.v, maybeNewBestDistance + pi_t(n.v, goal))); 
                    }
                }

                if (s_f.contains(n.v) && maybeNewBestDistance + dist_f.get(n.v) < mu) {
                    mu = maybeNewBestDistance + dist_f.get(n.v);
                    bestVertex = n.v;
                }
            }
        }


    

        // Get out the shortest path
        System.out.println("  --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        /* TODO something that checks if we actually found something */
        if (pred_f.get(bestVertex) == null && pred_b.get(bestVertex) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered);
        }

        Vertex temp = bestVertex;
        while (! start.equals(temp)) {
            out.add(temp);
            temp = pred_f.get(temp);
        }

        List<Vertex> out2 = new ArrayList<>();
        temp = bestVertex;
        while (! goal.equals(temp)) {
            temp = pred_b.get(temp);
            out2.add(temp);
        }

        out.add(start);
        Collections.reverse(out2);
        out2.addAll(out);
        

        System.out.println("      " + out.size() + " nodes");
        System.out.println("      " + out2.size() + " nodes");

        System.out.println("      " + edgesConsidered.size() + " edges considered");
        System.out.println("      " + comp.getComparisons() + " comparisons");
        System.out.println("      " + mu + " distance");

        Solution solution = new Solution(out2, edgesConsidered);

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

    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

        Vertex a = new Vertex(56.1378248, 10.1709604);
        Vertex b = new Vertex(56.1899393, 10.1083791);

        PathfindingAlgo d = new BidirectionalALT(graph, 5);
        Solution solution = d.shortestPath(b, a);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize("ALT");

    }
    
}

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

    PriorityQueue<Pair> pq_f;
    PriorityQueue<Pair> pq_b;

    private Map<Vertex, Double> dist_f;
    private Map<Vertex, Double> dist_b;
    private Map<Vertex, Vertex> pred_f; 
    private Map<Vertex, Vertex> pred_b; 

    private Set<Vertex> closed;

    private double bestPathLength;
    private double fA;
    private double fB;
    private Vertex touchNode;

    private Vertex start;
    private Vertex goal;

    private List<Edge> edgesConsidered;

    private List<Map<Vertex, Map<Vertex, Double>>> landmarks;


    public BidirectionalALT(Graph graph, int landmarkSelectionType, int noLandmarks) {
        this.graph = graph;

        
        Graph ginv = GraphUtils.invertGraph(graph);
        this.ginv = ginv;

        // List<Vertex> landmarks = new ArrayList<>();
        // landmarks.add(GraphUtils.findNearestVertex(graph, 56.21684389259911, 9.517964491806737));
        // landmarks.add(new Vertex(56.0929669, 10.0084564));

        // in the list is two maps, to and from
        if (landmarkSelectionType == 0){
            landmarks = GraphUtils.randomLandmarks(graph, noLandmarks);
        } else if (landmarkSelectionType == 1){
            landmarks = GraphUtils.farthestLandmarks(graph, noLandmarks);
        } else {
            System.out.println("Please provide a viable integer for selecting landmarks");
            landmarks = new ArrayList<>();
        }

        distanceToLandmark = landmarks.get(0);
        distanceFromLandmark = landmarks.get(1);
    }


    @Override
    public Solution shortestPath(Vertex start, Vertex goal) {
        reachableLandmarks = findReachableTo(start, goal);
        System.out.println(reachableLandmarks.size());
        if (reachableLandmarks.size() == 0) {
            // bailout early
            return new Solution(new ArrayList<>(), new ArrayList<>(), null);
        }


        // TODO visuellisering
        this.start = start;
        this.goal = goal;

        bestPathLength = INF_DIST;
        

        // SETUP
        edgesConsidered = new ArrayList<>();

        closed = new HashSet<>();
        
        dist_f = new HashMap<>();
        pred_f = new HashMap<>();
        dist_b = new HashMap<>();
        pred_b = new HashMap<>();

        DistComparator comp = new DistComparator();
        pq_f = new PriorityQueue<>(comp);
        pq_b = new PriorityQueue<>(comp);

        fB = fA = hf(start, goal);

        dist_f.put(start, 0.0);
        dist_b.put(goal, 0.0);
        pq_f.add(new Pair(start, 0));
        pq_b.add(new Pair(goal, 0));

        // ALGO
        while (pq_f.size() > 0 && pq_b.size() > 0) {
            //try{
            //    Thread.sleep(4);
            //} catch(Exception e){
            //    e.printStackTrace();
            //}
            if(pq_f.size() < pq_b.size()){
                expandForwad();
            }else{
                expandBackward();
            }
        } 


        // FINDING BEST PATH
        // TODO take out in method?
        System.out.println("  --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        /* TODO something that checks if we actually found something */
        if (pred_f.get(touchNode) == null && pred_b.get(touchNode) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered, null);
        }

        Vertex temp = touchNode;
        while (! start.equals(temp)) {
            out.add(temp);
            temp = pred_f.get(temp);
        }

        List<Vertex> out2 = new ArrayList<>();
        temp = touchNode;
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
        System.out.println("      " + bestPathLength + " distance");

        Solution solution = new Solution(out2, edgesConsidered, touchNode);

        return solution;
    }

    public void expandForwad(){
        Pair currentPair = pq_f.poll();

        if (closed.contains(currentPair.v)){
            return;
        }

        closed.add(currentPair.v);
        double dist = dist_f.getOrDefault(currentPair.v, INF_DIST);
        if(dist + hf(currentPair.v, goal) >= bestPathLength 
        || dist + fB - hf(start, currentPair.v) >= bestPathLength){
            // Reject node 
        } else {
            // Stabilize
            graph.getNeighboursOf(currentPair.v).forEach(n -> {
                if (closed.contains(n.v)){
                    return; // TODO possibly fucking everything up, it should be continue, but that is not allowed
                }

                double tentDist = dist + n.distance;

                // For counting amount of edges considered
                edgesConsidered.add(new Edge(currentPair.v, n.v, tentDist));
                
                if (dist_f.getOrDefault(n.v, INF_DIST) > tentDist) {
                    dist_f.put(n.v, tentDist);
                    pred_f.put(n.v, currentPair.v);
                    pq_f.add(new Pair(n.v, tentDist + hf(n.v, goal)));

                // Checking if we found new best
                if (dist_b.containsKey(n.v)) {
                    double pathLength = tentDist + dist_b.get(n.v);
                    if (bestPathLength > pathLength) {
                        bestPathLength = pathLength;
                        touchNode = n.v;
                    }
                }
                }



            });
        }

        if (!pq_f.isEmpty()) {
            fA = pq_f.peek().dist;
        }

    }

    public void expandBackward(){
        Pair currentPair = pq_b.poll();

        if (closed.contains(currentPair.v)){
            return;
        }

        closed.add(currentPair.v);
        double dist = dist_b.getOrDefault(currentPair.v, INF_DIST);
        if (dist + hf(start, currentPair.v) >= bestPathLength
        || dist + fA - hf(currentPair.v, goal) >= bestPathLength){
            // Reject
        } else {
            ginv.getNeighboursOf(currentPair.v).forEach(n -> {
                if (closed.contains(n.v)){
                    return; // TODO this might fuck shit up, should be continue but it can't be
                }

                double tentDist = dist + n.distance;
                
                // For counting amount of edges considered
                edgesConsidered.add(new Edge(currentPair.v, n.v, tentDist));

                if (dist_b.getOrDefault(n.v, INF_DIST) > tentDist){
                    dist_b.put(n.v, tentDist);
                    pred_b.put(n.v, currentPair.v);
                    pq_b.add(new Pair(n.v, tentDist + hf(start, n.v)));

                    //Checking if we found new best
                    if (dist_f.containsKey(n.v)){
                        double pathLength = tentDist + dist_f.get(n.v);
                        if (pathLength < bestPathLength){
                            bestPathLength = pathLength;
                            touchNode = n.v;
                        }
                    }
                }
            });
        }

        if (!pq_b.isEmpty()) {
            fB = pq_b.peek().dist;
        }
    }

    public double hf(Vertex v, Vertex to){
        double temp = pi_t(v, to);
        if (temp < 0) {
            System.out.println("Front:" +temp); 
        }
        return temp;
    }

    public double hb(Vertex v, Vertex to){
        double temp = pi_t(v, to);
        //System.out.println("Back:" + temp); 
        return temp;
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
            if (! distTo.containsKey(goal)
             || ! distFrom.containsKey(goal)){
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

            //System.out.println(dist_vl + "\n" + dist_tl + "\n" + dist_lt + "\n" + dist_lv + "\n\n");

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

    public Set<Vertex> getLandmarks(){
        return landmarks.get(0).keySet();
    }

    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

    
        Vertex a = new Vertex(56.2095925,10.0379637); 
        Vertex b = new Vertex(56.1371326,10.1842766); 

        BidirectionalALT d = new BidirectionalALT(graph, 1, 8);
        Solution solution = d.shortestPath(a, b);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
        vis.drawPath(solution.getShortestPath());
        vis.drawPoint(d.getLandmarks());
        vis.drawVisited(solution.getVisited());
        vis.drawMeetingNode(solution.getMeetingNode());

        vis.visualize("Bidirec ALT");

    }
    
}

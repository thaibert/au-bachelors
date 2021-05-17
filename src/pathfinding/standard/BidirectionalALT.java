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

    private LandmarkSelector landmarkSelector;

    PriorityQueue<Pair> pq_f;
    PriorityQueue<Pair> pq_b;

    private Map<Vertex, Double> dist_f;
    private Map<Vertex, Double> dist_b;
    private Map<Vertex, Vertex> pred_f; 
    private Map<Vertex, Vertex> pred_b; 

    private Set<Vertex> closed;

    private double bestPathLength;
    private double distToHeadOfForwards;
    private double distToHeadOfBackwards;
    private Vertex touchNode;

    private Vertex start;
    private Vertex goal;

    private List<Edge> edgesConsidered;

    private List<Map<Vertex, Map<Vertex, Double>>> landmarks;

    private double originalPi;
    private int landmarkCheckpoint = 0; // ranges from 0-10
    private int iterationsSinceLastLandmarkUpdate = 0;
    private boolean landmarksUpdated = false;
    private Vertex landmarkUpdateVertex = null;



    public BidirectionalALT(Graph graph, LandmarkSelector landmarkSelector) {
        this.graph = graph;
        this.landmarkSelector = landmarkSelector;

        
        Graph ginv = GraphUtils.invertGraph(graph);
        this.ginv = ginv;

        // List<Vertex> landmarks = new ArrayList<>();
        // landmarks.add(GraphUtils.findNearestVertex(graph, 56.21684389259911, 9.517964491806737));
        // landmarks.add(new Vertex(56.0929669, 10.0084564));
    }


    @Override
    public Solution shortestPath(Vertex start, Vertex goal) {
        landmarkSelector.updateLandmarks(start, goal, 2);
        originalPi = hf(start, goal);

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

        distToHeadOfBackwards = distToHeadOfForwards = hf(start, goal);

        dist_f.put(start, 0.0);
        dist_b.put(goal, 0.0);
        pq_f.add(new Pair(start, 0));
        pq_b.add(new Pair(goal, 0));

        int iterations = 0;
        // ALGO
        while (pq_f.size() > 0 && pq_b.size() > 0) {
            
            iterations++;
            iterationsSinceLastLandmarkUpdate++;

            if(pq_f.size() < pq_b.size()){
                expandForwad();
            }else{
                expandBackward();
            }

            if (landmarksUpdated) {
                landmarksUpdated = false;
                System.out.printf("Updated landmarks #%d @ iteration %d", landmarkCheckpoint, iterations);

                boolean addedLandmark = landmarkSelector.updateLandmarks(landmarkUpdateVertex, goal, 1);

                if (addedLandmark) {
                    // Updates both priority ques
                    System.out.println(" :)");
                    PriorityQueue<Pair> newPQ_f = new PriorityQueue<>(comp);
                
                    Iterator<Pair> it_f = pq_f.iterator();
                    Set<Vertex> alreadyAdded_f = new HashSet<>();
                    while (it_f.hasNext()) {
                        Pair next = it_f.next();
                        if (alreadyAdded_f.contains(next.v)) {
                            continue;
                        }
                        Vertex v = next.v;
                        double d = dist_f.get(v) + landmarkSelector.pi(v, goal);

                        newPQ_f.add(new Pair(v, d));
                    }
                    pq_f = newPQ_f;

                    PriorityQueue<Pair> newPQ_b = new PriorityQueue<>(comp);
                
                    Iterator<Pair> it_b = pq_b.iterator();
                    Set<Vertex> alreadyAdded_b = new HashSet<>();
                    while (it_b.hasNext()) {
                        Pair next = it_b.next();
                        if (alreadyAdded_b.contains(next.v)) {
                            continue;
                        }
                        Vertex v = next.v;
                        double d = dist_b.get(v) + landmarkSelector.pi(v, goal);

                        newPQ_b.add(new Pair(v, d));
                    }
                    pq_b = newPQ_b;
                } else {
                    System.out.println(" :("); // TODO better logging
                }

                
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

        // Reset landmarks, so they don't carry over if multiple queries are run in series.
        landmarkSelector.resetLandmarks();

        return solution;
    }

    public void expandForwad(){
        Pair currentPair = pq_f.poll();

        if (originalPi == 0 && iterationsSinceLastLandmarkUpdate >= 100) {
            // If the first pi was 0, no landmarks could reach start.
           // So try again after at least 100 opened edges. Maybe we can see the landmarks now!
           iterationsSinceLastLandmarkUpdate = 0;
          
           Vertex curr = currentPair.v;
           landmarkSelector.updateLandmarks(curr, goal, 2);
           originalPi = landmarkSelector.pi(curr, goal);
       }

        if (closed.contains(currentPair.v)){
            return;
        }

        closed.add(currentPair.v);
        double dist = dist_f.getOrDefault(currentPair.v, INF_DIST);
        if(dist + hf(currentPair.v, goal) >= bestPathLength 
        || dist + distToHeadOfBackwards - hf(start, currentPair.v) >= bestPathLength){
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

                    double pi = hf(n.v, goal);


                    // b(10−i)/10,    b is original lower bound from s->t, i is checkpoint
                    boolean tenPercentMore = pi < originalPi * (10 - landmarkCheckpoint) / 10;
                    boolean enoughIterations = iterationsSinceLastLandmarkUpdate > 100;

                    if (tenPercentMore && enoughIterations) {
                        landmarksUpdated = true;
                        landmarkCheckpoint++;
                        iterationsSinceLastLandmarkUpdate = 0;
                        landmarkUpdateVertex = currentPair.v; // TODO should this be n.v?
                    }

                    pq_f.add(new Pair(n.v, tentDist + pi));

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
            distToHeadOfForwards = pq_f.peek().dist;
        }

    }

    public void expandBackward(){
        Pair currentPair = pq_b.poll();

        if (originalPi == 0 && iterationsSinceLastLandmarkUpdate >= 100) {
            // If the first pi was 0, no landmarks could reach start.
           // So try again after at least 100 opened edges. Maybe we can see the landmarks now!
           iterationsSinceLastLandmarkUpdate = 0;
          
           Vertex curr = currentPair.v;
           landmarkSelector.updateLandmarks(curr, goal, 2);
           originalPi = landmarkSelector.pi(curr, goal);
       }

        if (closed.contains(currentPair.v)){
            return;
        }

        closed.add(currentPair.v);
        double dist = dist_b.getOrDefault(currentPair.v, INF_DIST);
        if (dist + hf(start, currentPair.v) >= bestPathLength
        || dist + distToHeadOfForwards - hf(currentPair.v, goal) >= bestPathLength){
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

                    double pi = hf(start, n.v);
                    
                    // b(10−i)/10,    b is original lower bound from s->t, i is checkpoint
                    boolean tenPercentMore = pi < originalPi * (10 - landmarkCheckpoint) / 10;
                    boolean enoughIterations = iterationsSinceLastLandmarkUpdate > 100;

                    if (tenPercentMore && enoughIterations) {
                        landmarksUpdated = true;
                        landmarkCheckpoint++;
                        iterationsSinceLastLandmarkUpdate = 0;
                        landmarkUpdateVertex = currentPair.v; // TODO should this be n.v?
                    }


                    pq_b.add(new Pair(n.v, tentDist + pi));

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
            distToHeadOfBackwards = pq_b.peek().dist;
        }
    }



    public double hf(Vertex v, Vertex to){
        double temp = landmarkSelector.pi(v, to);
        if (temp < 0) {
            System.out.println("Front:" +temp); 
        }
        return temp;
    }

    
    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

    
        Vertex a = new Vertex(56.2095925,10.0379637); 
        Vertex b = new Vertex(56.1371326,10.1842766); 

        LandmarkSelector ls = new LandmarkSelector(graph, 16, 1); 

        BidirectionalALT d = new BidirectionalALT(graph, ls);
        Solution solution = d.shortestPath(Location.Silkeborg, Location.Randersvej);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
        vis.drawPath(solution.getShortestPath());
        vis.drawPoint(ls.getAllLandmarks(), ls.getActiveLandmarks());
        vis.drawVisited(solution.getVisited());
        vis.drawMeetingNode(solution.getMeetingNode());

        vis.visualize("Bidirec ALT");

    }
    
}

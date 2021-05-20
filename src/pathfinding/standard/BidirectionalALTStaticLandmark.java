package pathfinding.standard;

import graph.*;
import graph.Vertex;
import pathfinding.framework.*;
import utility.*;

import java.util.*;

public class BidirectionalALTStaticLandmark implements PathfindingAlgo{

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


    public BidirectionalALTStaticLandmark(Graph graph, LandmarkSelector landmarkSelector) {
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
        landmarkSelector.setAllLandmarks();

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

        // ALGO
        while (pq_f.size() > 0 || pq_b.size() > 0) {
            //try{
            //    Thread.sleep(4);
            //} catch(Exception e){
            //    e.printStackTrace();
            //}
            if((pq_f.size() < pq_b.size() && pq_f.size() > 0) || pq_b.size() == 0){
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
        if (touchNode == null || (pred_f.get(touchNode) == null && pred_b.get(touchNode) == null)) {
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
        || dist + distToHeadOfBackwards - hf(start, currentPair.v) >= bestPathLength){
            // Reject node 
        } else {
            // Stabilize
            graph.getNeighboursOf(currentPair.v).forEach(n -> {
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
                        if (pathLength < bestPathLength) {
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
         distToHeadOfBackwards = pq_b.peek().dist;
        }
    }

    public double hf(Vertex v, Vertex to){
        double temp = landmarkSelector.pi(v, to);
        if (temp < 0) {
            //System.out.println("Front:" +temp); 
        }
        return temp;
    }

    
    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

    
        Vertex a = new Vertex(56.1702261,10.1700643); 
        Vertex b = new Vertex(56.1728893,10.1981565); 


        LandmarkSelector ls = new LandmarkSelector(graph, 16, 1); 

        PathfindingAlgo d = new BidirectionalALTStaticLandmark(graph, ls);
        Solution solution = d.shortestPath(a, b);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
        vis.drawPath(solution.getShortestPath());
        vis.drawPoint(ls.getAllLandmarks(), ls.getActiveLandmarks());
        vis.drawVisited(solution.getVisited());
        vis.drawMeetingNode(solution.getMeetingNode());

        vis.visualize("Bidirec ALT Static");
        

    }
    
}

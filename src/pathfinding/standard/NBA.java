package pathfinding.standard;

import pathfinding.framework.*;
import utility.*;
import graph.*;
import java.util.*;

public class NBA implements PathfindingAlgo{
    // BASED ON 
    // https://codereview.stackexchange.com/questions/144376/nba-very-efficient-bidirectional-heuristic-search-algorithm-in-java-follow-u
    // AND 
    // https://www.researchgate.net/publication/46434387_Yet_another_bidirectional_algorithm_for_shortest_paths

    private final double INF_DIST = Double.MAX_VALUE;

    Graph g;
    Graph ginv;

    public NBA (Graph g){
        this.g = g;
        this.ginv = GraphUtils.invertGraph(g);
    }

    PriorityQueue<Pair> pq_f;
    PriorityQueue<Pair> pq_b;

    private Map<Vertex, Double> dist_f;
    private Map<Vertex, Double> dist_b;
    private Map<Vertex, Vertex> pred_f; 
    private Map<Vertex, Vertex> pred_b; 

    private Set<Vertex> closed;

    private double bestPathLength;
    private double distToHeadOfForward;
    private double distToHeadOfBackward;
    private Vertex meetingNode;

    private Vertex start;
    private Vertex goal;

    private List<Edge> edgesConsidered;



    @Override
    public Solution shortestPath(Vertex start, Vertex goal) {
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

        distToHeadOfBackward = distToHeadOfForward = hf(start, goal);

        dist_f.put(start, 0.0);
        dist_b.put(goal, 0.0);
        pq_f.add(new Pair(start, 0));
        pq_b.add(new Pair(goal, 0));

        // ALGO
        while (pq_f.size() > 0 || pq_b.size() > 0) {
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
        if (pred_f.get(meetingNode) == null && pred_b.get(meetingNode) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered, null);
        }

        Vertex temp = meetingNode;
        while (! start.equals(temp)) {
            out.add(temp);
            temp = pred_f.get(temp);
        }

        List<Vertex> out2 = new ArrayList<>();
        temp = meetingNode;
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

        Solution solution = new Solution(out2, edgesConsidered, meetingNode);

        return solution;
    }

    public void expandForwad(){
        Pair head = pq_f.poll();

        if (closed.contains(head.v)){
            return;
        }

        closed.add(head.v);
        double dist = dist_f.getOrDefault(head.v, INF_DIST);
        if(dist + hf(head.v, goal) >= bestPathLength 
        || dist + distToHeadOfBackward - hf(head.v, start) >= bestPathLength){
            // Reject node 
        } else {
            // Stabilize
            g.getNeighboursOf(head.v).forEach(n -> {
                if (closed.contains(n.v)){
                    return;
                }
                double tentDist = dist + n.distance;

                // For counting amount of edges considered
                edgesConsidered.add(new Edge(head.v, n.v, tentDist));
                
                if (dist_f.getOrDefault(n.v, INF_DIST) > tentDist) {
                    dist_f.put(n.v, tentDist);
                    pred_f.put(n.v, head.v);
                    pq_f.add(new Pair(n.v, tentDist + hf(n.v, goal)));

                    // Checking if we found new best
                    if (dist_b.containsKey(n.v)) {
                        double pathLength = tentDist + dist_b.get(n.v);
                        if (bestPathLength > pathLength) {
                            bestPathLength = pathLength;
                            meetingNode = n.v;
                        }
                    }
                }



            });
        }

        if (!pq_f.isEmpty()) {
            distToHeadOfForward = pq_f.peek().dist;
        }

    }

    public void expandBackward(){
        Pair head = pq_b.poll();

        if (closed.contains(head.v)){
            return;
        }

        closed.add(head.v);
        double dist = dist_b.getOrDefault(head.v, INF_DIST);
        if (dist + hb(head.v, start) >= bestPathLength
        || dist + distToHeadOfForward - hb(head.v, goal) >= bestPathLength){
            // Reject
        } else {
            ginv.getNeighboursOf(head.v).forEach(n -> {
                if (closed.contains(n.v)){
                    return;
                }
                double tentDist = dist + n.distance;
                
                // For counting amount of edges considered
                edgesConsidered.add(new Edge(head.v, n.v, tentDist));

                if (dist_b.getOrDefault(n.v, INF_DIST) > tentDist){
                    dist_b.put(n.v, tentDist);
                    pred_b.put(n.v, head.v);
                    pq_b.add(new Pair(n.v, tentDist + hb(n.v, start)));

                    //Checking if we found new best
                    if (dist_f.containsKey(n.v)){
                        double pathLength = tentDist + dist_f.get(n.v);
                        if (pathLength < bestPathLength){
                            bestPathLength = pathLength;
                            meetingNode = n.v;
                        }
                    }
                }
            });
        }

        if (!pq_b.isEmpty()) {
            distToHeadOfBackward = pq_b.peek().dist;
        }
    }

    // TODO Think 1 is enough, but i'm honestly unsure if this one is consistent as they mention it should be in the text on it.
    public double hf(Vertex v, Vertex to){
        // TODO
        return GraphUtils.haversineDist(v, to);
    }

    public double hb(Vertex v, Vertex to){
        //TODO
        return GraphUtils.haversineDist(v, to);
    }



    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("denmark-intersections.csv");

        //Vertex a = new Vertex(56.1336391,9.7235112);
        //Vertex b = new Vertex(56.1906785,10.0880127);

        //56.0337, 56.2794, 9.4807, 10.259

        Vertex a = GraphUtils.findNearestVertex(graph, 56.0337, 9.4807);
        Vertex b = GraphUtils.findNearestVertex(graph, 56.2794, 10.259);
        PathfindingAlgo d = new NBA(graph);
        Solution solution = d.shortestPath(Location.CPH, Location.Skagen);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.Denmark);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.drawMeetingNode(solution.getMeetingNode());
        vis.visualize("A* bidirectional");
    }

}
    


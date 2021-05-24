package pathfinding.standard;

import graph.*;
import graph.Vertex;
import pathfinding.framework.*;
import utility.*;

import java.util.*;

public class ALT implements PathfindingAlgo {
    private static final double INF_DIST = Double.MAX_VALUE;

    private Graph graph;

    LandmarkSelector landmarkSelector;

    private Map<Vertex, Double> dist;
    private Map<Vertex, Vertex> parent;

    // For visual
    private List<Edge> edgesConsidered;

    public ALT(Graph graph, LandmarkSelector landmarkSelector) {
        this.graph = graph;

        // List<Vertex> landmarks = new ArrayList<>();
        // landmarks.add(GraphUtils.findNearestVertex(graph, 56.21684389259911, 9.517964491806737));
        // landmarks.add(new Vertex(56.0929669, 10.0084564));

        this.landmarkSelector = landmarkSelector;


    }

    @Override
    public Solution shortestPath(Vertex start, Vertex goal) {
        // Reset landmarks, so they don't carry over if multiple queries are run in series.
        landmarkSelector.resetLandmarks();

        // Now find the two best ones
        landmarkSelector.updateLandmarks(start, goal, 2);

        double originalPi = landmarkSelector.pi(start, goal);
        int landmarkCheckpoint = 0; // ranges from 0-10


        Map<Vertex, Double> dist = new HashMap<>();
        Map<Vertex, Vertex> parent = new HashMap<>(); 
        List<Edge> edgesConsidered = new ArrayList<>();
        
        dist.put(start, 0.0);

        DistComparator distComparator = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(distComparator);

        pq.add(new Pair(start, 0));

        Set<Vertex> settled = new HashSet<>();

        int iterations = 0;
        int iterationsSinceLastLandmarkUpdate = 0;
        while(pq.size() > 0){
            iterations++;
            iterationsSinceLastLandmarkUpdate++;

            Pair head = pq.poll();

            if (originalPi == 0 && iterationsSinceLastLandmarkUpdate >= 100) {
                 // If the first pi was 0, no landmarks could reach start.
                // So try again after at least 100 opened edges. Maybe we can see the landmarks now!
                iterationsSinceLastLandmarkUpdate = 0;
               
                Vertex curr = head.v;
                landmarkSelector.updateLandmarks(curr, goal, 2);
                originalPi = landmarkSelector.pi(curr, goal);
            }

            settled.add(head.v);

            if (iterations % 1000 == 0) {
                //System.out.println("    --> " + iterations + ",   pq size: " + pq.size());
            }

            if (head.v.equals(goal)) {
                System.out.println("  --> Finished early at " + iterations);
                break;
            }
            
            boolean landmarksUpdated = false;

            for (Neighbor n : graph.getNeighboursOf(head.v)) {

                if (settled.contains(n.v)){
                    continue;
                }
                // RELAX
                double maybeNewBestDistance = dist.get(head.v) + n.distance; // dist(s,v) + len(v,u)
                double previousBestDistance = dist.getOrDefault(n.v, INF_DIST); // dist(s,u)

                edgesConsidered.add(new Edge(head.v, n.v, maybeNewBestDistance));


                if (maybeNewBestDistance < previousBestDistance) {
                    dist.put(n.v, maybeNewBestDistance);
                    parent.put(n.v, head.v);

                    double pi = landmarkSelector.pi(n.v, goal);

                    // b(10−i)/10,    b is original lower bound from s->t, i is checkpoint
                    boolean tenPercentMore = pi < originalPi * (10 - landmarkCheckpoint) / 10;
                    boolean enoughIterations = iterationsSinceLastLandmarkUpdate > 100;

                    if (tenPercentMore && enoughIterations) {
                        landmarksUpdated = true;
                        landmarkCheckpoint++;
                        iterationsSinceLastLandmarkUpdate = 0;
                    }

                    pq.add(new Pair(n.v, maybeNewBestDistance + pi)); 

                }
            }

            if (landmarksUpdated) {
                System.out.printf("Updated landmarks #%d @ iteration %d", landmarkCheckpoint, iterations);

                boolean addedLandmark = landmarkSelector.updateLandmarks(head.v, goal, 1);

                if (addedLandmark) {
                    System.out.println(" :)");
                    PriorityQueue<Pair> newPQ = new PriorityQueue<>(distComparator);
                
                    Iterator<Pair> it = pq.iterator();
                    Set<Vertex> alreadyAdded = new HashSet<>();
                    while (it.hasNext()) {
                        Pair next = it.next();
                        if (alreadyAdded.contains(next.v)) {
                            continue;
                        }
                        Vertex v = next.v;
                        double d = dist.get(v) + landmarkSelector.pi(v, goal);

                        newPQ.add(new Pair(v, d));
                    }
                    pq = newPQ;
                } else {
                    System.out.println(" :("); // TODO better logging
                }

                
            }
        }

        // Get out the shortest path
        System.out.println("  --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        if (parent.get(goal) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered, null);
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

        Solution solution = new Solution(out, edgesConsidered, null);




        return solution;
    }



    
    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("denmark-intersections.csv");

        Vertex a = new Vertex(56.0440049,9.9025227);
        Vertex b = new Vertex(56.0418177, 9.8967658); // 52 Skanderborg V 

        // a = GraphUtils.pickRandomVertex(graph);
        // b = GraphUtils.pickRandomVertex(graph);

        LandmarkSelector landmarkSelector = new LandmarkSelector(graph, 16, 1);

        ALT d = new ALT(graph, landmarkSelector);
        Solution solution = d.shortestPath(Location.CPH, Location.Skagen);
        // Solution solution = d.shortestPath(GraphUtils.pickRandomVertex(graph), GraphUtils.pickRandomVertex(graph));

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.Denmark);
        vis.drawPoint(landmarkSelector.getAllLandmarks(), landmarkSelector.getActiveLandmarks());
        System.out.println("allLandmarks size:    " + landmarkSelector.getAllLandmarks().size());
        System.out.println("activeLandmarks size: " + landmarkSelector.getActiveLandmarks().size());
        
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize("ALT");

    }

}




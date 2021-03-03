package pathfinding.standard;

import graph.BoundingBox;
import graph.Graph;
import graph.GraphPopulator;
import graph.GraphVisualiser;
import graph.Neighbor;
import graph.Vertex;
import pathfinding.framework.Edge;
import pathfinding.framework.PathfindingAlgo;
import pathfinding.framework.Solution;
import utility.*;

import java.util.*;

public class ALT implements PathfindingAlgo {
    private static final double INF_DIST = Double.MAX_VALUE;

    private Graph graph;

    private Map<Vertex, Map<Vertex, Double>> distanceToLandmark;
    private Map<Vertex, Map<Vertex, Double>> distanceFromLandmark;

    private Map<Vertex, Double> dist;
    private Map<Vertex, Vertex> parent;

    // For visual
    private List<Edge> edgesConsidered;

    public ALT(Graph graph) {
        this.graph = graph;

        Graph ginv = GraphUtils.invertGraph(graph);

        List<Vertex> landmarks = landmark(graph, 25);
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
        
        double bestTriangle = -INF_DIST;
        Vertex bestLandmark = null;

        for (Vertex v: distanceToLandmark.keySet()){
            Map<Vertex, Double> distTo = distanceToLandmark.get(v);
            Map<Vertex, Double> distFrom = distanceFromLandmark.get(v);

            double temp = Math.max(distTo.getOrDefault(start, -INF_DIST)
                                  -distTo.getOrDefault(goal, INF_DIST), 
                                   distFrom.getOrDefault(goal, -INF_DIST)
                                  -distFrom.getOrDefault(start, INF_DIST));
                                System.out.println(temp);
            if (temp > bestTriangle) {
                bestTriangle = temp;
                bestLandmark = v;
                System.out.println(bestLandmark + " is now best landmark");
            }
        }


        Map<Vertex, Double> dist = new HashMap<>();
        Map<Vertex, Vertex> parent = new HashMap<>(); 
        List<Edge> edgesConsidered = new ArrayList<>();
        
        dist.put(start, 0.0);

        DistComparator distComparator = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(distComparator);

        pq.add(new Pair(start, 0));

        Set<Vertex> minDist = new HashSet<>();

        int iterations = 0;
        while(pq.size() > 0){
            iterations++;
            Pair head = pq.poll();

            minDist.add(head.v);

            if (iterations % 1000 == 0) {
                System.out.println(head.dist);
                System.out.println("    --> " + iterations + ",   pq size: " + pq.size());
            }

            if (head.v.equals(goal)) {
                System.out.println("  --> Finished early at ");
                break;
            }

            for (Neighbor n : graph.getNeighboursOf(head.v)) {
                // RELAX
                double maybeNewBestDistance = dist.get(head.v) + n.distance;
                double previousBestDistance = dist.getOrDefault(n.v, INF_DIST);

                edgesConsidered.add(new Edge(head.v, n.v, maybeNewBestDistance));

                if (maybeNewBestDistance < previousBestDistance) {
                    dist.put(n.v, maybeNewBestDistance);
                    parent.put(n.v, head.v);

                    double estimateToGoal = Math.max(distanceToLandmark.get(bestLandmark)
                                                       .getOrDefault(n.v,INF_DIST)
                                                    -distanceToLandmark.get(bestLandmark)
                                                       .getOrDefault(goal,INF_DIST), 
                                                     distanceFromLandmark.get(bestLandmark)
                                                       .getOrDefault(goal,INF_DIST)
                                                     -distanceFromLandmark.get(bestLandmark)
                                                       .getOrDefault(n.v,INF_DIST)); 
                    // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                    // if (! minDist.contains(n.v)) {
                        pq.add(new Pair(n.v, maybeNewBestDistance + estimateToGoal)); 
                    // }
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
        // Map<Vertex, Vertex> predecessor = new HashMap<>(); 

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        pq.add(new Pair(start, 0));

        //g.getAllVertices().stream()
        //    .map(v -> new Pair(v, INF_DIST))
        //    .forEach(pq::add);
        
        Map<Vertex, Double> shortest = new HashMap<>();
        List<Edge> expanded = new ArrayList<>();

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

                    expanded.add(new Edge(head.v, n.v, maybeNewBestDistance));


                    if (maybeNewBestDistance < previousBestDistance) {
                        bestDist.put(n.v, maybeNewBestDistance);
                        // predecessor.put(n.v, head.v);

                        // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                        pq.add(new Pair(n.v, maybeNewBestDistance)); 
                    }
                });
        }
        return shortest;
    }

}

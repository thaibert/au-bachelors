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
        reachableLandmarks = findReachableTo(start, goal);
        System.out.println(reachableLandmarks.size());
        if (reachableLandmarks.size() == 0) {
            // bailout early
            return new Solution(new ArrayList<>(), new ArrayList<>());
        }


        //TODO the actual algorithm :D




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
        Graph graph = GraphPopulator.populateGraph("denmark-all-roads.csv");

        Vertex a = new Vertex(56.0440049,9.9025227);
        Vertex b = new Vertex(56.1814955,10.2042923);

        PathfindingAlgo d = new BidirectionalALT(graph, 5);
        Solution solution = d.shortestPath(Location.Skagen, Location.CPH);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.Denmark);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize("ALT");

    }
    
}

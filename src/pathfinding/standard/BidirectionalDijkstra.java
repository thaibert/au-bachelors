package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;
import java.util.*;
import utility.*;

public class BidirectionalDijkstra implements PathfindingAlgo {
    private final double INF_DIST = Double.MAX_VALUE;

    private final Graph g;
    private final Graph ginv;


    //_f means forward algo, _b means backwards algo
    private Map<Vertex, Double> bestDist_f;
    private Map<Vertex, Vertex> predecessor_f; // S in the algo is pred.keySet()
    private Map<Vertex, Double> bestDist_b;
    private Map<Vertex, Vertex> predecessor_b; // S in the algo is pred.keySet()
    private Set<Vertex> s_f;
    private Set<Vertex> s_b;
    private Vertex meetingNode; 
    double mu;

    // For visual
    private List<Edge> edgesConsidered;

    Set<Vertex> closed_f;
    Set<Vertex> closed_b;

    public BidirectionalDijkstra(Graph g) {
        this.g = g;
        this.ginv = GraphUtils.invertGraph(g);
    }
    

    public Solution shortestPath(Vertex start, Vertex goal) {

        // mu? // https://www.ucl.ac.uk/~ucahmto/math/2020/05/30/bidirectional-dijkstra.html

        bestDist_f = new HashMap<>();
        predecessor_f = new HashMap<>();
        bestDist_b = new HashMap<>();
        predecessor_b = new HashMap<>();
        s_f = new HashSet<>();
        s_b = new HashSet<>();
        meetingNode = null;
        mu = INF_DIST;
        
        closed_f = new HashSet<>(); 
        closed_b = new HashSet<>(); 


        //Purely for visualising
        edgesConsidered = new ArrayList<>();

        // Main source of inspiration:
        // https://www.ucl.ac.uk/~ucahmto/math/2020/05/30/bidirectional-dijkstra.html
        
        bestDist_f.put(start, 0.0);
        bestDist_b.put(goal, 0.0);


        // Main algo
        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq_f = new PriorityQueue<>(comp);
        pq_f.add(new Pair(start, 0));

        PriorityQueue<Pair> pq_b = new PriorityQueue<>(comp);
        pq_b.add(new Pair(goal, 0));

        while (pq_f.size() > 0 && pq_b.size() > 0) {

            Pair min_b = pq_b.peek();
            Pair min_f = pq_f.peek();

            // TODO BREAK CONDITION
            if (bestDist_b.get(min_b.v) + 
                bestDist_f.get(min_f.v) >= mu) {
                System.out.println("Entered exit");
                break;
            }

            if ((pq_f.size() < pq_b.size() && pq_f.size() > 0) || pq_b.size() == 0){
                Pair head_f = pq_f.poll();
                s_f.add(head_f.v); 
                closed_f.add(head_f.v);

                expandForwad(head_f, pq_f);
            } else {
                Pair head_b = pq_b.poll();
                s_b.add(head_b.v);
                closed_b.add(head_b.v);

                expandBackward(head_b, pq_b);
            }



            /*g.getNeighboursOf(head_f.v)
                .forEach(n -> {
                    if (closed_f.contains(n.v)){
                        return;
                    }
                    // RELAX
                    double maybeNewBestDistance = head_f.dist + n.distance;
                    double previousBestDistance = bestDist_f.getOrDefault(n.v, INF_DIST);

                    edgesConsidered.add(new Edge(head_f.v, n.v, maybeNewBestDistance));

                    if (maybeNewBestDistance < previousBestDistance) {
                        bestDist_f.put(n.v, maybeNewBestDistance);
                        predecessor_f.put(n.v, head_f.v);

                        // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                        pq_f.add(new Pair(n.v, maybeNewBestDistance)); 
                    }

                    if (s_b.contains(n.v) && bestDist_f.get(head_f.v) + n.distance + bestDist_b.get(n.v) < mu) {
                        mu = bestDist_f.get(head_f.v) + n.distance + bestDist_b.get(n.v);
                        meetingNode = n.v;
                    }
                });
           
            ginv.getNeighboursOf(head_b.v)
            .forEach(n -> {
                if (closed_b.contains(n.v)){
                    return;
                }
                // RELAX
                double maybeNewBestDistance = head_b.dist + n.distance;
                double previousBestDistance = bestDist_b.getOrDefault(n.v, INF_DIST);

                edgesConsidered.add(new Edge(head_b.v, n.v, maybeNewBestDistance));


                if (maybeNewBestDistance < previousBestDistance) {
                    bestDist_b.put(n.v, maybeNewBestDistance);
                    predecessor_b.put(n.v, head_b.v);

                    // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                    pq_b.add(new Pair(n.v, maybeNewBestDistance)); 
                }

                if (s_f.contains(n.v) && bestDist_b.get(head_b.v) + n.distance + bestDist_f.get(n.v) < mu) {
                    mu = bestDist_b.get(head_b.v) + n.distance + bestDist_f.get(n.v);
                    meetingNode = n.v;
                }
            });
        
        */


        }

        // Get out the shortest path
        System.out.println("  --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        /* TODO something that checks if we actually found something */
        if (predecessor_f.get(meetingNode) == null && predecessor_b.get(meetingNode) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered, meetingNode, closed_b.size() + closed_f.size());
        }

        Vertex temp = meetingNode;
        while (! start.equals(temp)) {
            out.add(temp);
            temp = predecessor_f.get(temp);
        }

        List<Vertex> out2 = new ArrayList<>();
        temp = meetingNode;
        while (! goal.equals(temp)) {
            temp = predecessor_b.get(temp);
            out2.add(temp);
        }
        out.add(start);
        Collections.reverse(out2);
        out2.addAll(out);
        

        System.out.println("      " + out2.size() + " nodes");
        System.out.println("      " + edgesConsidered.size() + " edges considered");
        System.out.println("      " + comp.getComparisons() + " comparisons");
        System.out.println("      " + mu + " distance");

        Solution solution = new Solution(out2, edgesConsidered, meetingNode, closed_b.size() + closed_f.size());

        return solution;

    }
    
    public void expandForwad(Pair head_f, PriorityQueue<Pair> pq_f){
        g.getNeighboursOf(head_f.v)
        .forEach(n -> {
            if (closed_f.contains(n.v)){
                return;
            }
            // RELAX
            double maybeNewBestDistance = head_f.dist + n.distance;
            double previousBestDistance = bestDist_f.getOrDefault(n.v, INF_DIST);

            edgesConsidered.add(new Edge(head_f.v, n.v, maybeNewBestDistance));

            if (maybeNewBestDistance < previousBestDistance) {
                bestDist_f.put(n.v, maybeNewBestDistance);
                predecessor_f.put(n.v, head_f.v);

                // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                pq_f.add(new Pair(n.v, maybeNewBestDistance)); 
            }

            if (s_b.contains(n.v) && bestDist_f.get(head_f.v) + n.distance + bestDist_b.get(n.v) < mu) {
                mu = bestDist_f.get(head_f.v) + n.distance + bestDist_b.get(n.v);
                meetingNode = n.v;
            }
        });
    }

    public void expandBackward(Pair head_b, PriorityQueue<Pair> pq_b){
        ginv.getNeighboursOf(head_b.v)
        .forEach(n -> {
            if (closed_b.contains(n.v)){
                return;
            }
            // RELAX
            double maybeNewBestDistance = head_b.dist + n.distance;
            double previousBestDistance = bestDist_b.getOrDefault(n.v, INF_DIST);

            edgesConsidered.add(new Edge(head_b.v, n.v, maybeNewBestDistance));


            if (maybeNewBestDistance < previousBestDistance) {
                bestDist_b.put(n.v, maybeNewBestDistance);
                predecessor_b.put(n.v, head_b.v);

                // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                pq_b.add(new Pair(n.v, maybeNewBestDistance)); 
            }

            if (s_f.contains(n.v) && bestDist_b.get(head_b.v) + n.distance + bestDist_f.get(n.v) < mu) {
                mu = bestDist_b.get(head_b.v) + n.distance + bestDist_f.get(n.v);
                meetingNode = n.v;
            }
        });
    }


    public static void main(String[] args) {
        // We need to be able to utilize the inverted graph, so for now we ignore space efficiency and just create 2 graphs
        Graph graph = GraphPopulator.populateGraph("iceland-latest-roads.csv");
        Graph pruned = GraphUtils.pruneChains(graph);

        Vertex a = new Vertex(56.1570293,9.814296);
        Vertex b = new Vertex(56.1582726,9.8152893);


        BidirectionalDijkstra d = new BidirectionalDijkstra(pruned);
        Solution solution = d.shortestPath(GraphUtils.findNearestVertex(pruned, 64.25013,-15.190943), GraphUtils.findNearestVertex(pruned,65.528015,-24.455515));

        GraphVisualiser vis = new GraphVisualiser(pruned, BoundingBox.Iceland);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.drawMeetingNode(solution.getMeetingNode());
        vis.visualize("Dijkstra bidirectional");
    }

}

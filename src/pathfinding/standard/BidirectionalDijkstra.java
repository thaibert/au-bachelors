package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;
import java.util.*;
import utility.*;

public class BidirectionalDijkstra{
    private final double INF_DIST = Double.MAX_VALUE;
    double mu = INF_DIST;

    //_f means forward algo, _b means backwards algo
    private Map<Vertex, Double> bestDist_f;
    private Map<Vertex, Vertex> predecessor_f; // S in the algo is pred.keySet()
    private Map<Vertex, Double> bestDist_b;
    private Map<Vertex, Vertex> predecessor_b; // S in the algo is pred.keySet()
    private Set<Vertex> s_f;
    private Set<Vertex> s_b;
    private Vertex bestVertex; 

    // For visual
    private List<Edge> edgesConsidered;
    


     // TODO problems in taking 2 graphs as input :/
    public Solution shortestPath(Graph g, Graph ginv, Vertex start, Vertex goal) {

        // mu? // https://www.ucl.ac.uk/~ucahmto/math/2020/05/30/bidirectional-dijkstra.html

        bestDist_f = new HashMap<>();
        predecessor_f = new HashMap<>();
        bestDist_b = new HashMap<>();
        predecessor_b = new HashMap<>();
        s_f = new HashSet<>();
        s_b = new HashSet<>();
        
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

            Pair head_f = pq_f.poll();
            Pair head_b = pq_b.poll();

            s_f.add(head_f.v); 
            s_b.add(head_b.v);

            // TODO BREAK CONDITION
            if (bestDist_b.get(head_b.v) + 
                bestDist_f.get(head_f.v) > mu) {
                System.out.println("Entered exit");
                break;
            }


            g.getNeighboursOf(head_f.v)
                .forEach(n -> {
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
                        bestVertex = n.v;
                    }
                });
           
            ginv.getNeighboursOf(head_b.v)
            .forEach(n -> {
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
                    mu = bestDist_f.get(head_b.v) + n.distance + bestDist_f.get(n.v);
                    bestVertex = n.v;
                }
            });
        



        }

        // Get out the shortest path
        System.out.println("  --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        /* TODO something that checks if we actually found something
        if (predecessor.get(goal) == null) {
            System.out.println("  --> No path exists!!");
            return new Solution(new ArrayList<>(), edgesConsidered);
        }*/

        Vertex temp = bestVertex;
        while (! start.equals(temp)) {
            out.add(temp);
            temp = predecessor_f.get(temp);
        }

        List<Vertex> out2 = new ArrayList<>();
        temp = bestVertex;
        while (! goal.equals(temp)) {
            temp = predecessor_b.get(temp);
            out2.add(temp);
        }
        out.add(start);
        Collections.reverse(out);
        out.addAll(out2);
        
        System.out.println("      " + out.size() + " nodes");
        System.out.println("      " + edgesConsidered.size() + " edges considered");
        System.out.println("      " + comp.getComparisons() + " comparisons");

        Solution solution = new Solution(out, edgesConsidered); // edgesConsidered);

        return solution;

    }
    
    public static void main(String[] args) {
        // We need to be able to utilize the inverted graph, so for now we ignore space efficiency and just create 2 graphs
        Graph graph = GraphPopulator.populateGraph("denmark-intersections.csv");
        Graph invertedGraph = GraphUtils.invertGraph(graph);

        // Vertex a = new Vertex(56.1634686,10.1722176); // Viborgvej
        // Vertex a = new Vertex(56.1723636,9.5538336); // Silkeborg
        // Vertex b = new Vertex(56.1828308,10.2037825); // O2/Randersvej

        BidirectionalDijkstra d = new BidirectionalDijkstra();
        Solution solution = d.shortestPath(graph, invertedGraph, Location.CPH, Location.Skagen);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.Denmark);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize();
    }

}
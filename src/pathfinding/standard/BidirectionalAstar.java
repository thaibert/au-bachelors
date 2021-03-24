package pathfinding.standard;

import pathfinding.framework.*;
import utility.*;
import graph.*;
import java.util.*;

public class BidirectionalAstar implements PathfindingAlgo {
    private final double INF_DIST = Double.MAX_VALUE;
    

    Graph g;
    Graph ginv;

    public BidirectionalAstar(Graph g) {
        this.g = g;
        this.ginv = GraphUtils.invertGraph(g);
    } 

    private Map<Vertex, Double> dist_f;
    private Map<Vertex, Vertex> pred_f; // S in the algo is pred.keySet()
    private Map<Vertex, Double> dist_b;
    private Map<Vertex, Vertex> pred_b; // S in the algo is pred.keySet()

    private Set<Vertex> s_f;
    private Set<Vertex> s_b;

    private List<Edge> edgesConsidered;

    private Vertex bestVertex; 
    private double mu;

    @Override
    public Solution shortestPath(Vertex start, Vertex goal) {
        // TODO Auto-generated method stub
        mu = INF_DIST;
        bestVertex = null;
  

        dist_f = new HashMap<>();
        pred_f = new HashMap<>();
        dist_b = new HashMap<>();
        pred_b = new HashMap<>();
        s_f = new HashSet<>();
        s_b = new HashSet<>();

        //Purely for visualising
        edgesConsidered = new ArrayList<>();

        dist_f.put(start, 0.0);
        dist_b.put(goal, 0.0);

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq_f = new PriorityQueue<>(comp);
        PriorityQueue<Pair> pq_b = new PriorityQueue<>(comp);


        pq_f.add(new Pair(start, 0));
        pq_b.add(new Pair(goal, 0));

        int num = 0;
        while (pq_f.size() > 0 && pq_b.size() > 0) {
            // Testing purpose
            //num++;
            //if (num % 1000 == 0) {
                //System.out.println("    --> " + num);
            //}

            Pair min_f = pq_f.poll();
            Pair min_b = pq_b.poll();     
            
            s_f.add(min_f.v); 
            s_b.add(min_b.v);

            // TODO Something about not exploring the same nodes in forward and backwards search?


            //TODO er det her rigtigt?
            if (dist_f.get(min_f.v) + dist_b.get(min_b.v) >= 
                mu + potentialBackward(start, goal, goal)) {
                System.out.println("Potential added: " + potentialBackward(start, goal, goal));
                System.out.println("Entered exit");
                break;
            }


            g.getNeighboursOf(min_f.v).forEach(n -> {
                double reduced_distance = n.distance;
                double tent_gScore = dist_f.getOrDefault(min_f.v, INF_DIST) + reduced_distance;

                // TODO Is this the right thing added?
                double potentialNewFscore = tent_gScore + potentialForward(start, goal, n.v);

                edgesConsidered.add(new Edge(min_f.v, n.v, potentialNewFscore));
                if (tent_gScore < dist_f.getOrDefault(n.v, INF_DIST)) {

                    pred_f.put(n.v, min_f.v);
                    dist_f.put(n.v, tent_gScore);

                    //TODO add selv hvis backward har set den her node?
                    pq_f.add(new Pair(n.v, potentialNewFscore));
                }

                if (s_b.contains(n.v) && dist_f.get(min_f.v) + reduced_distance + dist_b.get(n.v) < mu) {
                    mu = dist_f.get(min_f.v) + reduced_distance + dist_b.get(n.v);
                    bestVertex = n.v;
                }
            });

            ginv.getNeighboursOf(min_b.v).forEach(n -> {
                
                double reduced_distance = n.distance;
                double tent_gScore = dist_b.getOrDefault(min_b.v, INF_DIST) + reduced_distance;


                // TODO Is this the right thing added?
                double potentialNewFscore = tent_gScore + potentialBackward(start, goal, n.v) ;

                //edgesConsidered.add(new Edge(min_b.v, n.v, tent_gScore));
                if (tent_gScore < dist_b.getOrDefault(n.v, INF_DIST)) {
                    pred_b.put(n.v, min_b.v);
                    dist_b.put(n.v, tent_gScore);

                    //TODO add selv hvis forward har set den her node?
                    pq_b.add(new Pair(n.v, potentialNewFscore));
                }

                if (s_f.contains(n.v) && dist_b.get(min_b.v) + reduced_distance + dist_f.get(n.v) < mu) {
                    mu = dist_b.get(min_b.v) + reduced_distance + dist_f.get(n.v);
                    bestVertex = n.v;
                }

            });

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


    private double potentialForward(Vertex start, Vertex goal, Vertex v){

        double est = (heuristic(v, goal) - heuristic(start, v))/2 + heuristic(start, goal)/2; 
        return est;
    }

    private double potentialBackward(Vertex start, Vertex goal, Vertex v){


        double est = (heuristic(start, v) - heuristic(v, goal))/2 + heuristic(start, goal)/2;
        return est;
    }

    // TODO what is the heuristic suposed to be for bidirectional A*
    private double heuristic(Vertex a, Vertex b) {
        return GraphUtils.haversineDist(a, b);
    }


    public static void main(String[] args) {
        // We need to be able to utilize the inverted graph, so for now we ignore space efficiency and just create 2 graphs
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

        Vertex a = new Vertex(56.1336391,9.7235112);
        Vertex b = new Vertex(56.1906785,10.0880127);


        BidirectionalAstar d = new BidirectionalAstar(graph);
        Solution solution = d.shortestPath(Location.Viborgvej, Location.Randersvej);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.Aarhus);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize("A* bidirectional");
    }
    
}

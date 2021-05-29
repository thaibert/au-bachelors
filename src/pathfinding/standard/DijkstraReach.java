package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.*;

import utility.*;

import java.io.*;

public class DijkstraReach implements PathfindingAlgo {
    private final double INF_DIST = Double.MAX_VALUE;

    private Graph g;
    private Map<Vertex, Double> reaches;

    //DEBUGGING
    private int nodesPruned = 0;

    public Set<Vertex> prunedNodes = new HashSet<>();


    public DijkstraReach(Graph g, Map<Vertex, Double> reaches) {
        this.g = g;
        this.reaches = reaches;
    }

    public Solution shortestPath(Vertex start, Vertex goal){
        System.out.println("--> Running \"reach\" Dijkstra");
        //  Pseudocode from CLRS
        //  Initialize-Single-Source(G, s) (s = source)
        //  S = Ø
        //  Q = G.V
        //  While Q != Ø
        //      u = Extract-Min(Q)    
        //      S = S U {u}
        //      for each vertex v in G.Adj[u]
        //          Relax(u,v,w)   

        /* Invariants:
            All edges are non-negative
            Q = V - S at the start of each iteration of the while loop
            Each edge is extracted from Q and added to S exactly once 
        */


        Collection<Vertex> VminusS = new HashSet<>(g.getAllVertices()); 
        

        Map<Vertex, Double> bestDist = new HashMap<>();
        bestDist.put(start, 0.0);
        Map<Vertex, Vertex> predecessor = new HashMap<>(); 
        List<Edge> edgesConsidered = new ArrayList<>();

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        Set<Vertex> closed = new HashSet<>();

        Collection<Vertex> Q = new HashSet<>(g.getAllVertices()); // Initially all vertices are added
        /*g.getAllVertices().stream()
            .map(p -> new Pair(p, INF_DIST))
            .forEach(p -> {
                pq.add(p);
                assert(p.dist >= 0); // All weights are non-negative
            });
        pq.remove(new Pair(start, INF_DIST)); // Update start's weight */
        pq.add(new Pair(start, 0));

        //assert(pq.peek() != null && pq.peek().dist >= 0); // All weights are non-negative

        
        int iterations = 0;
        while (pq.size() > 0) {

            iterations++;

            if (iterations % 1000 == 0) {
                System.out.println("    --> " + iterations + ",   pq size: " + pq.size());
            }

            Pair head = pq.poll();
            Q.remove(head.v);

            VminusS.remove(head.v); // Put v in S ==> V-S loses v
            if (closed.contains(head.v)){
                continue;
            }
            closed.add(head.v);

            if (head.v.equals(goal)) {
                System.out.println("  --> Finished early at " + iterations);
                break;
            }

            g.getNeighboursOf(head.v)
                .forEach(n -> {
                    // RELAX                    
                    if(closed.contains(n.v)){
                        return;
                    }

                    double maybeNewBestDistance = head.dist + n.distance;
                    double previousBestDistance = bestDist.getOrDefault(n.v, INF_DIST);

                    if (reaches.get(n.v)*1.00001 < maybeNewBestDistance && reaches.get(n.v)*1.00001 < GraphUtils.haversineDist(n.v, goal) ){
                        //System.out.println("Node pruned with reaching");
                        prunedNodes.add(n.v);
                        nodesPruned++;
                        //System.out.println("First check " + reaches.get(n.v) + " < " + maybeNewBestDistance);
                        //System.out.println("Second check " + reaches.get(n.v) + " < " + GraphUtils.haversineDist(n.v, goal));
                        //System.out.println("Vertex: " + n.v + "\n");

                        //Gutmans test is false, and the node is not worth considering.
                        return; //Again this ugly return statement inside a foreach loop, its equal to continue
                    }

                    edgesConsidered.add(new Edge(head.v, n.v, maybeNewBestDistance));


                    if (maybeNewBestDistance < previousBestDistance) {
                        // Reach pruning:


                        // Update v.d and v.pi
                        bestDist.put(n.v, maybeNewBestDistance);
                        predecessor.put(n.v, head.v);
                        
                        // Remove pair of (v, oldDist) and insert (v, newDist) instead
                        pq.remove(new Pair(n.v, previousBestDistance));

                        Pair newPair = new Pair(n.v, maybeNewBestDistance);
                        pq.add(newPair);
                        assert(newPair.dist >= 0); 
                    }
                });
        }


        //DEBUGGING
        System.out.println("Nodes pruned: " + nodesPruned);

        // Get out the shortest path
        System.out.println("  --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        if (predecessor.get(goal) == null) {
            System.out.println("  --> No path exists!!");
            out.add(goal);
            out.add(start);
            return new Solution(out, edgesConsidered, null);
        }

        Vertex temp = goal;
        while (! start.equals(temp)) {
            out.add(temp);
            temp = predecessor.get(temp);
        }
        out.add(start);
        System.out.println("      " + out.size() + " nodes");
        System.out.println("      " + edgesConsidered.size() + " edges considered");
        System.out.println("      " + comp.getComparisons() + " comparisons");
        System.out.println("      " + bestDist.get(goal) + " distance");

        Solution solution = new Solution(out, edgesConsidered, null);

        return solution;
    }




    public static void main(String[] args) {
        // Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");
        Graph graph = readShortcutGraph("iceland-shortcutV2");
        Graph fullG = GraphPopulator.populateGraph("iceland-latest-roads.csv");
        Graph pruned = GraphUtils.pruneChains(fullG);

        Map<Vertex, Double> r = readReaches("iceland-reachV2");

        
        PrintStream originalStream = System.out;

        PrintStream noopStream = new PrintStream(new OutputStream(){
            public void write(int b) {
                // NO-OP
            }
        });
        
        /*System.setOut(noopStream);
        
        for (int i = 0; i < 1000; i++ ){
            System.setOut(originalStream);
            //System.out.print(".");
            if (i % 100 == 0){
                System.out.println("\n      " + i +  " iterations done!");
            }
            System.setOut(noopStream);


            Vertex a = GraphUtils.pickRandomVertex(graph);
            Vertex b = GraphUtils.pickRandomVertex(graph);

            DijkstraReach d = new DijkstraReach(graph, r);
            Solution solution = d.shortestPath(a, b);
    

            PathfindingAlgo da = new Dijkstra(fullG);
            Solution solution2 = da.shortestPath(a, b);

            // if (!solution.getShortestPath().equals(solution2.getShortestPath())){
            Collection<Vertex> dijkstraPath = new ArrayList<>(solution2.getShortestPath());
            Collection<Vertex> shortcutPath = new ArrayList<>(solution.getShortestPath());

            System.setOut(originalStream);
            int diff = dijkstraPath.size() - shortcutPath.size();
            //System.out.println("Reach path has  " + diff + "  fewer nodes");
            if (diff > 25) {
                System.out.println("  diff " + diff + " @ " + a + "->" + b);
                GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
                vis.drawPath(solution.getShortestPath());
                vis.drawVisited(solution.getVisited());
                //vis.visualize("Dijkstra reach");

                GraphVisualiser vis2 = new GraphVisualiser(fullG, BoundingBox.AarhusSilkeborg);
                vis2.drawPath(solution2.getShortestPath());
                vis2.drawVisited(solution2.getVisited());
                System.out.println("Nodes considered reach: " + solution.getVisited().size() + " vs normal dijkstra " + solution2.getVisited().size());
                //vis2.visualize("Dijkstra normal");
            }
            System.setOut(noopStream);

            //if (! solution2.getShortestPath().equals(solution.getShortestPath())) {  
            if (! dijkstraPath.containsAll(shortcutPath)){              

                try {
                    System.setOut(originalStream);

                    System.out.println("Mistake found on :" + a + " -> " + b);

                    GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.Iceland);
                    vis.drawPath(solution.getShortestPath());
                    vis.drawVisited(solution.getVisited());
                    vis.visualize("Dijkstra Reaches");


                    GraphVisualiser vis2 = new GraphVisualiser(graph, BoundingBox.Iceland);
                    vis2.drawPath(solution2.getShortestPath());
                    vis2.drawVisited(solution2.getVisited());
                    vis2.visualize("Dijkstra");

                    Thread.sleep(100000);

                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        }*/

        // 63.615295,-20.239555 -> 63.606285,-20.230413
        //Vertex a = new Vertex(63.739067,-20.12924);
        //Vertex b = new Vertex(65.67908,-18.131926);

        Vertex a = GraphUtils.pickRandomVertex(fullG);
        Vertex b = GraphUtils.pickRandomVertex(fullG);


        DijkstraReach d = new DijkstraReach(graph, r);
        Solution solution = d.shortestPath(a, b);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.Iceland);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());

        System.out.println("Edges considered      " + solution.getVisited().size() );



        PathfindingAlgo da = new DijkstraTraditional(pruned);
        Solution solution2 = da.shortestPath(a, b);

        for (Vertex v: solution2.getShortestPath()) {
            if (d.prunedNodes.contains(v)){
                //System.out.println(v + ": with reach " + r.get(v));
                //System.out.println(GraphUtils.realLength(pruned, solution2.getShortestPath().subList(0, solution2.getShortestPath().indexOf(v))));
                //System.out.println(GraphUtils.realLength(pruned, solution2.getShortestPath().subList(solution2.getShortestPath().indexOf(v), solution2.getShortestPath().size()-1)));
                vis.drawMeetingNode(v);
            }
        }
        vis.visualize("Dijkstra Reaches");

        GraphVisualiser vis2 = new GraphVisualiser(pruned, BoundingBox.Iceland);
        vis2.drawPath(solution2.getShortestPath());
        vis2.drawVisited(solution2.getVisited());
        vis2.visualize("Dijkstra");
        
      
    }

    public static Map<Vertex, Double> readReaches(String filename) {
        Map<Vertex, Double> r = null;
        try {
            File toRead = new File(filename);
            FileInputStream fis = new FileInputStream(toRead);
            ObjectInputStream ois = new ObjectInputStream(fis);
    
            r = (HashMap<Vertex,Double>) ois.readObject();
    
            ois.close();
            fis.close();
            //print All data in MAP
        } catch(Exception e) {
            e.printStackTrace();
        }

        return r;
    }

    private static Graph readShortcutGraph(String filename) {
        Graph g = null;
        try {
            File toRead = new File(filename);
            FileInputStream fis = new FileInputStream(toRead);
            ObjectInputStream ois = new ObjectInputStream(fis);
    
            g = (Graph) ois.readObject();
    
            ois.close();
            fis.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

        return g;
    }

}

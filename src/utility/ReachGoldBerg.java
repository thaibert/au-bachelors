package utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;


import graph.*;
import pathfinding.framework.Edge;

public class ReachGoldBerg {
    private static Double INF_DIST = Double.MAX_VALUE;


    public static Map<Vertex, Double> reach(Graph graph, double[] bs) {
        Map<Vertex, Double> inPenalties = new HashMap<>();
        Map<Vertex, Double> outPenalties = new HashMap<>();

        Graph graphPrime = graph;

        Map<Edge, Double> r = new HashMap<>();
        Map<Vertex, Double> rVertex = new HashMap<>();

        for (int i = 0; i < bs.length; i++){
            //Iterative step

            // Grow trees 
            for (Vertex v: graphPrime.getAllVertices()){
                Tree tree = partialTree(graphPrime, v, bs[i]);
                // We do not include v according to Goldberg
                tree.inner.remove(v);
                for (Vertex u: tree.inner){
                    for (Neighbor n: graphPrime.getNeighboursOf(u)){
                        Double tempR = calcReach(u,n.v, tree);
                        Edge un = new Edge(v, u, 0.0);
                        if (r.get(un) < tempR){
                            r.put(un, tempR);
                        }
                    }
                }
            }
            // Prune
            

            // Penalties
            for (Vertex v: graphPrime.getAllVertices()){
                //TODO in penalties


                for (Neighbor n: graphPrime.getNeighboursOf(v)){
                    outPenalties.put(v, Math.max(outPenalties.getOrDefault(v, 0.0), r.get(new Edge(v, n.v, 0.0))));
                }
            }
            

            // Shortcuts
            shortcut(graphPrime, bs[i]); //TODO what graph should this be given

        } 



        return rVertex;
    }

    public static Double calcReach(Vertex v, Vertex u, Tree tree){

        return Math.min(depth(v,u, tree), height(v,u,tree));

    }

    public static double depth(Vertex v, Vertex u, Tree tree){
        return tree.dist.get(u); // TODO i think it's to the end of the edge, but unsure
    }

    public static double height(Vertex v, Vertex u, Tree tree){
        Double h = 0.0;
        for (Vertex w: tree.leafs){
            if (tree.paths.get(w).contains(v) && tree.closed.contains(w)) {
                h = Math.max(h, tree.dist.get(w) - tree.dist.get(v)); // If we take the distance to the beginning node, we don't have to keep track of the length of the edge
            } else if (tree.paths.get(w).contains(v) && !tree.closed.contains(w)){
                h = INF_DIST;
            }
        }

        return h;
    }

    public static Tree partialTree(Graph g, Vertex x, double epsilon){
        // This may be super inefficient to maintain all this, but we find it easier than trying to squeze it all 
        // into one data structure.

        //TODO Something about perturbation????? wtf even is that 

        Set<Vertex> innerCircle = new HashSet<>();
        Set<Vertex> outerCircle = new HashSet<>();

        Map<Vertex, Double> bestDist = new HashMap<>();
        Map<Vertex, Vertex> pred = new HashMap<>();


        Map<Vertex, Set<Vertex>> paths = new HashMap<>(); // This may not scale idk
        Map<Vertex, Double> xprimeDist = new HashMap<>();

        Set<Vertex> leafTprime = new HashSet<>(); 
        Set<Vertex> leafT = new HashSet<>();

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        Map<Vertex, Double> noOfChildren = new HashMap<>();

        Set<Vertex> closed = new HashSet<>();

        pq.add(new Pair(x, 0));
        bestDist.put(x, 0.0);
        xprimeDist.put(x, 0.0);

        paths.put(x, new HashSet<Vertex>());
        
        double bestxPrimeDist = 0.0;
        
        while (pq.size() > 0) {

            boolean trueForAllLeaf = leafTprime.size() > 0 && bestxPrimeDist > 2 * epsilon ; // if there are any, assume it's tru and disprove in for loop
            
            /*System.out.println(bestDist);
            System.out.println(xprimeDist);
            System.out.println(leafT);
            System.out.println(leafTprime + "\n");*/

            // Stopping condition as shown in https://www.microsoft.com/en-us/research/wp-content/uploads/2006/01/tr-2005-132.pdf
            for (Vertex v: leafTprime) {
                if (!(leafT.contains(v) || xprimeDist.get(v) >= 2 * epsilon)){
                    trueForAllLeaf = false;
                    break;
                }
            }
            if (trueForAllLeaf){                
                //System.out.println("Break 1");
                /*System.out.println("Start: " + start);
                System.out.println(leafT);
                System.out.println(leafTprime + "\n");*/
                break;
            }


            Pair head = pq.poll();
            if (closed.contains(head.v)){
                continue;
            }
            closed.add(head.v);
            // Maintain inner and outercircles. 
            if (xprimeDist.get(head.v) < epsilon) {
                innerCircle.add(head.v);
            } else {
                outerCircle.add(head.v);
            }

            g.getNeighboursOf(head.v)
                .forEach(n -> {
                    // RELAX
                    if (closed.contains(n.v)){
                        return;
                    }

                    double maybeNewBestDistance = head.dist + n.distance;
                    double previousBestDistance = bestDist.getOrDefault(n.v, INF_DIST);

                    if (maybeNewBestDistance < previousBestDistance) {
                        leafT.add(n.v);
                        leafT.remove(head.v);
                        
                        Set<Vertex> path = new HashSet<Vertex>(paths.get(head.v));
                        path.add(head.v);
                        paths.put(n.v, path); // This should maintain all shortest paths

                        if (!head.v.equals(x)) {
                            xprimeDist.put(n.v, xprimeDist.get(head.v) + n.distance);
                        } else {
                            xprimeDist.put(n.v, xprimeDist.get(head.v) + 0.0);
                        }
                        bestDist.put(n.v, maybeNewBestDistance);
                        Vertex oldParent = pred.get(n.v);
                        if (oldParent != null){
                            noOfChildren.put(oldParent, noOfChildren.get(oldParent) -1); // Should never give 0
                            if (noOfChildren.get(oldParent) == 0){
                                leafT.add(oldParent);
                            }
                        } 
                        bestDist.put(n.v, maybeNewBestDistance);

                        pred.put(n.v, head.v);
                        noOfChildren.put(head.v, noOfChildren.getOrDefault(head.v, 0.0) + 1);

                        // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                        pq.add(new Pair(n.v, maybeNewBestDistance)); 
                    }
                });
        }
        //System.out.println("Dijkstra done");
        Tree tree = new Tree(bestDist, innerCircle, outerCircle, pred, leafT, paths, closed);
        return tree;

    }

    public static Graph shortcut(Graph g, double epsilon){
        //TODO add shortcuts to the graph

        return null;
    }


    public static void main(String[] args){
        //Graph graph = makeExampleGraph();
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");
        //Graph graph = makeSquareGraph(); 

        /*for (Vertex v: graph.getAllVertices()){
            for (Neighbor n: graph.getNeighboursOf(v)){boegebakken-intersections
                System.out.println(n);
            }
        }*/

        long timeBefore = System.currentTimeMillis();
        double[] bs = new double[]{25,100, 250, 500};
        //double[] bs = new double[]{200};
        Map<Vertex, Double> r = reach(graph, bs);
        long timeAfter = System.currentTimeMillis();

        
        System.out.println(r.keySet().size() + " reaches returned");
        //System.out.println(r);
        System.out.println("Calculating reach took " + ((timeAfter-timeBefore)/1000) + " seconds");

        saveReachArrayToFile("aarhus-silkeborg-reach", r);

    }

    public static void saveReachArrayToFile(String filename, Map<Vertex, Double> r ){
        try {
            File fileOne = new File(filename);
            FileOutputStream fos = new FileOutputStream(fileOne);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
    
            oos.writeObject(r);
            oos.flush();
            oos.close();
            fos.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }



    private static Graph makeExampleGraph() {
        Graph graph = new SimpleGraph();
        Vertex a = new NamedVertex("a");
        Vertex b = new NamedVertex("b");
        Vertex c = new NamedVertex("c");
        Vertex d = new NamedVertex("d");
        Vertex e = new NamedVertex("e");
        Vertex f = new NamedVertex("f");
        Vertex g = new NamedVertex("g");
        Vertex s = new NamedVertex("s");
        Vertex t = new NamedVertex("t");

        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addVertex(d);
        graph.addVertex(e);
        graph.addVertex(f);
        graph.addVertex(g);
        graph.addVertex(s);
        graph.addVertex(t);
        
        graph.addEdge(a, d, 9);
        graph.addEdge(a, s, 4);
        graph.addEdge(b, e, 3);
        graph.addEdge(c, e, 4);
        graph.addEdge(c, f, 6);
        graph.addEdge(c, s, 7);
        graph.addEdge(d, a, 9);
        graph.addEdge(d, e, 12);
        graph.addEdge(d, t, 13);
        graph.addEdge(e, b, 3);
        graph.addEdge(e, c, 4);
        graph.addEdge(e, d, 12);
        graph.addEdge(e, f, 2);
        graph.addEdge(e, g, 5);
        graph.addEdge(f, c, 6);
        graph.addEdge(f, e, 2);
        graph.addEdge(f, t, 9);
        graph.addEdge(g, e, 5);
        graph.addEdge(g, t, 3);
        graph.addEdge(s, a, 4);
        graph.addEdge(s, b, 5);
        graph.addEdge(s, c, 7);
        graph.addEdge(t, d, 13);
        graph.addEdge(t, f, 9);
        graph.addEdge(t, g, 3); 

        return graph;
    }

    private static Graph makeSquareGraph() {
        Graph graph = new SimpleGraph();
        Vertex a = new NamedVertex("a");
        Vertex b = new NamedVertex("b");
        Vertex c = new NamedVertex("c");
        Vertex d = new NamedVertex("d");
        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addVertex(d);


        graph.addEdge(a, b, 19.23);
        graph.addEdge(b, a, 19.23);
        
        graph.addEdge(a, c, 14.49);
        graph.addEdge(c, a, 14.49);
        
        graph.addEdge(c, d, 19.25);
        graph.addEdge(d, c, 19.25);
        
        graph.addEdge(b, d, 13.80);
        graph.addEdge(d, b, 13.80);

        return graph;
    }

}

class Tree {
    public Map<Vertex, Double> dist;
    public Set<Vertex> inner;
    public Set<Vertex> outer;
    public Map<Vertex,Vertex> pred;
    public Set<Vertex> leafs;
    public Map<Vertex, Set<Vertex>> paths;
    public Set<Vertex> closed;

    public Tree(Map<Vertex, Double> dist, Set<Vertex> inner, Set<Vertex> outer, Map<Vertex,Vertex> pred, Set<Vertex> leafs, Map<Vertex, Set<Vertex>> paths, Set<Vertex> closed){
        this.dist = dist;
        this.inner = inner;
        this.outer = outer;
        this.pred = pred;
        this.leafs = leafs;
        this.paths = paths;
        this.closed = closed;
    }

} 
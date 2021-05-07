package utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import javax.swing.plaf.basic.BasicListUI.ListDataHandler;
import javax.swing.plaf.metal.MetalIconFactory.TreeControlIcon;

import org.checkerframework.checker.units.qual.C;

import graph.*;

public class Reach {
    public static final double INF_DIST = Double.MAX_VALUE;


    //TODO testing something
    static Set<Vertex> testedForReach = new HashSet<>();

    // bs is a 
    public static Map<Vertex, Double> reach(Graph graph, double[] bs) {
        System.out.println("Calculating reach");
        System.out.println("  (" + graph.getAllVertices().size() + " nodes)");
        Graph graphPrime = graph;
        Graph graphInv = GraphUtils.invertGraph(graph);
        Map<Vertex, Double> bounds = new HashMap<>();

        Map<Vertex, Double> r = new HashMap<>();

        for (Vertex v : graph.getAllVertices()) {
            bounds.put(v, INF_DIST);
        }

        for (int i = 0; i < bs.length; i++) {
            long timeBefore = System.currentTimeMillis();

            // Iterate!
            System.out.println("Iterating, epsilon = " + bs[i]);
            System.out.println("Gprime size at this iteration = " + graphPrime.getAllVertices().size());
            //System.out.println("Reaches: " + r);


            Graph graphPrimeInv = GraphUtils.invertGraph(graphPrime);
            double c = 0; // todo should never be negative, right?

            Collection<Vertex> vertices = graph.getAllVertices();
            Collection<Vertex> verticesPrime = graphPrime.getAllVertices();
            //System.out.println("V': " + verticesPrime);

            Collection<Vertex> vMinusVPrime = new HashSet<>(vertices);
            vMinusVPrime.removeAll(verticesPrime);

            if (! vertices.equals(verticesPrime) ) {  // todo: gutman makes no sense!
                // todo: maybe only look at the size?
                // if V = V'

                for (Vertex x : vMinusVPrime) {
                    c = Math.max(c, bounds.getOrDefault(x, INF_DIST));
                }
            }

            // 
            for (Vertex v : verticesPrime) {
                bounds.put(v, 0.0);
                r.put(v, 0.0);
            }
            
            int interations = 0;
            for (Vertex sPrime : verticesPrime) {
                if (interations % 1000 == 0) {
                    //System.out.println("1000 passed");
                }
                interations++;

                //System.out.println("s' = "+ sPrime);
                double g = 0;
                double d = 0;
                for (Neighbor x : graphInv.getNeighboursOf(sPrime)) {
                    if (graphPrimeInv.getNeighboursOf(sPrime).contains(x)) {
                        // both in E and E', so doesn't fulfill conditon 
                        continue;
                    }
                    g = Math.max(g, bounds.get(x.v) + measure(x.v, sPrime));
                    d = Math.max(d, measure(x.v, sPrime));
                }

                // Traverse T
                Map<Vertex, Neighbor> tree = dijkstra(graphPrime, sPrime, bs[i]);
                //System.out.println(sPrime.toString() + tree.keySet());

                for (Vertex v : tree.keySet()) {
                    // compute r(v, T) TODO needed here?

                    Collection<Vertex> leaves = findLeaves(v, tree);
                    
                    // Loop over all paths in T that yadda yadda yadda
                    for (Vertex tPrime : leaves) {
                        double rt = 0;

                        if (vMinusVPrime.contains(tPrime)) {
                            rt = bounds.get(tPrime);
                        }
                        
                        if (sPrime.equals(v) || v.equals(tPrime)){
                            // There is no suffix or prefix. So it will always return 0. And it should not ?
                            // TODO remove maybe?
                            continue;
                        }
                        double rb = Math.min(g + measure(sPrime, v, tree), // todo give tree?
                                             rt + measure(v, tPrime, tree));  // todo give tree?
                        
   
                        if (rb > bounds.get(v)) {
                            bounds.put(v, rb);
                        }
                    }

                    testedForReach.add(v);
                    double reachV = calcReach(v, tree);
                    if (reachV > r.get(v)) {
                        r.put(v, reachV);
                    }

                }

            }


            for (Vertex v : verticesPrime) {
                Vertex u = new Vertex(56.1870903,10.02968);
                if (u.equals(v)){
                    System.out.println(u);
                    System.out.println("Reach:  " + r.get(v));
                    System.out.println("Bounds: " + bounds.get(u));
                }
                if (r.get(v) >= bs[i]) {
                    bounds.put(v, INF_DIST); // Reach not validated  
                }
                if (u.equals(v)){
                    System.out.println(u);
                    System.out.println("Reach:  " + r.get(v));
                    System.out.println("Bounds: " + bounds.get(u));
                }
            }

            // Change G'!!
            Graph newGPrime = new SimpleGraph();
            // get new vertices
            for (Vertex v : vertices) {
                if (bounds.get(v) == INF_DIST) {
                    newGPrime.addVertex(v);
                }
            }
            Collection<Vertex> verticesStillIncluded = new HashSet<>(newGPrime.getAllVertices());
            // get new edges
            for (Vertex v : newGPrime.getAllVertices()) {
                for (Neighbor n : graph.getNeighboursOf(v)) {
                    if (verticesStillIncluded.contains(n.v)) {
                        newGPrime.addEdge(v, n.v, n.distance);
                    }
                }
            }

            graphPrime = newGPrime;

            long timeAfter = System.currentTimeMillis();
            System.out.println("Calculating reach iteration " + i + " took " + ((timeAfter-timeBefore)/1000) + " seconds");


        }
        // All the nodes still in Gprime, their reaches have not been "settled" yet, so set them = inf
        for (Vertex v : graph.getAllVertices()) {
            if (bounds.get(v) == INF_DIST) {
                r.put(v, INF_DIST);       
            }
        }

        return r;
    }


    private static double measure(Vertex a, Vertex b) {
        // has to be >= haversine(a, b)
        return GraphUtils.haversineDist(a, b);
    }

    private static double measure(Vertex a, Vertex b, Map<Vertex, Neighbor> tree) {
        // TODO use distance saved in neighbor?
        if (a.equals(b)) {
            return 0;
        }
        Vertex parent = tree.get(b).v;
        return measure(b, parent) + measure(a, parent, tree);
    }

    private static Collection<Vertex> findLeaves(Vertex current, Map<Vertex, Neighbor> tree) {
        // TODO probably slow 
        //System.out.println("Findleaves start");
        Collection<Vertex> out = new ArrayList<>();
        Collection<Vertex> notLeaf = new HashSet<>();

        for (Vertex v: tree.keySet()) {
            
            Neighbor parent = tree.get(v);
            if (parent != null){
                notLeaf.add(parent.v);
            }
            
        }
        Collection<Vertex> leafs = new HashSet<>(tree.keySet());
        leafs.removeAll(notLeaf);
        //System.out.println("Findleaves middle");

        Collection<Vertex> leafsThroughCurrent = new HashSet<>();

        for (Vertex v: leafs){
            Neighbor parent = tree.get(v);
            while(parent != null) {
                if (parent.v.equals(current)){
                    leafsThroughCurrent.add(v);
                    break;
                }
                parent = tree.get(parent.v);
            }

        }
        //System.out.println("Findleaves end");

        return leafsThroughCurrent;
    }



    public static double calcReach(Vertex v, Map<Vertex, Neighbor> tree) {
        // First find the distance s -> v.
        // Then, for all leaves in the tree that look like s -> v -> t,
        //   calculate reach for v as max_{all t}( min(s->v, v->t) )

        Collection<Vertex> leavesThroughV = findLeaves(v, tree);

        double sToV = 0;
        Neighbor parent = tree.get(v);
        while (parent != null) {
            sToV += parent.distance;
            parent = tree.get(parent.v);
        }

        double maxReach = 0;
        // System.out.println(" leaves @ " + v + ": " + leavesThroughV.size());

        for (Vertex leaf : leavesThroughV) {
            // First, calculate distance from v -> leaf
            parent = tree.get(leaf);
            double vToLeaf = 0;
            while (! v.equals(parent.v)) {
                vToLeaf += parent.distance;
                parent = tree.get(parent.v);
            }
            vToLeaf += parent.distance;
            
            // System.out.println("  vtoleaf:   " + v + "->" + leaf + ": " + vToLeaf);

            maxReach = Math.max(maxReach, 
                Math.min(sToV, vToLeaf));
        }
        // System.out.println("    reach @ " + v + ":  " + maxReach + "   (stov=" + sToV + ")");
        return maxReach;
    }

    public static Map<Vertex, Neighbor> dijkstra(Graph g, Vertex start, Double epsilon){
        //System.out.println("Start djikstra with: " + start);
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
        Map<Vertex, Double> xprimeDist = new HashMap<>();

        Map<Vertex, Neighbor> pred = new HashMap<>();
        Map<Vertex, Neighbor> predTrue = new HashMap<>();

        Set<Vertex> leafTprime = new HashSet<>(); 
        Set<Vertex> leafT = new HashSet<>();

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        Map<Vertex, Double> noOfChildren = new HashMap<>();

        Set<Vertex> closed = new HashSet<>();

        pq.add(new Pair(start, 0));
        bestDist.put(start, 0.0);
        xprimeDist.put(start, 0.0);
        
        double bestxPrimeDist = 0.0;
        
        Map<Vertex, Double> shortest = new HashMap<>();

        while (pq.size() > 0) {

            boolean trueForAllLeaf = leafTprime.size() > 0 && bestxPrimeDist > 2 * epsilon ; // if there are any, assume it's tru and disprove in for loop
            
            /*System.out.println(bestDist);
            System.out.println(xprimeDist);
            System.out.println(leafT);
            System.out.println(leafTprime + "\n");*/
            try{
                //Thread.sleep(1000);
            } catch(Exception e) {
                e.printStackTrace();
            }
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
        
            bestxPrimeDist = xprimeDist.get(head.v);

            //System.out.println(head.v);

            /*if (xprimeDist.get(head.v) >= 2 * epsilon) {
                System.out.println(head.v);
                System.out.println("Break 2");
                break;
            }*/

            leafTprime.add(head.v);
            
            // remove parent - it's no longer a leaf!
            Neighbor parent = pred.get(head.v);
            predTrue.put(head.v, parent);
            if (parent != null) {
                leafTprime.remove(parent.v);
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
                        if (!head.v.equals(start)) {
                            xprimeDist.put(n.v, xprimeDist.get(head.v) + n.distance);
                        } else {
                            xprimeDist.put(n.v, xprimeDist.get(head.v) + 0.0);
                        }
                        bestDist.put(n.v, maybeNewBestDistance);
                        Neighbor oldParent = pred.get(n.v);
                        if (oldParent != null){
                            noOfChildren.put(oldParent.v, noOfChildren.get(oldParent.v) -1); // Should never give 0
                            if (noOfChildren.get(oldParent.v) == 0){
                                leafT.add(oldParent.v);
                            }
                        } 

                        pred.put(n.v, new Neighbor(head.v, n.distance));
                        noOfChildren.put(head.v, noOfChildren.getOrDefault(head.v, 0.0) + 1);

                        // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                        pq.add(new Pair(n.v, maybeNewBestDistance)); 
                    }
                });
        }
        //System.out.println("Dijkstra done");
        // TODO is it T or T' that should be returned here. It seems that i get ther ight results if i return T.
        return pred;
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

    public static void main(String[] args){
        //Graph graph = makeExampleGraph();
        //Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");
        Graph graph = makeSquareGraph(); 

        /*for (Vertex v: graph.getAllVertices()){
            for (Neighbor n: graph.getNeighboursOf(v)){boegebakken-intersections
                System.out.println(n);
            }
        }*/

        long timeBefore = System.currentTimeMillis();
        //double[] bs = new double[]{25,100, 250, 500, 1000, 2000, 5000, 10000};
        double[] bs = new double[]{200};
        Map<Vertex, Double> r = reach(graph, bs);
        long timeAfter = System.currentTimeMillis();

        
        System.out.println(r.keySet().size() + " reaches returned");
        System.out.println(r);
        System.out.println("Calculating reach took " + ((timeAfter-timeBefore)/1000) + " seconds");

        //saveReachArrayToFile("aarhus-silkeborg-reach3", r);

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
}   


class NamedVertex extends Vertex {
    private String name;
    public NamedVertex(String name) {
        super(name.hashCode(), name.hashCode()); // ugly hack: they have to be different to add more than one.
        this.name = name;
    }
    @Override
    public String toString() {
        return name;
    }
}


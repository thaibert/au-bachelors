package utility;

import java.util.*;

import javax.swing.plaf.basic.BasicListUI.ListDataHandler;
import javax.swing.plaf.metal.MetalIconFactory.TreeControlIcon;

import graph.*;

public class Reach {
    public static final double INF_DIST = Double.MAX_VALUE;

    // bs is a 
    public static Map<Vertex, Double> reach(Graph graph, double[] bs) {
        Graph graphPrime = graph;
        Graph graphInv = GraphUtils.invertGraph(graph);
        Map<Vertex, Double> bounds = new HashMap<>();

        Map<Vertex, Double> r = new HashMap<>();

        for (Vertex v : graph.getAllVertices()) {
            bounds.put(v, INF_DIST);
        }

        for (int i = 0; i < bs.length; i++) {
            // Iterate!


            Graph graphPrimeInv = GraphUtils.invertGraph(graphPrime);
            double c = 0; // todo should never be negative, right?

            Collection<Vertex> vertices = graph.getAllVertices();
            Collection<Vertex> verticesPrime = graphPrime.getAllVertices();

            Collection<Vertex> vMinusVPrime = vertices;
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
            
            //Form the graph H


            for (Vertex sPrime : verticesPrime) {
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
                Map<Vertex, Neighbor> tree = dijkstra(graph, sPrime, bs[i]);
                for (Vertex v : tree.keySet()) {
                    // compute r(v, T) TODO needed here?

                    Collection<Vertex> leaves = findLeaves(v, tree);

                    // Loop over all paths in T that yadda yadda yadda
                    for (Vertex tPrime : leaves) {
                        double rt = 0;

                        if (vMinusVPrime.contains(tPrime)) {
                            rt = bounds.get(tPrime);
                        }
                        
                        double rb = Math.min(g + measure(sPrime, v, tree), // todo give tree?
                                             rt + measure(v, tPrime, tree));  // todo give tree?
                        
                        if (rb > bounds.get(v)) {
                            bounds.put(v, rb);
                        }
                    }

                    if (calcReach(v, tree) > r.get(v)) {
                        r.put(v, calcReach(v, tree));
                    }

                }

            }

            for (Vertex v : verticesPrime) {
                if (r.get(v) >= bs[i]) {
                    bounds.put(v, INF_DIST);
                }
            }

            // Change G'!!
            Graph newGPrime = new SimpleGraph();
            // get new vertices
            for (Vertex v : verticesPrime) {
                if (bounds.get(v) == INF_DIST) {
                    newGPrime.addVertex(v);
                }
            }
            // get new edges
            for (Vertex v : newGPrime.getAllVertices()) {
                for (Neighbor n : graph.getNeighboursOf(v)) {
                    newGPrime.addEdge(v, n.v, n.distance);
                }
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
        Collection<Vertex> out = new ArrayList<>();
        Collection<Vertex> notLeaf = new HashSet<>();

        for (Vertex v: tree.keySet()) {
            
            Neighbor parent = tree.get(v);
            notLeaf.add(parent.v);
            
        }
        Collection<Vertex> leafs = tree.keySet();
        leafs.removeAll(notLeaf);
        
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
        return leafsThroughCurrent;

    }



    public static double calcReach(Vertex v, Map<Vertex, Neighbor> tree) {
        // TODO
        return 0;
    }

    public static Map<Vertex, Neighbor> dijkstra(Graph g, Vertex start, Double epsilon){

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

        Set<Vertex> leafTprime = new HashSet<>(); 
        Set<Vertex> leafT = new HashSet<>();

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        pq.add(new Pair(start, 0));
        xprimeDist.put(start, 0.0);
        
        Map<Vertex, Double> shortest = new HashMap<>();

        while (pq.size() > 0) {

            boolean trueForAllLeaf = true;
            for (Vertex v: leafTprime) {
                if (!(leafT.contains(v) || xprimeDist.get(v) >= 2 * epsilon)){
                    trueForAllLeaf = false;
                    break;
                }
            }
            if (trueForAllLeaf){
                break;
            }

            Pair head = pq.poll();

            leafTprime.add(head.v);
            leafTprime.remove(pred.get(head.v));

            g.getNeighboursOf(head.v)
                .forEach(n -> {
                    // RELAX

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
                        pred.put(n.v, new Neighbor(head.v, n.distance));

                        // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                        pq.add(new Pair(n.v, maybeNewBestDistance)); 
                    }
                });
        }
        return pred;
    }



    public static void main(String[] args){
        Graph graph = new SimpleGraph();
 
        Vertex a = new Vertex(2,5); // a
        Vertex b = new Vertex(2,4); // b
        Vertex c = new Vertex(4,4); // c
        Vertex d = new Vertex(1,3); // d
        Vertex e = new Vertex(3,3); // e
        Vertex f = new Vertex(2,4); // f
        Vertex g = new Vertex(2,2); // g
        Vertex s = new Vertex(2,6); // s
        Vertex t = new Vertex(2,1); // t

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

        double[] bs = new double[]{5,100};
        Map<Vertex, Double> r = reach(graph, bs);
        
        System.out.println(r);

    }

}   

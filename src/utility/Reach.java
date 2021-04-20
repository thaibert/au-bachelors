package utility;

import java.util.*;

import javax.swing.plaf.basic.BasicListUI.ListDataHandler;
import javax.swing.plaf.metal.MetalIconFactory.TreeControlIcon;

import graph.*;

public class Reach {
    public static final double INF_DIST = Double.MAX_VALUE;

    // bs is a 
    public static Graph reach(Graph graph, int[] bs) {
        Graph graphPrime = graph;
        Graph graphInv = GraphUtils.invertGraph(graph);
        Map<Vertex, Double> bounds = new HashMap<>();

        for (Vertex v : graph.getAllVertices()) {
            bounds.put(v, INF_DIST);
        }

        for (int i = 0; i < bs.length; i++) {
            // Iterate!


            Graph graphPrimeInv = GraphUtils.invertGraph(graphPrime);
            Map<Vertex, Double> r = new HashMap<>();
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
                Map<Vertex, TreeNode> tree = generateTree();
                for (Vertex v : tree.keySet()) {
                    // compute r(v, T)
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

        return null;
    }


    private static double measure(Vertex a, Vertex b) {
        // has to be >= haversine(a, b)
        return GraphUtils.haversineDist(a, b);
    }

    private static double measure(Vertex a, Vertex b, Map<Vertex, TreeNode> tree) {
        if (a.equals(b)) {
            return 0;
        }
        Vertex parent = tree.get(b).parent;
        return measure(b, parent) + measure(a, parent, tree);
    }

    private static Collection<Vertex> findLeaves(Vertex current, Map<Vertex, TreeNode> tree) {
        Collection<Vertex> out = new ArrayList<>();
        TreeNode treeNode = tree.get(current);
        if (treeNode.children.size() == 0) {
            // leaf!
            out.add(current);
        } else {
            // not leaf - continue!
            for (Vertex child : treeNode.children) {
                out.addAll(findLeaves(child, tree));
            }
        }
        return out;
    }


    class TreeNode {
        final Vertex parent;
        final Vertex me;
        final Collection<Vertex> children;
        
        TreeNode(Vertex parent, Vertex me, Collection<Vertex> children) {
            this.parent = parent;
            this.me = me;
            this.children = children;
        }
    }

}   

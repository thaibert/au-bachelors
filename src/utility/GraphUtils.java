package utility;

import graph.*;
import java.util.*;

public class GraphUtils {

    public static Graph invertGraph(Graph g) {
        Collection<Vertex> vertices = g.getAllVertices();

        Graph inverted = new SimpleGraph(); // TODO if we get more graph types?
        for (Vertex v : vertices) {
            inverted.addVertex(v);
        }

        for (Vertex v : vertices) {
            for (Neighbor n : g.getNeighboursOf(v)) {
                inverted.addEdge(n.v, v, n.distance);
            }
        }

        return inverted;
    }

    public static Vertex findNearestVertex(Graph g, double lat, double lon) {
        Collection<Vertex> vertices = g.getAllVertices();
        Vertex coords = new Vertex(lat, lon);

        Vertex bestSoFar = null;
        double bestDist = Double.MAX_VALUE;
        for (Vertex v : vertices) {
            if (haversineDist(v, coords) < bestDist) {
                bestSoFar = v;
                bestDist = haversineDist(v, coords);
            }
        }
        return bestSoFar;
    }

    public static Vertex findNearestVertex(Graph g, Vertex v) {
        Collection<Vertex> vertices = g.getAllVertices();

        Vertex bestSoFar = null;
        double bestDist = Double.MAX_VALUE;
        for (Vertex u : vertices) {
            if (haversineDist(u, v) < bestDist) {
                bestSoFar = u;
                bestDist = haversineDist(u, v);
            }
        }
        return bestSoFar;
    }

    public static double haversineDist(Vertex a, Vertex b) {
        double radius = 6371000; // ~6371 km
        double DEGREES_TO_RADIANS = Math.PI / 360;

        double phi_1 = a.getLatitude() * DEGREES_TO_RADIANS;
        double phi_2 = b.getLatitude() * DEGREES_TO_RADIANS;
        double lambda_1 = a.getLongitude() * DEGREES_TO_RADIANS;
        double lambda_2 = b.getLongitude() * DEGREES_TO_RADIANS;
        double dist = 2 * radius * Math.asin(
            Math.sqrt(
                hav(phi_1 - phi_2) 
              + Math.cos(phi_1)
              * Math.cos(phi_2)
              * hav(lambda_1-lambda_2)
            )
        );
        return dist;
    }
    private static double hav(double number) {
        return (1.0 - Math.cos(number)) / 2.0;
    }

    public static Vertex pickRandomVertex(Graph g) {
        Collection<Vertex> vertices = g.getAllVertices();
        return vertices.stream()
            .skip((int) (vertices.size() * Math.random()))
            .findFirst()
            .get();
    }

    public static double pathDistance(List<Vertex> path) {
        if (path.size() < 2) {
            return 0;
        }

        double sum = 0;
        for (int i = 1; i < path.size(); i++) {
            sum += haversineDist(path.get(i-1), path.get(i));
        }
        return sum;
    }

    public static Graph pruneGraphOfChains(Graph g) {
        System.out.println("pruning graph");
        Graph g_inv = invertGraph(g);
        Collection<Vertex> vertices = g.getAllVertices();
        Iterator<Vertex> it = vertices.iterator();

        System.out.println("  inverted graph, starting now");

        Set<Vertex> removed = new HashSet<>();

        Graph newGraph = new SimpleGraph();

        int iterations = 0;
        while (it.hasNext()) {
            iterations++;
            if (iterations % 100000 == 0) {
                System.out.print(".");
            }


            Vertex curr = it.next();
            
            if (removed.contains(curr)) {
                // We have pruned this node - loop again
                continue;
            }

            Collection<Neighbor> incoming = g_inv.getNeighboursOf(curr);
            Collection<Neighbor> outgoing = g.getNeighboursOf(curr);

            boolean isMiddleLink = incoming.size() == 1 
                                && outgoing.size() == 1
                                && ! incoming.equals(outgoing);
            
            if (! isMiddleLink) {
                // A normal node


            } else {
                // We're a middle link in a chain!
                Neighbor in = incoming.iterator().next();
                Neighbor out = outgoing.iterator().next();

                removed.add(curr);

                // Find start of chain
                double distBack = 0;
                Neighbor potentialLinkBefore = in; // neighbor on inverted graph
                int links = 0;
                while (g_inv.getNeighboursOf(potentialLinkBefore.v).size() == 1
                    && g.getNeighboursOf(potentialLinkBefore.v).size() == 1
                    && ! g_inv.getNeighboursOf(potentialLinkBefore.v).equals(g.getNeighboursOf(potentialLinkBefore.v))
                    && ! removed.contains(potentialLinkBefore.v)) {
                        // Looking at a node with 1 in, 1 out, and in != out.
                        // found another middle link
                        links++;
                        if (links > 1000) {
                            System.out.println(potentialLinkBefore.v); // Todo remove debug code
                        }
                        distBack += potentialLinkBefore.distance;
                        removed.add(potentialLinkBefore.v);
                        potentialLinkBefore = g_inv.getNeighboursOf(potentialLinkBefore.v).iterator().next();
                }
                distBack += potentialLinkBefore.distance;
                Neighbor chainStart = potentialLinkBefore;

                // Find end of chain
                double distForward = 0;
                Neighbor potentialLinkAfter = out; // neighbor on normal graph
                links = 0;
                while (g_inv.getNeighboursOf(potentialLinkAfter.v).size() == 1
                    && g.getNeighboursOf(potentialLinkAfter.v).size() == 1
                    && ! g_inv.getNeighboursOf(potentialLinkAfter.v).equals(g.getNeighboursOf(potentialLinkAfter.v))
                    && ! removed.contains(potentialLinkAfter.v)) {
                        // Looking at a node with 1 in, 1 out, and in != out.
                        // found another middle link
                        links++;
                        if (links > 1000) {
                            System.out.println(links); // todo remove debug code
                        }
                        distForward += potentialLinkAfter.distance;
                        removed.add(potentialLinkAfter.v);
                        potentialLinkAfter = g.getNeighboursOf(potentialLinkAfter.v).iterator().next();
                }
                distForward += potentialLinkAfter.distance;
                Neighbor chainEnd = potentialLinkAfter;

                double chainDist = distBack + distForward;

                // System.out.println("chain: " + chainStart.v + "-->" + chainEnd.v + "    dist: " + chainDist);
                if (chainDist < haversineDist(chainStart.v, chainEnd.v)) {
                    System.out.println(chainDist + " < " + haversineDist(chainStart.v, chainEnd.v));
                    System.out.println("  " + chainStart.v + "-->" + chainEnd.v);
                }

                newGraph.addVertex(chainStart.v);
                newGraph.addVertex(chainEnd.v);
                newGraph.addEdge(chainStart.v, chainEnd.v, chainDist);
            }
            // todo undirected chain: check if incoming == outgoing != empty?
        }

        // All nodes that were removed have been marked.
        // Now copy everything else over
        it = vertices.iterator();
        System.out.println("  copying intersections over");

        while (it.hasNext()) {
            Vertex curr = it.next();

            if (removed.contains(curr)) {
                continue;
            }

            newGraph.addVertex(curr);
            for (Neighbor n : g.getNeighboursOf(curr)) {
                if (! removed.contains(n.v)) {
                    newGraph.addVertex(n.v);
                    newGraph.addEdge(curr, n.v, n.distance);
                }
            }

        }



        System.out.println("\nnodes: " + g.getAllVertices().size());
        System.out.println("pruned: " + removed.size());
        System.out.println("new graph: " + newGraph.getAllVertices().size());

        return newGraph;
    }
    
}

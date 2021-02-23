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
            if (dist(v, coords) < bestDist) {
                bestSoFar = v;
                bestDist = dist(v, coords);
            }
        }
        return bestSoFar;
    }

    public static double dist(Vertex a, Vertex b) {
        double radius = 6371000; // Radius of the Earth
        double dist = 2 * radius * Math.asin(Math.sqrt(hav(a.getLatitude() - b.getLatitude()) + Math.cos(a.getLatitude()) * Math.cos(b.getLatitude())*hav(a.getLongitude()-b.getLongitude())));
        return dist;
    }

    private static double hav(double number) {
        return (1-Math.cos(number))/2;
    }
    
}

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
    
}

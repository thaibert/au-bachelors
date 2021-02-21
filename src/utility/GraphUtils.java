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
    
}

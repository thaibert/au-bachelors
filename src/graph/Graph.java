package graph;

import java.util.*;

public interface Graph {
    void addVertex(Vertex v);
    void addEdge(Vertex u, Vertex v, double distance);
    Collection<Vertex> getAllVertices();
    Collection<Neighbor> getNeighboursOf(Vertex v);

    boolean saveToFile(String filename);
    boolean loadFromFile(String filename);
}

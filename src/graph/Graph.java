package graph;

import java.util.*;

public interface Graph {
    void addVertex(Vertex v);
    void addEdge(Vertex u, Vertex v);
    Collection<Vertex> getAllVertices();
    Collection<Vertex> getNeighboursOf(Vertex v);

    boolean saveToFile(String filename);
    boolean loadFromFile(String filename);
}

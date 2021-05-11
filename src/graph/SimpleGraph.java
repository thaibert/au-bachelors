package graph;

import java.io.Serializable;
import java.util.*;
import java.util.stream.*;

public class SimpleGraph implements Graph, Serializable {
    
    private Map<Vertex, Collection<Neighbor>> neighborsMap;

    public SimpleGraph(){
        this.neighborsMap = new HashMap<>();
    }

    @Override
    public void addVertex(Vertex v) {
        if (neighborsMap.keySet().contains(v)) {
            // System.out.println("--> Tried adding already existing vertex");
        }
        neighborsMap.putIfAbsent(v, new ArrayList<>());
    }

    @Override
    public void removeVertex(Vertex v) {
        if (! neighborsMap.keySet().contains(v)) {
            System.out.println("Removing non-existent vertex");
        }
        neighborsMap.remove(v);
    }

    //** Add an edge from u -> v */
    @Override
    public void addEdge(Vertex u, Vertex v, double distance) {
        Neighbor n = new Neighbor(v, distance);
        Collection<Neighbor> neighbors = neighborsMap.getOrDefault(u, new HashSet<>());
        neighbors.add(n);
    }

    @Override
    public void removeEdge(Vertex u, Vertex v) {
        Collection<Neighbor> neighbors = neighborsMap.get(u);
        if (neighbors == null) {
            System.out.println("null neighbors @ " + u + "->" + v);
            return;
        }
        Neighbor correctPair = null;
        for (Neighbor n : neighbors) {
            if (! v.equals(n.v)) {
                continue;
            }
            // We found v!
            correctPair = n;
        }
        neighbors.remove(correctPair);
        neighborsMap.put(u, neighbors);
    }

    @Override
    public Collection<Vertex> getAllVertices() {
        return neighborsMap.keySet();
    }

    @Override
    public Collection<Neighbor> getNeighboursOf(Vertex v) {
        return neighborsMap.getOrDefault(v, new ArrayList<>());
    }

    @Override
    public boolean saveToFile(String filename) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean loadFromFile(String filename) {
        // TODO Auto-generated method stub
        return false;
    }

}



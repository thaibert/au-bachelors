package graph;

import java.util.*;

public class SimpleGraph implements Graph {
    
    private Map<Vertex, Collection<Vertex>> neighborsMap;

    public SimpleGraph(){
        this.neighborsMap = new HashMap<Vertex, Collection<Vertex>>();
    }

    @Override
    public void addVertex(Vertex v) {
        if (neighborsMap.keySet().contains(v)) {
            throw new RuntimeException("--> Tried adding already existing vertex");
        }
        neighborsMap.putIfAbsent(v, new ArrayList<>());
    }

    //** Add an edge from u -> v */
    @Override
    public void addEdge(Vertex u, Vertex v) {
        neighborsMap.get(u).add(v);
    }

    @Override
    public Collection<Vertex> getAllVertices() {
        return neighborsMap.keySet();
    }

    @Override
    public Collection<Vertex> getNeighboursOf(Vertex v) {
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

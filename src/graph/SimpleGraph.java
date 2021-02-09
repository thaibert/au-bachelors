package graph;

import java.util.*;
import java.util.stream.*;

public class SimpleGraph implements Graph {
    
    private Map<Vertex, Collection<Neighbor>> neighborsMap;

    public SimpleGraph(){
        this.neighborsMap = new HashMap<>();
    }

    @Override
    public void addVertex(Vertex v) {
        if (neighborsMap.keySet().contains(v)) {
            System.out.println("--> Tried adding already existing vertex");
        }
        neighborsMap.putIfAbsent(v, new ArrayList<>());
    }

    //** Add an edge from u -> v */
    @Override
    public void addEdge(Vertex u, Vertex v, double distance) {
        // TODO USE DISTANCE!
        Neighbor n = new Neighbor(v, distance);
        neighborsMap.get(u).add(n);
    }

    @Override
    public Collection<Vertex> getAllVertices() {
        return neighborsMap.keySet();
    }

    @Override
    public Collection<Vertex> getNeighboursOf(Vertex v) {
        Collection<Neighbor> neighbors = neighborsMap.getOrDefault(v, new ArrayList<>());
        return neighbors.stream().map( n -> n.v).collect(Collectors.toSet());
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


    private class Neighbor {
        public Vertex v;
        public double distance;

        public Neighbor(Vertex v, double distance) {
            this.v = v;
            this.distance = distance;
        }
    }
}



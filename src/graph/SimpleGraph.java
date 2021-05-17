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
        neighborsMap.putIfAbsent(v, new HashSet<>());
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
        Neighbor newNeighbor = new Neighbor(v, distance);
        Collection<Neighbor> neighbors = neighborsMap.getOrDefault(u, new HashSet<>());

        // Find those with smaller distance that are equal to v
        Collection<Neighbor> existingSmallerThan = neighbors.stream()
            .filter(n -> n.distance < distance)
            .filter(n -> v.equals(n.v))
            .collect(Collectors.toCollection(HashSet::new));

        // If there are no existing smaller, don't insert the new (it'll be bigger)
        if (existingSmallerThan.size() == 0) {
            // Remove old v, insert new (smaller) u-->v
            neighbors = neighbors.stream()
                .filter(n -> ! v.equals(n.v))
                .collect(Collectors.toCollection(HashSet::new));
            neighbors.add(newNeighbor);
            neighborsMap.put(u, neighbors);
        }
    }

    @Override
    public void removeEdge(Vertex u, Vertex v) {
        Collection<Neighbor> neighbors = neighborsMap.get(u);
        if (neighbors == null) {
            System.out.println("null neighbors @ " + u + "->" + v);
            return;
        }
        Iterator<Neighbor> it = neighbors.iterator();
        while (it.hasNext()) {
            Neighbor n = it.next();
            if (v.equals(n.v)) {
                // We found v!
                it.remove();
            }
        }
        neighborsMap.put(u, neighbors);
    }

    @Override
    public Collection<Vertex> getAllVertices() {
        return neighborsMap.keySet();
    }

    @Override
    public Collection<Neighbor> getNeighboursOf(Vertex v) {
        return neighborsMap.getOrDefault(v, new HashSet<>());
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



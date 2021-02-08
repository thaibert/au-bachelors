package graph;

import java.util.*;

public class SimpleGraph implements Graph {
    
    public Map<Vertex, List<Vertex>> neighborsMap = new HashMap<>();


    public SimpleGraph(){
        this.neighborsMap = new HashMap<Vertex, List<Vertex>>();
    }

    public void addVertex(double longitude, double latitude) {
        Vertex v = new Vertex(longitude, latitude);
        // For now i'll assume we never have 2 vertexes at the same long/lat, as that is measured down to 11 mm ish
        neighborsMap.putIfAbsent(v, new ArrayList<>());
    }

    /*
    * Input int directed gives us a way to add roads, that can only be traversed in one direction
    * I've randomly come up with the following "idea" for it
    * 1 = Bidirectional: v1 -> v2 and v2 -> v1
    * 2 = v1 -> v2
    * 3 = v2 -> v1 
    */
    public void addEdge(double long1, double lat1, double long2, double lat2, int directed) {
        Vertex v1 = new Vertex(long1, lat1);
        Vertex v2 = new Vertex(long2, lat2);

        if (directed == 1) {
            neighborsMap.get(v1).add(v2);
            neighborsMap.get(v2).add(v1);
        } else if (directed == 2){
            neighborsMap.get(v1).add(v2);
        } else if (directed == 3){
            neighborsMap.get(v2).add(v1);
        } else {
            throw new RuntimeException("Wrong code for adding edge");
        }

    }

    public List<Vertex> getNeighbors(double longitude, double latitude){
        Vertex v = new Vertex(longitude, latitude);
        return neighborsMap.get(v);
    }

}

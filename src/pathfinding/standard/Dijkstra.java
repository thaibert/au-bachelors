package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;
import java.util.*;

public class Dijkstra implements PathfindingAlgo {
 
    /**
     * Implemented based on the description in the book
     * 
     * Talks about directed graph, but does it even matter, it's never going to be better to travel the same edge twice?
     * I think at least
    */

    public List<Vertex> shortestPath(Graph G, Vertex a, Vertex b){
        





        return new ArrayList<>();
    }

    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("intersections.csv");

        Vertex a = new Vertex(1,1);
        Vertex b = new Vertex(2,2);

        Dijkstra d = new Dijkstra();
        List<Vertex> shortest = d.shortestPath(a, b);

        GraphVisualiser vis = new GraphVisualiser(graph);
        vis.drawPath(shortest);
        vis.visualize();
    }


}

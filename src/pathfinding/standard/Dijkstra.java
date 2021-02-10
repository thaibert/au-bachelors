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
        //  Psudokode from the book
        //  Initialize-Single-Source(G, s) (s = source)
        //  S = Ø
        //  Q = G.V
        //  While Q != Ø
        //      u = Extract-Min(Q)    
        //      S = S U {u}
        //      for each vertex v in G.Adj[u]
        //          Relax(u,v,w)   





        return new ArrayList<>();
    }


    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("intersections.csv");

        Vertex a = new Vertex(56.1634686,10.1722176); // Viborgvej
        Vertex b = new Vertex(56.1828308,10.2037825); // O2/Randersvej

        Dijkstra d = new Dijkstra();
        List<Vertex> shortest = d.shortestPath(graph, a, b);

        shortest.add(a);
        shortest.add(b);

        System.out.println(shortest);

        GraphVisualiser vis = new GraphVisualiser(graph);
        vis.drawPath(shortest);
        vis.visualize();
    }


}

package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;
import java.util.*;

public class Dijkstra implements PathfindingAlgo {
    private final double INF_DIST = Double.MAX_VALUE;
 
    /**
     * Implemented based on the description in the book
     * 
     * Talks about directed graph, but does it even matter, it's never going to be better to travel the same edge twice?
     * I think at least
    */
    private Map<Vertex, Double> dist;
    private Map<Vertex, Vertex> pred; // S in the algo is pred.keySet()

    public List<Vertex> shortestPath(Graph g, Vertex a, Vertex b){
        System.out.println("--> Running Dijkstra");
        //  Psudokode from the book
        //  Initialize-Single-Source(G, s) (s = source)
        //  S = Ø
        //  Q = G.V
        //  While Q != Ø
        //      u = Extract-Min(Q)    
        //      S = S U {u}
        //      for each vertex v in G.Adj[u]
        //          Relax(u,v,w)   


        dist = new HashMap<>();
        pred = new HashMap<>();

        g.getAllVertices().stream().forEach( v -> dist.put(v, INF_DIST) );
        dist.put(a, 0.0);


        // Main algo
        Set<Vertex> vertices = new HashSet<>(g.getAllVertices());
        Queue<Vertex> pq = new PriorityQueue<>(vertices.size(), new DistComparator());
        pq.addAll(vertices);

        System.out.println("vertices: " + vertices.size() + ",    pq: " + pq.size());

        int num = 0;        
        while (pq.size() > 0) {
            num++;
            if (num % 1000 == 0) {
                System.out.println("  --> " + num + ",   pq size: " + pq.size());
            }
            Vertex u = pq.poll();
            if (u.equals(b)) {
                break;
            }
            pq.remove(u);
            g.getNeighboursOf(u).forEach(n -> {
                relax(u, n);
            });

            Vertex[] allElements = pq.toArray(new Vertex[]{});
            pq = new PriorityQueue<>(pq.size()+1, new DistComparator());
            pq.addAll(Arrays.asList(allElements));
        }

        // Get out the shortest path
        System.out.println("    --> backtracking solution");
        List<Vertex> out = new ArrayList<>();

        Vertex temp = b;
        int i = 0;
        while (! a.equals(temp)) {
            out.add(temp);
            temp = pred.get(temp);
            i++;
        }
        out.add(a);
        return out;
    }

    private void relax(Vertex u, Neighbor n) {
        if (dist.get(n.v) > dist.get(u) + n.distance) {
            dist.put(n.v, dist.get(u) + n.distance);
            pred.put(n.v, u);
        }
    }






// 1  function Dijkstra(Graph, source):
//         create vertex set Q
//  4
//  5      for each vertex v in Graph:            
//  6          dist[v] ← INFINITY                 
//  7          prev[v] ← UNDEFINED                
//  8          add v to Q                     
//  9      dist[source] ← 0                       
// 10     
// 11      while Q is not empty:
// 12          u ← vertex in Q with min dist[u]   
// 13                                             
// 14          remove u from Q
// 15         
// 16          for each neighbor v of u:           // only v that are still in Q
//                 if v == goal return // maybe relax first?
// 17              alt ← dist[u] + length(u, v)
// 18              if alt < dist[v]:              
// 19                  dist[v] ← alt
// 20                  prev[v] ← u
// 21
// 22      return dist[], prev[]





    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("intersections.csv");

        Vertex a = new Vertex(56.1634686,10.1722176); // Viborgvej
        Vertex b = new Vertex(56.1828308,10.2037825); // O2/Randersvej

        Dijkstra d = new Dijkstra();
        List<Vertex> shortest = d.shortestPath(graph, a, b);

        GraphVisualiser vis = new GraphVisualiser(graph);
        vis.drawPath(shortest);
        vis.visualize();
    }


    class DistComparator implements Comparator<Vertex> {

        @Override
        public int compare(Vertex a, Vertex b) {
            return Double.compare(dist.getOrDefault(a, INF_DIST), dist.getOrDefault(b, INF_DIST));
        }
    }

}

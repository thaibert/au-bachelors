package pathfinding.standard;

import pathfinding.framework.*;
import graph.*;
import java.util.*;

public class BidirectionalDijkstra implements PathfindingAlgo {

    @Override // TODO problems in taking 2 graphs as input :/
    public Solution shortestPath(Graph graph, Vertex start, Vertex goal) {




        // Main source of inspiration:
        // https://www.ucl.ac.uk/~ucahmto/math/2020/05/30/bidirectional-dijkstra.html



        
        // TODO Auto-generated method stub
        return null;
    }

    
    public static void main(String[] args) {
        // We need to be able to utilize the inverted graph, so for now we ignore space efficiency and just create 2 graphs
        Graph graph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv", false);
        Graph invertedGraph = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv", true);

        // Vertex a = new Vertex(56.1634686,10.1722176); // Viborgvej
        Vertex b = new Vertex(56.1723636,9.5538336); // Silkeborg
        Vertex a = new Vertex(56.1828308,10.2037825); // O2/Randersvej

        BidirectionalDijkstra d = new BidirectionalDijkstra();
        Solution solution = d.shortestPath(graph, a, b);

        GraphVisualiser vis = new GraphVisualiser(graph, BoundingBox.AarhusSilkeborg);
        vis.drawPath(solution.getShortestPath());
        vis.drawVisited(solution.getVisited());
        vis.visualize();
    }

    class DistComparator implements Comparator<Pair> {

        public long comparisons = 0;

        @Override
        public int compare(Pair p1, Pair p2) {
            this.comparisons++;
            return Double.compare(p1.dist, p2.dist);
        }

        public long getComparisons() {
            return this.comparisons;
        }
        
    }

    class Pair {
        public final Vertex v;
        public final double dist;

        public Pair(Vertex v, double dist) {
            this.v = v;
            this.dist = dist;
        }
    }

}

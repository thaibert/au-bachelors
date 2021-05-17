package testing;

import pathfinding.framework.*;
import pathfinding.standard.*;
import utility.*;
import graph.*;

import java.io.*;
import java.util.*;


public class TestPruningChains {
    public static void main(String[] args) {
        String csv = "aarhus-silkeborg-all-roads.csv";
        // String csv = "denmark-latest-roads.csv";
        BoundingBox bbox = BoundingBox.AarhusSilkeborg;
        int runs = (int) 1e6;

        Graph graph = GraphPopulator.populateGraph(csv);

        Graph pruned = GraphPopulator.populateGraph(csv);
        pruned = GraphUtils.pruneChains(pruned);

        System.out.println(graph.getAllVertices().size() + " nodes in original graph");
        System.out.println(pruned.getAllVertices().size() + " nodes in pruned graph");
        
        Astar d = null;
        Astar d2 = null;
        Solution solution = null;
        Solution solution2 = null;

        d = new Astar(graph);
        d2 = new Astar(pruned);

        System.out.println("--> Testing " + csv);
        System.out.println("   (Doing " + runs + " runs)");

        PrintStream originalStream = System.out;
        PrintStream noopStream = new PrintStream(new OutputStream(){
            public void write(int b) {
                // NO-OP
            }
        });
        System.setOut(noopStream);

        for (int i = 0; i < runs; i++) {
            if (i % 50 == 0) {
                System.setOut(originalStream);
                System.out.println(i + " runs done");
                System.setOut(noopStream);
            }
            Vertex a = GraphUtils.pickRandomVertex(pruned);
            Vertex b = GraphUtils.pickRandomVertex(pruned);

            System.out.println("Original:");
            solution = d.shortestPath(a, b);

            System.out.println("Pruned:");
            solution2 = d2.shortestPath(a, b);

            double lengthOriginal = GraphUtils.realLength(graph, solution.getShortestPath());
            double lengthPruned = GraphUtils.realLength(pruned, solution2.getShortestPath());
            // Assume  delta  =  1 * 10^-3  =  0.001m  =  1mm
            boolean isPlusMinusDelta = Math.abs(lengthOriginal - lengthPruned) <= 1e-3;

            List<Vertex> originalPath = new ArrayList<>(solution.getShortestPath());
            List<Vertex> prunedPath = new ArrayList<>(solution2.getShortestPath());
            boolean isSubsetOfLongPath = originalPath.containsAll(prunedPath);

            if (! isPlusMinusDelta || ! isSubsetOfLongPath ) {
                System.setOut(originalStream);

                System.out.println(i + ":  " + a + "-->" + b);
                System.out.println("  original: " + lengthOriginal);
                System.out.println("  pruned:   " + lengthPruned);

                // System.out.println("original path: " + solution.getShortestPath());
                // System.out.println("pruned path:   " + solution2.getShortestPath());

                System.setOut(noopStream);


                // DRAW!
                GraphVisualiser vis = new GraphVisualiser(graph, bbox);
                vis.drawPath(solution.getShortestPath());
                vis.drawVisited(solution.getVisited());
                vis.visualize("A* " + i);
        
        
                GraphVisualiser vis2 = new GraphVisualiser(pruned, bbox);
                vis2.drawPath(solution2.getShortestPath());
                vis2.drawVisited(solution2.getVisited());
                vis2.visualize("A* pruned " + i);
                break;
            } else {
                // It's correct; collect data!

            }

            System.out.println();
        }
        System.setOut(originalStream);
    }
}

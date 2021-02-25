package testing;

import graph.*;
import pathfinding.framework.*;
import pathfinding.standard.*;

import java.io.*;
import java.util.*;


public class TestDijkstras {

    static long totalTimeTraditional = 0;
    static long totalTimeOurs = 0;

    static long start = 0;
    static long stop = 0;

    static void testTraditionalDijkstraVsOurs(Graph g) {

        // Disable printing while running
        PrintStream originalStream = System.out;

        PrintStream noopStream = new PrintStream(new OutputStream(){
            public void write(int b) {
                // NO-OP
            }
        });
        System.setOut(noopStream);

        Vertex a = pickRandomVertex(g);
        Vertex b = pickRandomVertex(g);

        
        PathfindingAlgo traditional = new DijkstraTraditional();
        PathfindingAlgo ours = new Dijkstra();

        Solution solutionTraditional, solutionOurs;
        try {
            start = System.nanoTime();
            solutionTraditional = traditional.shortestPath(g, a, b);
            stop = System.nanoTime();
            totalTimeTraditional += (stop - start);
            
            start = System.nanoTime();
            solutionOurs = ours.shortestPath(g, a, b);
            stop = System.nanoTime();
            totalTimeOurs += (stop - start);

        } catch(Exception e) {
            System.setOut(originalStream);
            System.out.println(e.getMessage());
            solutionTraditional = solutionOurs = new Solution(new ArrayList<>(), new ArrayList<>());
        }
        System.setOut(originalStream);
        System.out.print("  " + a + "  ->  " + b);

        boolean equalSolutions = solutionTraditional.getShortestPath().equals(solutionOurs.getShortestPath());
        // assert !equalSolutions;
        // TODO wrap so only on when assertions are on
        if (!equalSolutions) {
            System.out.println(" not equal!");

            GraphVisualiser vis1 = new GraphVisualiser(g, BoundingBox.AarhusSilkeborg);
            vis1.drawPath(solutionOurs.getShortestPath());
            vis1.visualize();

            GraphVisualiser vis2 = new GraphVisualiser(g, BoundingBox.AarhusSilkeborg);
            vis2.drawPath(solutionTraditional.getShortestPath());
            vis2.visualize();

            try{
            Thread.sleep(400000);
            } catch(InterruptedException e) {
                System.out.println(e);
            }
        }
        System.out.println();

    }

    static Vertex pickRandomVertex(Graph g) {
        Collection<Vertex> vertices = g.getAllVertices();
        return vertices.stream()
            .skip((int) (vertices.size() * Math.random()))
            .findFirst()
            .get();
    }

    public static void main(String[] args) {
        Graph g = GraphPopulator.populateGraph("denmark-intersections.csv");

        int runs = 1000;

        for (int i = 0; i < runs; i++) {
            System.out.print(" -> " + i);
            try {

                testTraditionalDijkstraVsOurs(g);

            } catch(Exception e) {
                 System.out.println(" failed (exception)");
            } catch(AssertionError e) {
                System.out.println(" failed (assert)");
            }
        }
        System.out.println("[*] Done!");
        System.out.println("     Total runs: " + runs);
        System.out.printf("     Total time taken for \"traditional\"       %8.3f seconds \n", (double) totalTimeTraditional/1000000000);
        System.out.printf("     Total time taken for ours                %8.3f seconds \n", (double) totalTimeOurs/1000000000);

        System.out.printf("     Average time taken for \"traditional\"     %8.3f seconds \n", (double) totalTimeTraditional/runs/1000000000);
        System.out.printf("     Average time taken for ours              %8.3f seconds \n", (double) totalTimeOurs/runs/1000000000);

    }
}

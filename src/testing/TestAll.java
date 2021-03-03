package testing;

import graph.*;
import utility.*;
import pathfinding.framework.*;
import pathfinding.standard.*;

import java.io.*;
import java.util.*;

public class TestAll {

    static PathfindingAlgo traditional;
    static PathfindingAlgo ours;
    static PathfindingAlgo astar;
    static PathfindingAlgo bidirectional;

    static long totalTimeTraditional = 0;
    static long totalTimeOurs = 0;
    static long totalTimeBidirectional = 0;
    static long totalTimeAStar = 0;

    static long start = 0;
    static long stop = 0;

    static long totalExpandedTraditional = 0;
    static long totalExpandedOurs = 0;
    static long totalExpandedBidirectional = 0;
    static long totalExpandedAStar = 0;

    static void testAllShortestPath(Graph g){
        // TODO in "actual" runs, we should comment in out in files, as it still takes time?
        // Disable printing while running 
        PrintStream originalStream = System.out;

        PrintStream noopStream = new PrintStream(new OutputStream(){
            public void write(int b) {
                // NO-OP
            }
        });
        System.setOut(noopStream);

        Vertex a = GraphUtils.pickRandomVertex(g);
        Vertex b = GraphUtils.pickRandomVertex(g);

        System.setOut(originalStream);
        System.out.print("  " + a + "  ->  " + b);
        System.setOut(noopStream);


        Solution solutionTraditional, solutionOurs, solutionAStar, solutionBidirectional;
        try {
            start = System.nanoTime();
            solutionTraditional = traditional.shortestPath(a, b);
            stop = System.nanoTime();
            totalTimeTraditional += (stop - start);
            
            start = System.nanoTime();
            solutionOurs = ours.shortestPath(a, b);
            stop = System.nanoTime();
            totalTimeOurs += (stop - start);

            start = System.nanoTime();
            solutionAStar = astar.shortestPath(a, b);
            stop = System.nanoTime();
            totalTimeAStar += (stop - start);

            start = System.nanoTime();
            solutionBidirectional = bidirectional.shortestPath(a, b);
            stop = System.nanoTime();
            totalTimeBidirectional += (stop - start);
            

        } catch(Exception e) {
            System.setOut(originalStream);
            System.out.println(e.getMessage());
            solutionTraditional = solutionOurs = solutionAStar = solutionBidirectional = new Solution(new ArrayList<>(), new ArrayList<>());
        }
        System.setOut(originalStream);

        // Can it be compared more efficiently?
        boolean equalSolutionsDijk = solutionTraditional.getShortestPath().equals(solutionOurs.getShortestPath());
        boolean equalSolutionsBiDijk = solutionTraditional.getShortestPath().equals(solutionBidirectional.getShortestPath());
        boolean equalSolutionsAstar = solutionTraditional.getShortestPath().equals(solutionAStar.getShortestPath());

        // assert !equalSolutions;
        // TODO wrap so only on when assertions are on
        if (!equalSolutionsDijk || !equalSolutionsBiDijk || !equalSolutionsAstar) {
            System.out.println(" not equal!");

            GraphVisualiser vis1 = new GraphVisualiser(g, BoundingBox.AarhusSilkeborg);
            vis1.drawPath(solutionOurs.getShortestPath());
            vis1.visualize();

            if (!equalSolutionsDijk) {
                System.out.println("Difference in Traditional dijkstra and ours");
                GraphVisualiser vis2 = new GraphVisualiser(g, BoundingBox.AarhusSilkeborg);
                vis2.drawPath(solutionTraditional.getShortestPath());
                vis2.visualize();
            }

            if (!equalSolutionsBiDijk) {
                System.out.println("Difference in Traditional dijkstra and bidirectional");
                GraphVisualiser vis3 = new GraphVisualiser(g, BoundingBox.AarhusSilkeborg);
                vis3.drawPath(solutionBidirectional.getShortestPath());
                System.out.println(solutionBidirectional.getShortestPath());
                vis3.visualize();
            }            

            if (!equalSolutionsAstar) {
                System.out.println("Difference in Traditional dijkstra and Astar");
                GraphVisualiser vis4 = new GraphVisualiser(g, BoundingBox.AarhusSilkeborg);
                vis4.drawPath(solutionAStar.getShortestPath());
                vis4.visualize();
            }

            try{
            Thread.sleep(400000);
            } catch(InterruptedException e) {
                System.out.println(e);
            }
        }

        totalExpandedTraditional += solutionTraditional.getVisited().size();
        totalExpandedOurs += solutionOurs.getVisited().size();
        totalExpandedBidirectional+= solutionBidirectional.getVisited().size();
        totalExpandedAStar += solutionAStar.getVisited().size();

        System.out.println();   

    }


    public static void main(String[] args) {
        Graph g = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

        traditional = new DijkstraTraditional(g);
        ours = new Dijkstra(g);
        astar = new Astar(g);
        bidirectional = new BidirectionalDijkstra(g);

        int runs = 300;

        for (int i = 0; i < runs; i++) {
            System.out.print(" -> " + i);
            try {

                testAllShortestPath(g);

            } catch(Exception e) {
                System.out.println(" failed (exception)");
                e.printStackTrace();
            } catch(AssertionError e) {
                System.out.println(" failed (assert)");
            }
        }

        double sec = 10e9; // nanoseconds per second
        double ms = 10e6;  // nanoseconds per millisecond

        System.out.println("[*] Done!");
        System.out.println("     Total runs: " + runs);
        System.out.println();

        System.out.printf("     Total time taken for \"traditional\"       %8.3f seconds \n", (double) totalTimeTraditional / sec);
        System.out.printf("     Total time taken for ours                %8.3f seconds \n", (double) totalTimeOurs / sec);
        System.out.printf("     Total time taken for bidirectional       %8.3f seconds \n", (double) totalTimeBidirectional / sec);
        System.out.printf("     Total time taken for astar               %8.3f seconds \n", (double) totalTimeAStar / sec);
        System.out.println();

        System.out.printf("     Average time taken for \"traditional\"     %8.3f ms \n", (double) totalTimeTraditional/runs / ms);
        System.out.printf("     Average time taken for ours              %8.3f ms \n", (double) totalTimeOurs/runs / ms);
        System.out.printf("     Average time taken for bidirectional     %8.3f ms \n", (double) totalTimeBidirectional/runs / ms);
        System.out.printf("     Average time taken for astar             %8.3f ms \n", (double) totalTimeAStar/runs / ms);
        System.out.println();

        System.out.printf("     Average edges expanded for \"traditional\"     %8d edges \n", (long) totalExpandedTraditional/runs);
        System.out.printf("     Average edges expanded for ours              %8d edges \n", (long) totalExpandedOurs/runs);
        System.out.printf("     Average edges expanded for bidirectional     %8d edges \n", (long) totalExpandedBidirectional/runs);
        System.out.printf("     Average edges expanded for A*                %8d edges \n", (long) totalExpandedAStar/runs);

    }



}



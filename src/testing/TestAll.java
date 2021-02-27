package testing;

import graph.*;
import utility.*;
import pathfinding.framework.*;
import pathfinding.standard.*;

import java.io.*;
import java.util.*;

public class TestAll {

    static long totalTimeTraditional = 0;
    static long totalTimeOurs = 0;
    static long totalTimeBidirectional = 0;
    static long totalTimeAStar = 0;

    static long start = 0;
    static long stop = 0;

    static void testAllShortestPath(Graph g, Graph ginv){
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
        
        PathfindingAlgo traditional = new DijkstraTraditional();
        PathfindingAlgo ours = new Dijkstra();
        PathfindingAlgo astar = new Astar();
        BidirectionalDijkstra bidirictional = new BidirectionalDijkstra();

        System.setOut(originalStream);
        System.out.print("  " + a + "  ->  " + b);
        System.setOut(noopStream);


        Solution solutionTraditional, solutionOurs, solutionAStar, solutionBidirectional;
        try {
            start = System.nanoTime();
            solutionTraditional = traditional.shortestPath(g, a, b);
            stop = System.nanoTime();
            totalTimeTraditional += (stop - start);
            
            start = System.nanoTime();
            solutionOurs = ours.shortestPath(g, a, b);
            stop = System.nanoTime();
            totalTimeOurs += (stop - start);

            start = System.nanoTime();
            solutionAStar = astar.shortestPath(g, a, b);
            stop = System.nanoTime();
            totalTimeAStar += (stop - start);

            start = System.nanoTime();
            solutionBidirectional = bidirictional.shortestPath(g, ginv, a, b);
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
        System.out.println();   

    }


    public static void main(String[] args) {
        Graph g = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");
        Graph ginv = GraphUtils.invertGraph(g);

        int runs = 1000;

        for (int i = 0; i < runs; i++) {
            System.out.print(" -> " + i);
            try {

                testAllShortestPath(g, ginv);

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
        System.out.printf("     Total time taken for bidirectional       %8.3f seconds \n", (double) totalTimeBidirectional/1000000000);
        System.out.printf("     Total time taken for astar               %8.3f seconds \n", (double) totalTimeAStar/1000000000);

        System.out.printf("     Average time taken for \"traditional\"     %8.3f seconds \n", (double) totalTimeTraditional/runs/1000000000);
        System.out.printf("     Average time taken for ours              %8.3f seconds \n", (double) totalTimeOurs/runs/1000000000);
        System.out.printf("     Average time taken for bidirectional     %8.3f seconds \n", (double) totalTimeBidirectional/runs/1000000000);
        System.out.printf("     Average time taken for astar             %8.3f seconds \n", (double) totalTimeAStar/runs/1000000000);


    }



}



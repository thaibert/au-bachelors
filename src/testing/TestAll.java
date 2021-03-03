package testing;

import graph.*;
import utility.*;
import pathfinding.framework.*;
import pathfinding.standard.*;

import java.io.*;
import java.util.*;

public class TestAll {
    static final int DIJKSTRA_TRADITIONAL = 0;
    static final int DIJKSTRA_OURS = 1;
    static final int DIJKSTRA_BIDIRECTIONAL = 2;
    static final int ASTAR = 3;

    static String[] names = new String[]{"TradDijk   ", 
                                         "OurDijk    ", 
                                         "BidirecDijk", 
                                         "A*         "};
    static int numAlgos = names.length;

    static PathfindingAlgo[] algos = new PathfindingAlgo[numAlgos];
    static long[] totalTimes = new long[numAlgos];
    static long[] totalExpanded = new long[numAlgos];

    static long start;
    static long stop;


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

        if (a.equals(b)) {
            System.setOut(originalStream);
            System.out.print("\n");
            return;
        }

        System.setOut(originalStream);
        System.out.print("  " + a + "  ->  " + b);
        System.setOut(noopStream);


        Solution[] solutions = new Solution[numAlgos];
        try {

            for (int i = 0; i < numAlgos; i++) {
                start = System.nanoTime();
                solutions[i] = algos[i].shortestPath(a, b);
                stop = System.nanoTime();
                totalTimes[i] += (stop - start);
                totalExpanded[i] += solutions[i].getVisited().size();
            }

        } catch(Exception e) {
            System.setOut(originalStream);
            e.printStackTrace();
            Solution emptySolution = new Solution(new ArrayList<>(), new ArrayList<>());
            for (int i = 0; i < numAlgos; i++) {
                solutions[i] = emptySolution;
            }
        }
        System.setOut(originalStream);

        boolean[] solutionsEqual = new boolean[numAlgos];
        boolean hasDifference = false;
        for (int i = 0; i < numAlgos; i++) {
            boolean isEqual = solutions[0].getShortestPath().equals(solutions[i].getShortestPath());
            solutionsEqual[i] = isEqual;
            if (! isEqual) {
                hasDifference = true;
            }
        }

        // TODO wrap so only on when assertions are on
        if (hasDifference) {
            System.out.println(" not equal!");

            List<Vertex> path = solutions[0].getShortestPath();
            System.out.printf("%s has %5d nodes and a distance of %8.2f meters\n", 
                names[0], 
                path.size(), 
            GraphUtils.pathDistance(path));

            // Draw traditional dijkstra
            GraphVisualiser vis1 = new GraphVisualiser(g, BoundingBox.Christiansbjerg);
            vis1.drawPath(path);
            vis1.visualize(names[0]);
            

            // Draw the rest
            for (int i = 0; i < numAlgos; i++) {
                if (! solutionsEqual[i]) {
                    path = solutions[i].getShortestPath();
                    System.out.printf("%s has %5d nodes and a distance of %8.2f meters\n", 
                        names[i], 
                        path.size(), 
                        GraphUtils.pathDistance(path));
                    GraphVisualiser vis2 = new GraphVisualiser(g, BoundingBox.Christiansbjerg);
                    vis2.drawPath(solutions[i].getShortestPath());
                    vis2.visualize(names[i]);
                }
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
        Graph g = GraphPopulator.populateGraph("aarhus-intersections.csv");

        algos[DIJKSTRA_TRADITIONAL] = new DijkstraTraditional(g);
        algos[DIJKSTRA_OURS] = new Dijkstra(g);
        algos[ASTAR] = new Astar(g);
        algos[DIJKSTRA_BIDIRECTIONAL] = new BidirectionalDijkstra(g);

        int runs = (int) 1e3;

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

        double sec = 1e9; // nanoseconds per second
        double ms = 1e6;  // nanoseconds per millisecond

        System.out.println("[*] Done!");
        System.out.println("     Total runs: " + runs + "\n");

        for (int i = 0; i < numAlgos; i++) {
            System.out.printf("     Total time taken for %s:      %8.3f seconds \n", names[i], (double) totalTimes[i] / sec);
        }
        System.out.println();

        for (int i = 0; i < numAlgos; i++) {
            System.out.printf("     Average time taken for %s     %8.3f ms \n", names[i], (double) totalTimes[i]/runs / ms);
        }
        System.out.println();

        for (int i = 0; i < numAlgos; i++) {
            System.out.printf("     Average edges expanded for %s     %8d edges \n", names[i], (long) totalExpanded[i]/runs);
        }

    }


}



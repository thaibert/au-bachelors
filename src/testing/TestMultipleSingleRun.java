package testing;

import graph.*;
import utility.*;
import pathfinding.framework.*;
import pathfinding.standard.*;

import java.io.*;
import java.util.*;

public class TestMultipleSingleRun {

    static final int DIJKSTRA_TRADITIONAL = 0;
    static final int DIJKSTRA_OURS = 1;
    static final int DIJKSTRA_BIDIRECTIONAL = 2;
    static final int ASTAR = 3;
    static final int ALT = 4;
    static final int ASTAR_BIDIRECTIONAL = 5;
    static final int ALT_BIDIRECTIONAL = 6;

    static String[] names = new String[]{"TradDijk   ", 
                                         "OurDijk    ", 
                                         "BidirecDijk", 
                                         "A*         ",
                                         "ALT        ",
                                         "BidrecAstar",
                                         "BidrecALT  "};
    static int numAlgos = names.length;

    static PathfindingAlgo[] algos = new PathfindingAlgo[numAlgos];
    static long[] totalTimes = new long[numAlgos];
    static long[] totalExpanded = new long[numAlgos];

    static long start;
    static long stop;

    static void testMultipleSingleRun(Graph g, Vertex source, Vertex target) {

        PrintStream originalStream = System.out;

        PrintStream noopStream = new PrintStream(new OutputStream(){
            public void write(int b) {
                // NO-OP
            }
        });
        System.setOut(noopStream);


        System.setOut(originalStream);
        System.out.print("  " + source + "  ->  " + target);
        System.setOut(noopStream);

        Solution[] solutions = new Solution[numAlgos];

        try {

            for (int i = 0; i < numAlgos; i++) {
                start = System.nanoTime();
                solutions[i] = algos[i].shortestPath(source, target);
                stop = System.nanoTime();
                totalTimes[i] += (stop - start);
                totalExpanded[i] += solutions[i].getVisited().size();
            }

        } catch(Exception e) {
            System.setOut(originalStream);
            e.printStackTrace();
            Solution emptySolution = new Solution(new ArrayList<>(), new ArrayList<>(), null);
            for (int i = 0; i < numAlgos; i++) {
                solutions[i] = emptySolution;
            }
        }

        System.setOut(originalStream);

        boolean[] solutionsEqual = new boolean[numAlgos];
        for (int i = 0; i < numAlgos; i++) {
            boolean isEqual = solutions[DIJKSTRA_TRADITIONAL].getShortestPath().equals(solutions[i].getShortestPath());
            solutionsEqual[i] = isEqual;
        }

        // TODO wrap so only on when assertions are on
        List<Vertex> path = solutions[DIJKSTRA_TRADITIONAL].getShortestPath();
        System.out.printf("%s has %5d nodes and a distance of %8.2f meters\n", 
            names[DIJKSTRA_TRADITIONAL], 
            path.size(), 
        GraphUtils.pathDistance(path));


        // Draw them and print if something is different
        for (int i = 0; i < numAlgos; i++) {
            if (! solutionsEqual[i]) {
                path = solutions[i].getShortestPath();
                System.out.printf("%s has %5d nodes and a distance of %8.2f meters\n", 
                    names[i], 
                    path.size(), 
                    GraphUtils.pathDistance(path));
            }
            GraphVisualiser vis2 = new GraphVisualiser(g, BoundingBox.AarhusSilkeborg);
            vis2.drawVisited(solutions[i].getVisited());
            vis2.drawPath(solutions[i].getShortestPath());
            vis2.drawMeetingNode(solutions[i].getMeetingNode());
            vis2.visualize(names[i]);
        }

        System.out.println();


    }


    public static void main(String[] args){
        Graph g = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");
        //Graph gpruned = GraphUtils.pruneGraphOfChains(g);

        algos[DIJKSTRA_TRADITIONAL] = new DijkstraTraditional(g);
        algos[DIJKSTRA_OURS] = new Dijkstra(g);
        algos[ASTAR] = new Astar(g);
        algos[DIJKSTRA_BIDIRECTIONAL] = new BidirectionalDijkstra(g);
        algos[ALT] = new ALT(g, 1, 5); // TODO how many landmarks
        algos[ASTAR_BIDIRECTIONAL] = new NBA(g);
        algos[ALT_BIDIRECTIONAL] = new BidirectionalALT(g, 1, 5);  //TODO how many landmarks


        Vertex a = Location.Silkeborg;
        Vertex b = Location.Viborgvej;

        testMultipleSingleRun(g, a, b);

        // Print info about the runs
        double sec = 1e9; // nanoseconds per second

        System.out.println("[*] Done!");
        for (int i = 0; i < numAlgos; i++) {
            System.out.printf("     Total time taken for %s:      %8.3f seconds \n", names[i], (double) totalTimes[i] / sec);
        }
        System.out.println();
        for (int i = 0; i < numAlgos; i++) {
            System.out.printf("     Edges expanded for %s     %8d edges \n", names[i], (long) totalExpanded[i]);
        }

    }

    
}

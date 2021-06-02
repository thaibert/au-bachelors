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
    static final int ALT = 4;
    static final int ASTAR_BIDIRECTIONAL = 5;
    static final int ALT_BIDIRECTIONAL = 6;
    static final int REACH_DIJKSTRA = 7;
    static final int REACH_ALT = 8;

    static String[] names = new String[]{"TradDijk   ", 
                                         "OurDijk    ", 
                                         "BidirecDijk", 
                                         "A*         ",
                                         "ALT        ",
                                         "BidrecAstar",
                                         "BidrecALT  ",
                                         "ReachDijk  ",
                                         "ReachALT   "};
    static int numAlgos = names.length;

    static PathfindingAlgo[] algos = new PathfindingAlgo[numAlgos];
    static long[] totalTimes = new long[numAlgos];
    static long[] totalExpanded = new long[numAlgos];
    static long[] nodeScanned = new long[numAlgos];
    static long nodesInAllPaths = 0;

    static long start;
    static long stop;

    static File csv;
    static PrintWriter pw;


    static void testAllShortestPath(Graph g, Random rnd) throws FileNotFoundException {
        // TODO in "actual" runs, we should comment in out in files, as it still takes time?
        // Disable printing while running 
        PrintStream originalStream = System.out;

        PrintStream noopStream = new PrintStream(new OutputStream(){
            public void write(int b) {
                // NO-OP
            }
        });
        System.setOut(noopStream);

        Vertex a = GraphUtils.pickRandomVertexWithSeed(g, rnd);
        Vertex b = GraphUtils.pickRandomVertexWithSeed(g, rnd);

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
                if(i == 0){
                    nodesInAllPaths += solutions[i].getShortestPath().size();
                }
                totalTimes[i] += (stop - start);
                totalExpanded[i] += solutions[i].getVisited().size();
                nodeScanned[i] += solutions[i].getAmountOfScannedVertices();

                // WRITE TO CSV
                //  algo, time(ns), edges expanded, #nodes, driven_len
                pw.write(names[i] + "," 
                    + (stop-start) + "," 
                    + solutions[i].getVisited().size() + ","
                    + solutions[0].getShortestPath().size() + ","
                    + GraphUtils.realLength(g, solutions[0].getShortestPath()) + ","+ 
                    + solutions[i].getAmountOfScannedVertices() + "\n");
            }

        } catch(Exception e) {
            System.setOut(originalStream);
            e.printStackTrace();
            Solution emptySolution = new Solution(new ArrayList<>(), new ArrayList<>(), null, 0);
            for (int i = 0; i < numAlgos; i++) {
                solutions[i] = emptySolution;
            }
        }
        System.setOut(originalStream);

        boolean[] solutionsEqual = new boolean[numAlgos];
        boolean hasDifference = false;
        for (int i = 0; i < numAlgos; i++) {

            boolean isEqual = solutions[DIJKSTRA_TRADITIONAL].getShortestPath().containsAll(solutions[i].getShortestPath());
            solutionsEqual[i] = isEqual;
            if (! isEqual) {
                hasDifference = true;
            }
        }


        // TODO wrap so only on when assertions are on
        if (hasDifference) {
            System.out.println(" not equal!");

            List<Vertex> path = solutions[DIJKSTRA_TRADITIONAL].getShortestPath();
            System.out.printf("%s has %5d nodes and a distance of %8.2f meters\n", 
                names[DIJKSTRA_TRADITIONAL], 
                path.size(), 
            GraphUtils.pathDistance(path));

            // Draw traditional dijkstra
            GraphVisualiser vis1 = new GraphVisualiser(g, BoundingBox.Iceland);
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
                    GraphVisualiser vis2 = new GraphVisualiser(g, BoundingBox.Iceland);
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

        System.out.print("  [" + solutions[DIJKSTRA_TRADITIONAL].getShortestPath().size() + " nodes]");
        System.out.println();   
    }

   


    public static void main(String[] args) throws FileNotFoundException {
        String fileIn = "iceland-latest-roads.csv";
        int runs = (int) 1000;


        Graph g = GraphPopulator.populateGraph(fileIn);
        g = GraphUtils.pruneChains(g);
        Graph gReach = readShortcutGraph("iceland-shortcutV2");
        Map<Vertex, Double> r = readReaches("iceland-reachV2");


        //Graph gpruned = GraphUtils.pruneGraphOfChains(g);

        LandmarkSelector ls = new LandmarkSelector(g, 16, 1); // TODO how many landmarks
        LandmarkSelector ls2 = new LandmarkSelector(gReach, 16, 1);

        algos[DIJKSTRA_TRADITIONAL] = new DijkstraTraditional(g); //TODO change to DijkstraTraditional, its just slow to run
        algos[DIJKSTRA_OURS] = new Dijkstra(g);
        algos[ASTAR] = new Astar(g);
        algos[DIJKSTRA_BIDIRECTIONAL] = new BidirectionalDijkstra(g);
        algos[ALT] = new ALT(g, ls); 
        algos[ASTAR_BIDIRECTIONAL] = new NBA(g);
        algos[ALT_BIDIRECTIONAL] = new BidirectionalALT(g, ls);
        algos[REACH_DIJKSTRA] = new DijkstraReach(gReach, r);
        algos[REACH_ALT] = new ALTReach(gReach, ls2, r);
        
        // Prepare data logging file
        csv = new File("log-"+ runs + "-" +fileIn+"-pruned");
        pw = new PrintWriter(csv);
        pw.write("algo,time,edges_expanded,no_nodes,driven_len,verticesScanned\n");
        
        Random rnd = new Random(1);

        System.out.println();
        for (int i = 0; i < runs; i++) {
            System.out.print(" -> " + i);
            try {
                testAllShortestPath(g, rnd);

            } catch(Exception e) {
                System.out.println(" failed (exception)");
                e.printStackTrace();
            } catch(AssertionError e) {
                System.out.println(" failed (assert)");
            }
        }

        double sec = 1e9; // nanoseconds per second
        double ms = 1e6;  // nanoseconds per millisecond

        System.out.println("[*]  Done!");
        System.out.println("     Total runs: " + runs + "\n");

        pw.flush();
        pw.close();

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

        System.out.println();
        for (int i = 0; i < numAlgos; i++) {
            System.out.printf("     Average Nodes scanned for %s     %8d nodes \n", names[i], (long) nodeScanned[i]/runs);
        }

        System.out.println();
        for (int i = 0; i < numAlgos; i++) {
            System.out.printf("     Efficiency for %s     %8.6f  \n", names[i], (double) (nodesInAllPaths*1.0/nodeScanned[i]*1.0));
        }



    }

    public static Map<Vertex, Double> readReaches(String filename) {
        Map<Vertex, Double> r = null;
        try {
            File toRead = new File(filename);
            FileInputStream fis = new FileInputStream(toRead);
            ObjectInputStream ois = new ObjectInputStream(fis);
    
            r = (HashMap<Vertex,Double>) ois.readObject();
    
            ois.close();
            fis.close();
            //print All data in MAP
        } catch(Exception e) {
            e.printStackTrace();
        }

        return r;
    }

    private static Graph readShortcutGraph(String filename) {
        Graph g = null;
        try {
            File toRead = new File(filename);
            FileInputStream fis = new FileInputStream(toRead);
            ObjectInputStream ois = new ObjectInputStream(fis);
    
            g = (Graph) ois.readObject();
    
            ois.close();
            fis.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

        return g;
    }


}



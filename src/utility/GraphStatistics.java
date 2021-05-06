package utility;

import java.io.*;
import java.util.*;


import graph.*;


public class GraphStatistics {

    private static void doStatistics(String filename) {
        int twowayEdges = 0;
        int onewayEdges = 0;
        Map<Vertex, IntTuple> nodeDegrees = new HashMap<>(); // in-degree, out-degree

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(
                    new File(filename))))) {
            System.out.println("Performing statistics on " + filename);
            String currentLine = reader.readLine(); // Read first line to skip CSV header line

            Vertex prevVertex = null;
            Vertex currVertex = null;
            String prevWayID = "";
            String currWayID = "";

            while (null != (currentLine = reader.readLine())) {
                // Setup "prev" values at the start rather than the end. Then we don't forget :)
                prevWayID = currWayID;
                prevVertex = currVertex;

                String[] args = currentLine.split(",");
                double lat = Double.valueOf(args[0]);
                double lon = Double.valueOf(args[1]);
                currWayID = args[2];
                boolean oneway = "1".equals(args[3]); // args[3] is the "oneway" column. Either 1 or 0.

                currVertex = new Vertex(lat, lon);

                if (! prevWayID.equals(currWayID)) {
                    // Hit a new ID; skip to next node in same way so we can construct an edge
                    continue;
                }
                
                // ========= the meat ===========
                // If we reach here, we know for sure there's an edge from prev->curr. Maybe also the other way.

                if (oneway) {
                    onewayEdges++;
                } else {
                    twowayEdges++;
                }

                

                // Increase prev's out-degree
                IntTuple oldPrev = nodeDegrees.getOrDefault(prevVertex, new IntTuple(0, 0));
                IntTuple newPrev = new IntTuple(oldPrev.in, oldPrev.out + 1);
                nodeDegrees.put(prevVertex, newPrev);

                // Increase curr's in-degree
                IntTuple oldCurr = nodeDegrees.getOrDefault(currVertex, new IntTuple(0, 0));
                IntTuple newCurr = new IntTuple(oldCurr.in + 1, oldCurr.out);
                nodeDegrees.put(currVertex, newCurr);


                if (! oneway) {
                    // Increase prev's in-degree
                    oldPrev = nodeDegrees.getOrDefault(prevVertex, new IntTuple(0, 0));
                    newPrev = new IntTuple(oldPrev.in + 1, oldPrev.out);
                    nodeDegrees.put(prevVertex, newPrev);

                    // Increase curr's out-degree
                    oldCurr = nodeDegrees.getOrDefault(currVertex, new IntTuple(0, 0));
                    newCurr = new IntTuple(oldCurr.in, oldCurr.out + 1);
                    nodeDegrees.put(currVertex, newCurr);
                }

                


            }
        } catch(Exception e) {
            e.printStackTrace();
        } 

        // Simple info
        System.out.printf("Total nodes:  %10d\n", nodeDegrees.keySet().size() );
        System.out.printf("Total edges:  %10d\n", twowayEdges);
        System.out.printf("Oneway edges: %10d\n", onewayEdges);



        // In / out degree
        int[][] inOut = new int[20][20]; // no node should have more than 20? hahah

        for (Vertex key : nodeDegrees.keySet()) {
            IntTuple value = nodeDegrees.get(key);
            inOut[value.in][value.out]++;
        }

        System.out.printf("in \\ out: %8d, %8d, %8d, %8d, %8d, %8d, %8d \n",
            1,2,3,4,5,6,7);
        for (int i = 1; i <= 7; i++) {
            System.out.printf("=== %d ==: %8d  %8d  %8d  %8d  %8d  %8d  %8d \n",
                i, inOut[i][1],  inOut[i][2],  inOut[i][3],  inOut[i][4],  inOut[i][5],  inOut[i][6],  inOut[i][7]  );
        }

    }



    public static void main(String[] args) {
        String filename = "denmark-latest-roads.csv";

        doStatistics(filename);


        Graph g = GraphPopulator.populateGraph(filename);

        int edges = 0;
        for (Vertex v : g.getAllVertices()) {
            edges += g.getNeighboursOf(v).size();
        }

        System.out.println("Actual edges in graph: " + edges);
        
    }

    
    
}

class IntTuple {
    public final int in;
    public final int out;

    public IntTuple(int in, int out) {
        this.in = in;
        this.out = out;
    }
}

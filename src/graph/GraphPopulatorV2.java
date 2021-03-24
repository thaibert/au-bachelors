package graph;

import java.io.*;
import java.util.*;

import utility.GraphUtils;

public class GraphPopulatorV2 {
    
    final static double radius = 6371000;

    public static Graph populateGraph(String filename) {
        System.out.println("--> Populating graph V2 DOES NOT WORK CURRENTLY");

        Graph graph = new SimpleGraph();

        addAll(filename, graph);

        return graph;
    }


    private static void addAll(String filename, Graph graph) {
        //TODO don't add nodes we don't need

        Map<String, Integer> count = new HashMap<>();

        // How many times is a node refereced?
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(
                    new File(filename))))){
                        String currentLine = reader.readLine(); // Read first line to skip CSV header line
                        
                        while (null != (currentLine = reader.readLine())) {
                            // Setup "prev" values at the start rather than the end. Then we don't forget :)
            
                            String[] args = currentLine.split(",");
                            double lat = Double.valueOf(args[0]);
                            double lon = Double.valueOf(args[1]);
            
                            count.put(Double.toString(lat)+Double.toString(lon), count.getOrDefault(Double.toString(lat)+Double.toString(lon), 0)+1);
            
                        }
            

        } catch(Exception e) {
            System.out.println("--> " + e);
        } 


        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(
                    new File(filename))))) {

            System.out.println("  --> adding nodes and ways");
            String currentLine = reader.readLine(); // Read first line to skip CSV header line
            
            Vertex prevVertex = null;
            Vertex currVertex = null;
            String prevWayID = "";
            String currWayID = "";
            double distToAdd = 0.0;


            while (null != (currentLine = reader.readLine())) {
                // Setup "prev" values at the start rather than the end. Then we don't forget :)
                prevVertex = currVertex;
                prevWayID = currWayID;

                String[] args = currentLine.split(",");
                double lat = Double.valueOf(args[0]);
                double lon = Double.valueOf(args[1]);
                currWayID = args[2];
                boolean oneway = "1".equals(args[3]); // args[3] is the "oneway" column. Either 1 or 0.

                currVertex = new Vertex(lat, lon);
                graph.addVertex(currVertex);

                if (! prevWayID.equals(currWayID)) {
                    // Hit a new ID; skip to next node in same way so we can construct an edge
                    continue;
                }

                //double dist_lat = Math.pow(currVertex.getLatitude() - prevVertex.getLatitude(), 2);
                //double dist_lon = Math.pow(currVertex.getLongitude()- prevVertex.getLongitude(),2);
                //double dist = Math.sqrt(dist_lat + dist_lon); // TODO may be slow?

                double dist = GraphUtils.haversineDist(prevVertex, currVertex);
                
                distToAdd += dist;

                if (count.get(Double.toString(lat)+Double.toString(lon)) < 2 ){
                    continue;
                }
                graph.addEdge(prevVertex, currVertex, distToAdd);

                if (! oneway) {
                    // Only add the edge going back if it makes sense!
                    graph.addEdge(currVertex, prevVertex, distToAdd);
                }

            }

        } catch(Exception e) {
            System.out.println("--> " + e);
        } 




    }
    
}

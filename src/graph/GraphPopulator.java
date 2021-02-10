package graph;

import java.io.*;
import java.util.*;

public class GraphPopulator {

    public static Graph populateGraph(String filename) {
        Graph graph = new SimpleGraph();

        addNodes(filename, graph);
        addEdges(filename, graph);

        return graph;
    }

    private static void addNodes(String filename, Graph graph) {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(
                    new File(filename))))) {
            System.out.println("-------------------- adding nodes");
            String currentLine = reader.readLine(); // Read first line to skip CSV header line

            while (null != (currentLine = reader.readLine())) {
                String[] args = currentLine.split(",");
                double lat = Double.valueOf(args[0]);
                double lon = Double.valueOf(args[1]);

                graph.addVertex(new Vertex(lat, lon));
            }
        } catch(Exception e) {
            System.out.println("--> " + e);
        } 
    }

    private static void addEdges(String filename, Graph graph) {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(
                    new File(filename))))) {
            System.out.println("-------------------- adding edges");
            String currentLine = reader.readLine(); // Read first line to skip CSV header line

            Vertex prevVertex = null;
            Vertex currVertex = null;
            String prevWayID = "";
            String currWayID = "";

            while (null != (currentLine = reader.readLine())) {
                // Setup "prev" values at the start rather than the end. Then we don't forget :)
                prevVertex = currVertex;
                prevWayID = currWayID;

                String[] args = currentLine.split(",");
                double lat = Double.valueOf(args[0]);
                double lon = Double.valueOf(args[1]);
                currWayID = args[2];

                currVertex = new Vertex(lat, lon);

                if (! prevWayID.equals(currWayID)) {
                    // Hit a new ID; skip to next node in same way so we can construct an edge
                    continue;
                }

                double dist_lat = Math.pow(currVertex.getLatitude() - prevVertex.getLatitude(), 2);
                double dist_lon = Math.pow(currVertex.getLongitude()- prevVertex.getLongitude(),2);
                double dist = Math.sqrt(dist_lat + dist_lon); // TODO may be slow?

                graph.addEdge(prevVertex, currVertex, dist);
                graph.addEdge(currVertex, prevVertex, dist);
            }
        } catch(Exception e) {
            System.out.println("--> " + e);
        } 
    }

}

package graph;

import java.io.*;
import java.util.*;

public class GraphPopulator {
    public static void main(String[] args) {
        GraphPopulator gp = new GraphPopulator();
        Graph graph = gp.populateGraph("intersections.csv");
    }

    public Graph populateGraph(String filename) {
        Graph graph = new SimpleGraph();

        addNodes(filename, graph);
        addEdges(filename, graph);

        return graph;
    }

    private void addNodes(String filename, Graph graph) {
        try {
            File file = new File(filename);
            InputStream temp = new FileInputStream(file);
            InputStreamReader input = new InputStreamReader(temp);
            System.out.println("--------------------");

            BufferedReader reader = new BufferedReader(input);
            String currentLine = reader.readLine(); // Read first line to skip CSV header line

            while (null != (currentLine = reader.readLine())) {
                String[] args = currentLine.split(",");
                double lat = Double.valueOf(args[0]);
                double lon = Double.valueOf(args[1]);

                graph.addVertex(new Vertex(lat, lon));
            }
            reader.close(); // TODO should probably be in a final block
        } catch(Exception e) {
            System.out.println("--> " + e);
        } 
    }

    private void addEdges(String filename, Graph graph) {
        try {
            File file = new File(filename);
            InputStream temp = new FileInputStream(file);
            InputStreamReader input = new InputStreamReader(temp);
            System.out.println("--------------------");

            BufferedReader reader = new BufferedReader(input);
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
                double dist = dist_lat + dist_lon; // should really be sqrt(d_lat+d_lon), but may be slow? todo

                graph.addEdge(prevVertex, currVertex, dist);
                graph.addEdge(currVertex, prevVertex, dist);
            }
            reader.close(); // TODO should probably be in a final block
        } catch(Exception e) {
            System.out.println("--> " + e);
        } 
    }

}

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

        try {
            File file = new File(filename);
            InputStream temp = new FileInputStream(file);
            InputStreamReader input = new InputStreamReader(temp);
            System.out.println("--------------------");

            BufferedReader reader = new BufferedReader(input);
            reader.readLine();
            String currentLine = reader.readLine();
            while (currentLine != null) { //TODO when should this stop, also remember to close reader
                String[] args = currentLine.split(",");
                double lat = Double.valueOf(args[0]);
                double lon = Double.valueOf(args[1]);

                graph.addVertex(new Vertex(lat, lon));

                currentLine = reader.readLine();
            }
            reader.close(); // TODO should probably be in a final block
        } catch(Exception e) {
            System.out.println("--> " + e);
            return null;
        } 

        addEdges(graph);

        return graph;
    }

    private void addEdges(Graph graph) {
        addWays(graph);
        addIntersections(graph);
    }


    private void addWays(Graph graph) {
        try {
            File file = new File("intersections.csv");
            InputStream temp = new FileInputStream(file);
            InputStreamReader input = new InputStreamReader(temp);
            System.out.println("--------------------");

            Vertex prevVertex = null;
            String prevWayID = "";

            BufferedReader reader = new BufferedReader(input);
            reader.readLine();
            String currentLine = reader.readLine();
            while (currentLine != null) { //TODO when should this stop, also remember to close reader
                String[] args = currentLine.split(",");
                double lat = Double.valueOf(args[0]);
                double lon = Double.valueOf(args[1]);
                String wayID = args[2];

                Vertex currVertex = new Vertex(lat, lon);

                if (! prevWayID.equals(wayID)) {
                    currentLine = reader.readLine();
                    prevWayID = wayID;
                    prevVertex = currVertex;
                    continue;
                }

                if (! prevWayID.equals(wayID)) {
                    // Hit a new ID, skip
                    currentLine = reader.readLine();
                    prevWayID = wayID;
                    prevVertex = currVertex;
                    continue;
                }

                graph.addEdge(prevVertex, currVertex);
                graph.addEdge(currVertex, prevVertex);

                prevVertex = currVertex;
                prevWayID = wayID;
                currentLine = reader.readLine();
            }
            reader.close(); // TODO should probably be in a final block
        } catch(Exception e) {
            System.out.println("--> " + e);
        } 
    }

    private void addIntersections(Graph graph) {
        // TODO
    }

}

package utility;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import graph.*;

public class GraphIO {

    public static void saveGraphToCSV(Graph graph, String filename) {
        System.out.println("Encoding as csv (" + filename + ")");
        File csv = new File(filename);

        try (PrintWriter pw = new PrintWriter(csv)) {
            pw.write("v_lat, v_lon,   dist, n_lat, n_lon\n");
            for (Vertex v : graph.getAllVertices()) {
                if (graph.getNeighboursOf(v).size() == 0) continue;

                for (Neighbor n : graph.getNeighboursOf(v)) {
                    pw.write(v.getLatitude() + ","
                           + v.getLongitude() + ","
                           + n.distance + ","
                           + n.v.getLatitude() + ","
                           + n.v.getLongitude() + "\n");
                }
            }
            pw.flush();
            pw.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static Graph loadGraphFromCSV(String filename) {
        System.out.println("Loading csv-encoded graph (" + filename + ")");
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            Graph graph = new SimpleGraph();

            br.lines()
                .skip(1) // Skip the header
                .forEach(line -> {
                    // Header =  a_lat, a_lon,  dist,  b_lat, b_lon
                    String[] partsString = line.split(",");

                    Double[] parts = Arrays.stream(partsString)
                        .map(s -> Double.parseDouble(s))
                        .collect(Collectors.toList())
                        .toArray(new Double[]{});

                    Vertex a = new Vertex(parts[0], parts[1]);
                    Vertex b = new Vertex(parts[3], parts[4]);
                    double dist = parts[2];

                    graph.addVertex(a);
                    graph.addVertex(b);
                    graph.addEdge(a, b, dist);
                });
                return graph;

        } catch(IOException e) {
            e.printStackTrace();
            return new SimpleGraph();
        }
    }




    @Deprecated
    public static void saveGraphToFile(Graph graph, String filename) {
        System.out.println("Saving graph " + filename);
        try {
            File file = new File(filename);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
    
            oos.writeObject(graph);
            oos.flush();
            oos.close();
            fos.close();
            System.out.println("--> Graph saved!");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    public static Graph loadGraphFromFile(String filename) { // TODO should throw exception instead?
        Graph g = null;
        System.out.println("Loading graph " + filename);
        try {
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
    
            g = (Graph) ois.readObject();
    
            ois.close();
            fis.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.out.println("--> Graph loaded!");
        return g;
    }
    
}

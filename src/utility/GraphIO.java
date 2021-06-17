package utility;

import java.io.*;

import graph.*;

public class GraphIO {

    public static void saveGraphToFile(Graph graph, String filename) {
        try {
            File file = new File(filename);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
    
            oos.writeObject(graph);
            oos.flush();
            oos.close();
            fos.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static Graph loadGraphFromFile(String filename) { // TODO should throw exception instead?
        Graph g = null;
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

        return g;
    }
    
}

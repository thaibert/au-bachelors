package graph;

import java.util.*;
import java.util.List;
import java.awt.*;
import javax.swing.*;

import java.io.*;

public class GraphVisualiser extends Canvas {

    // It's really small on my screen when it's only 700x900
    final static int multiplier = 2;

    // NE = (10.2050, 56.1850)
    // SE = (10.2050, 56.1600)
    // SW = (10.1700, 56.1600)
    // NW = (10.1700, 56.1850)
    final static double MIN_LONG = 9.4807; // 10.1700;
    final static double MAX_LONG = 10.259; // 10.2050;
    final static double MIN_LAT = 56.0337; // 56.1600;
    final static double MAX_LAT = 56.2794; // 56.1850;
    final static int window_x = 1600*multiplier;
    final static int window_y = 900*multiplier;
    final static int radius = 6*multiplier;

    private Graph graph;
    private List<Vertex> shortestPath;
    private Map<Vertex,Vertex> visited;

    public GraphVisualiser(Graph graph) {
        this.graph = graph;
        setBackground(Color.WHITE);
    }

    public void visualize() {
        JFrame f = new JFrame();
        f.add(this);
        
        f.setSize(window_x,window_y);
        f.setResizable(false);
        f.setVisible(true);
    }

    // Public method that will eventually call the internal method that actually draws a path
    public void drawPath(List<Vertex> path) {
        this.shortestPath = path;
    }

    public void drawVisited(Map<Vertex,Vertex> visited){
        this.visited = visited;
    }

    // ================== public use methods end here ==================

    public void paint(Graphics g) {
        g.setColor(new Color(0,0,0, 20)); // 20 alpha out of 255
        drawGraph(g);
        if (this.visited != null) {
            drawVisited(g);
        }
        if (this.shortestPath != null) {
            drawPath(g);
        }
    }

    private void drawGraph(Graphics g) {
        graph.getAllVertices().forEach(v -> {
            // Draw nodes
            String lat = Double.toString(v.getLatitude());
            String lon = Double.toString(v.getLongitude());
            int[] v_coords = convertToXAndY(new String[] { lat, lon });
            //g.drawOval(v_coords[0] - radius / 2, v_coords[1] - radius / 2, radius, radius);

            // Draw edges
            graph.getNeighboursOf(v).forEach(n -> {
                String n_lat = Double.toString(n.v.getLatitude());
                String n_lon = Double.toString(n.v.getLongitude());
                int[] n_coords = convertToXAndY(new String[] { n_lat, n_lon });
                g.drawLine(v_coords[0], v_coords[1], n_coords[0], n_coords[1]);
            });

            // Testing to see whether way intersection is handled; or if we need to detect where ways intersect
            if (graph.getNeighboursOf(v).size() > 2) {
                // A node in a way can have at most 2 neighbors. If it's bigger, it IS handled :D
                /*
                Color oldColor = g.getColor();
                g.setColor(Color.MAGENTA);

                int temp_radius = radius/2 * 3 * graph.getNeighboursOf(v).size() / 2;

                g.drawOval(v_coords[0] - temp_radius / 2, v_coords[1] - temp_radius / 2, temp_radius, temp_radius);

                g.setColor(oldColor);
                */
            }
        });

    }


    private void drawPath(Graphics g) {
        Color oldColor = g.getColor();
        g.setColor(Color.RED);

        Vertex prev = this.shortestPath.get(0);
        for (int i = 1; i < this.shortestPath.size(); i++) {
            Vertex v = this.shortestPath.get(i);
            String v_lat = Double.toString(v.getLatitude());
            String v_lon = Double.toString(v.getLongitude());
            int[] v_coords = convertToXAndY(new String[] { v_lat, v_lon });
            String prev_lat = Double.toString(prev.getLatitude());
            String prev_lon = Double.toString(prev.getLongitude());
            int[] prev_coords = convertToXAndY(new String[] { prev_lat, prev_lon });

            g.drawLine(v_coords[0], v_coords[1], prev_coords[0], prev_coords[1]);
            prev = v;
        }
        g.setColor(oldColor);

    }

    private void drawVisited(Graphics g){
        Color oldColor = g.getColor();
        g.setColor(new Color(0,0,153));

        this.visited.forEach((key, value) ->{
            String key_lat = Double.toString(key.getLatitude());
            String key_lon = Double.toString(key.getLongitude());
            int[] key_coords = convertToXAndY(new String[] { key_lat, key_lon });
            String value_lat = Double.toString(value.getLatitude());
            String value_lon = Double.toString(value.getLongitude());
            int[] value_coords = convertToXAndY(new String[] { value_lat, value_lon });

            g.drawLine(key_coords[0], key_coords[1], value_coords[0], value_coords[1]);
        });

        g.setColor(oldColor);

    }

    public static int[] convertToXAndY(String[] arg){
        
        double lon = Double.parseDouble(arg[1]);
        double lat = Double.parseDouble(arg[0]);

        //System.out.printf("Input lon: %f, lat: %f\n", lon, lat);

        double d_long = MAX_LONG-MIN_LONG;
        double d_lat=  MAX_LAT - MIN_LAT;

        //System.out.printf("temp dlon: %f, dlat: %f\n", d_long, d_lat);

        //double tempx = (lon-MIN_LONG)/d_long * window_x;
        //double tempy = (lat-MIN_LAT)/d_lat * window_y;

        //System.out.printf("lon-min_long: %f, lat-min_lat: %f \n", lon-MIN_LONG, lat-MIN_LAT);

        //System.out.printf("temp dx: %f, dy: %f\n", tempx, tempy);

        int x = (int) Math.round((lon-MIN_LONG)/d_long * window_x);
        int y = window_y - (int) Math.round((lat-MIN_LAT)/d_lat * window_y);

        //System.out.printf("output x: %d, y: %d\n", x, y);


        int[] result = {x,y};
        return result;
    }


    public static void main(String[] args) {
        Graph graph = GraphPopulator.populateGraph("intersections.csv");
        GraphVisualiser vis = new GraphVisualiser(graph);
        vis.visualize();
    }
    
}

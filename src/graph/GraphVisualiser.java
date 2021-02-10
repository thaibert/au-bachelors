package graph;

import java.util.*;
import java.util.List;
import java.awt.*;
import javax.swing.*;

import java.io.*;

public class GraphVisualiser extends Canvas {

    // It's really small on my screen when it's only 700x900
    final static int multiplier = 3;

    // NE = (10.2050, 56.1850)
    // SE = (10.2050, 56.1600)
    // SW = (10.1700, 56.1600)
    // NW = (10.1700, 56.1850)
    final static double MIN_LONG = 10.1700;
    final static double MAX_LONG = 10.2050;
    final static double MIN_LAT = 56.1600;
    final static double MAX_LAT = 56.1850;
    final static int window_x = 700*multiplier;
    final static int window_y = 900*multiplier;
    final static int radius = 6*multiplier;

    private Graph graph;
    private List<Vertex> shortestPath;

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

    // ================== public use methods end here ==================

    public void paint(Graphics g) {
        drawGraph(g);
        if (this.shortestPath != null) {
            drawPath(g);
        }
    }

    private void drawGraph(Graphics g) {
        Graphics g_ = getGraphics();
        graph.getAllVertices().forEach(v -> {
            // Draw nodes
            String lat = Double.toString(v.getLatitude());
            String lon = Double.toString(v.getLongitude());
            int[] v_coords = convertToXAndY(new String[] { lat, lon });
            g_.drawOval(v_coords[0] - radius / 2, v_coords[1] - radius / 2, radius, radius);

            // Draw edges
            graph.getNeighboursOf(v).forEach(n -> {
                String n_lat = Double.toString(n.v.getLatitude());
                String n_lon = Double.toString(n.v.getLongitude());
                int[] n_coords = convertToXAndY(new String[] { n_lat, n_lon });
                g_.drawLine(v_coords[0], v_coords[1], n_coords[0], n_coords[1]);
            });

            // Testing to see whether way intersection is handled; or if we need to detect where ways intersect
            if (graph.getNeighboursOf(v).size() > 2) {
                // A node in a way can have at most 2 neighbors. If it's bigger, it IS handled :D
                Color oldColor = g.getColor();
                g_.setColor(Color.MAGENTA);

                int temp_radius = 3 * graph.getNeighboursOf(v).size();

                g_.drawOval(v_coords[0] - temp_radius / 2, v_coords[1] - temp_radius / 2, temp_radius, temp_radius);

                g_.setColor(oldColor);
            }
        });

    }


    private void drawPath(Graphics g) {
        // TODO actually draw shortest path
        // Available in this.shortestPath
        g.drawLine(0, 0, window_x, window_y);
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

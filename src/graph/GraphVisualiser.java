package graph;

import java.util.*;
import java.util.List;
import java.awt.*;
import javax.swing.*;

import pathfinding.framework.*; 

import java.io.*;

public class GraphVisualiser extends Canvas {

    // It's really small on my screen when it's only 700x900
    final static int multiplier = 2;
    private static boolean DRAW_NODES = false;

    private static double MIN_LONG;
    private static double MAX_LONG;
    private static double MIN_LAT;
    private static double MAX_LAT;
    final static int window_x = 1600*multiplier;
    final static int window_y = 900*multiplier;
    final static int radius = 6*multiplier;

    private Graph graph;
    private List<Vertex> shortestPath;
    private List<Edge> visited;

    public GraphVisualiser(Graph graph, BoundingBox bbox) {
        this.graph = graph;

        MIN_LAT = bbox.SOUTH;
        MAX_LAT = bbox.NORTH;
        MIN_LONG = bbox.WEST;
        MAX_LONG = bbox.EAST;

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

    public void drawVisited(List<Edge> visited){
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
            if (DRAW_NODES) {
                g.drawOval(v_coords[0] - radius / 2, v_coords[1] - radius / 2, radius, radius);
            }

            // Draw edges
            graph.getNeighboursOf(v).forEach(n -> {
                String n_lat = Double.toString(n.v.getLatitude());
                String n_lon = Double.toString(n.v.getLongitude());
                int[] n_coords = convertToXAndY(new String[] { n_lat, n_lon });
                g.drawLine(v_coords[0], v_coords[1], n_coords[0], n_coords[1]);
            });
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

            //drawThickLine(g, v_coords[0], v_coords[1], prev_coords[0], prev_coords[1]);
            g.drawLine(v_coords[0], v_coords[1], prev_coords[0], prev_coords[1]);

            prev = v;
        }
        g.setColor(oldColor);

    }

    private void drawVisited(Graphics g){
        Color oldColor = g.getColor();
        g.setColor(new Color(0,0,153));

        this.visited.forEach(edge ->{
            Vertex node1 = edge.getStart();
            Vertex node2 = edge.getEnd(); 
            String node1_lat = Double.toString(node1.getLatitude());
            String node1_lon = Double.toString(node1.getLongitude());
            int[] node1_coords = convertToXAndY(new String[] { node1_lat, node1_lon });
            String node2_lat = Double.toString(node2.getLatitude());
            String node2_lon = Double.toString(node2.getLongitude());
            int[] node2_coords = convertToXAndY(new String[] { node2_lat, node2_lon });

            g.drawLine(node1_coords[0], node1_coords[1], node2_coords[0], node2_coords[1]);
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

    private static void drawThickLine(Graphics g, int x1, int y1, int x2, int y2) {

        int delta = 1;
        int[] xPoints;
        int[] yPoints;

        if (x1 < x2) {
            if (y1 < y2) {
                // DONE
                xPoints = new int[]{x1-delta, x1+delta, x2+delta, x2-delta};
                yPoints = new int[]{y1+delta, y1-delta, y2-delta, y2+delta};
            } else {
                // DONE
                //y2 < y1
                xPoints = new int[]{x2-delta, x2+delta, x1+delta, x1-delta};
                yPoints = new int[]{y2-delta, y2+delta, y1+delta, y1-delta};
            }
        } else {
            // x2 < x1
            if (y1 < y2) {
                // DONE
                xPoints = new int[]{x1-delta, x1+delta, x2+delta, x2-delta};
                yPoints = new int[]{y1-delta, y1+delta, y2+delta, y2-delta};
            } else {
                // 
                //y2 < y1
                xPoints = new int[]{x2-delta, x2+delta, x1+delta, x1-delta};
                yPoints = new int[]{y2+delta, y2-delta, y1-delta, y1+delta};
            }
        }
       

        g.fillPolygon(xPoints, yPoints, 4);
    }
    
}

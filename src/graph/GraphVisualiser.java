package graph;

import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

import pathfinding.framework.*; 

import java.io.*;

public class GraphVisualiser extends Canvas {

    // It's really small on my screen when it's only 700x900
    final static int zoom_level = 1;
    private static boolean DRAW_NODES = false;

    private static double MIN_LONG;
    private static double MAX_LONG;
    private static double MIN_LAT;
    private static double MAX_LAT;
    static int window_x, window_y;
    static int image_width, image_height;
    final static int radius = 12*zoom_level;

    private Graph graph;

    // For landmark drawing
    private Set<Vertex> landmarks = new HashSet<>();

    private List<Vertex> shortestPath;
    private List<Edge> visited;
    private Vertex meetingNode;

    public GraphVisualiser(Graph graph, BoundingBox bbox) {
        this.graph = graph;

        MIN_LAT = bbox.SOUTH;
        MAX_LAT = bbox.NORTH;
        MIN_LONG = bbox.WEST;
        MAX_LONG = bbox.EAST;

        double dx = Math.abs(bbox.EAST - bbox.WEST);
        double dy = Math.abs(bbox.NORTH - bbox.SOUTH);
        double bboxRatio = dx/dy;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double screenRatio = screenSize.getWidth()/(screenSize.getHeight()-100);
        if (bboxRatio > screenRatio) {
            // long bounding box
            window_x = (int) (screenSize.getWidth()-100);
            window_y = (int) (dy * (screenSize.getWidth()-100)/dx);
        } else {
            // tall bounding box
            window_x = (int) (dx * (screenSize.getHeight()-100)/dy);
            window_y = (int) (screenSize.getHeight()-100);
        }
        image_width = window_x * zoom_level;
        image_height = window_y * zoom_level;
        window_y += 100;

        setBackground(Color.WHITE);
    }

    public void visualize(String windowName) {
        JFrame f = new JFrame(windowName);
        // f.add(new BufferedImage(window_x, window_y, BufferedImage.TYPE_INT_ARGB));

        BufferedImage img = new BufferedImage(image_width, image_height, BufferedImage.TYPE_INT_ARGB);
        paint(img.getGraphics()); // Actually draw stuff!!!!

        // Enable scrolling around
        ImageIcon imgIcon = new ImageIcon(img);
        JScrollPane scrollPane = new JScrollPane(new JLabel(imgIcon));
        f.getContentPane().add(scrollPane);
        
        f.setSize(window_x,window_y);
        f.setVisible(true);
    }

    // Public method that will eventually call the internal method that actually draws a path
    public void drawPath(List<Vertex> path) {
        this.shortestPath = path;
    }

    public void drawVisited(List<Edge> visited){
        this.visited = visited;
    }

    public void drawMeetingNode(Vertex v){
        this.meetingNode = v; 
    }

    public void drawPoint(Set<Vertex> v) {
        this.landmarks = v;
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
            drawStartGoal(g);
        }
        if (this.meetingNode != null){
            drawMeetingNode(g);
        }
        if (this.landmarks.size() > 0){
            drawPoint(g);
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
                
                // TODO for debugging
                drawThickLine(g, v_coords[0], v_coords[1], n_coords[0], n_coords[1]);

                g.drawLine(v_coords[0], v_coords[1], n_coords[0], n_coords[1]);
            });
        });

    }


    private void drawPath(Graphics g) {
        g.setColor(Color.BLACK);

        if (this.shortestPath.size() == 0) {
            System.out.println("no shortest path");
            return;
        }

        Vertex prev = this.shortestPath.get(0);
        for (int i = 1; i < this.shortestPath.size(); i++) {
            Vertex v = this.shortestPath.get(i);
            String v_lat = Double.toString(v.getLatitude());
            String v_lon = Double.toString(v.getLongitude());
            int[] v_coords = convertToXAndY(new String[] { v_lat, v_lon });
            
            String prev_lat = Double.toString(prev.getLatitude());
            String prev_lon = Double.toString(prev.getLongitude());
            int[] prev_coords = convertToXAndY(new String[] { prev_lat, prev_lon });

            drawThickLine(g, v_coords[0], v_coords[1], prev_coords[0], prev_coords[1]);
            prev = v;
        }
    }

    private void drawStartGoal(Graphics g) {
        // Our list have start at the end, and goal at the front!
        // Draw end

        if (this.shortestPath.size() == 0) {
            return;
        }

        Vertex v = this.shortestPath.get(0);
        String v_lat = Double.toString(v.getLatitude());
        String v_lon = Double.toString(v.getLongitude());
        int[] v_coords = convertToXAndY(new String[] { v_lat, v_lon });
        g.setColor(Color.BLACK);
        g.fillOval(v_coords[0] - (radius+4) / 2, v_coords[1] - (radius+4) / 2, radius+4, radius+4);
        g.setColor(Color.red);
        g.fillOval(v_coords[0] - radius / 2, v_coords[1] - radius / 2, radius, radius);

        // Draw start
        v = this.shortestPath.get(this.shortestPath.size() - 1 );
        v_lat = Double.toString(v.getLatitude());
        v_lon = Double.toString(v.getLongitude());
        v_coords = convertToXAndY(new String[] { v_lat, v_lon });
        g.setColor(Color.BLACK);
        g.fillOval(v_coords[0] - (radius+4) / 2, v_coords[1] - (radius+4) / 2, radius+4, radius+4);
        g.setColor(Color.green);
        g.fillOval(v_coords[0] - radius / 2, v_coords[1] - radius / 2, radius, radius);
    }

    private void drawVisited(Graphics g){
        // Calculate min/max distance to get the colors right
        double minDist = Double.MAX_VALUE;
        double maxDist = 0;

        if (this.visited.size() == 0) {
            return;
        }

        for (Edge edge : this.visited) {
            double dist = edge.getDist();
            if (dist > maxDist) {
                maxDist = dist;
            }
            if (dist < minDist) {
                minDist = dist;
            }
        }

        // Actually color the edges!
        int index = 0;
        for (Edge edge : this.visited) {
            // Color according to when an edge was considered
            // float h = (index * 1.0f) / (this.visited.size() * 1.0f); // hue

            // Color according to distance in algorithm
            double hRaw = ((edge.getDist()-minDist) / (maxDist - minDist));
            double hLoDist = 0.2; // 72 degrees: slightly yellowish green
            double hHiDist = 1.0; // 360 degrees: red
            double hTranslated = hLoDist + hRaw / (hLoDist + hHiDist);

            float s = 1; // saturation
            float b = 1; // brightness
            float h = (float) hTranslated;
            g.setColor(Color.getHSBColor(h, s, b));
            
            Vertex node1 = edge.getStart();
            Vertex node2 = edge.getEnd(); 
            String node1_lat = Double.toString(node1.getLatitude());
            String node1_lon = Double.toString(node1.getLongitude());
            int[] node1_coords = convertToXAndY(new String[] { node1_lat, node1_lon });
            String node2_lat = Double.toString(node2.getLatitude());
            String node2_lon = Double.toString(node2.getLongitude());
            int[] node2_coords = convertToXAndY(new String[] { node2_lat, node2_lon });

            g.drawLine(node1_coords[0], node1_coords[1], node2_coords[0], node2_coords[1]);
            index++;
        }
    }

    private void drawMeetingNode(Graphics g){

        Vertex v = meetingNode;
        String v_lat = Double.toString(v.getLatitude());
        String v_lon = Double.toString(v.getLongitude());
        int[] v_coords = convertToXAndY(new String[] { v_lat, v_lon });
        g.setColor(Color.BLACK);
        g.fillOval(v_coords[0] - (radius+4) / 2, v_coords[1] - (radius+4) / 2, radius+4, radius+4);
        g.setColor(Color.CYAN);
        g.fillOval(v_coords[0] - radius / 2, v_coords[1] - radius / 2, radius, radius);

    } 

    private void drawPoint(Graphics g){
        for (Vertex v: landmarks){
            String v_lat = Double.toString(v.getLatitude());
            String v_lon = Double.toString(v.getLongitude());
            int[] v_coords = convertToXAndY(new String[] { v_lat, v_lon });
            g.setColor(Color.BLACK);
            g.fillOval(v_coords[0] - (radius+4) / 2, v_coords[1] - (radius+4) / 2, radius+4, radius+4);
            g.setColor(Color.PINK);
            g.fillOval(v_coords[0] - radius / 2, v_coords[1] - radius / 2, radius, radius);
        }
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

        int x = (int) Math.round((lon-MIN_LONG)/d_long * image_width);
        int y = image_height - (int) Math.round((lat-MIN_LAT)/d_lat * image_height);

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

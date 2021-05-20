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

    private static final double RADIUS = 6378137.0; /* in meters on the equator */
    private static double MIN_X; // meters
    private static double MAX_X; // meters
    private static double MIN_Y; // meters
    private static double MAX_Y; // meters
    static int window_x, window_y;
    static int image_width, image_height;
    final static int radius = 12*zoom_level;

    private Graph graph;

    // For landmark drawing
    private Collection<Vertex> activelandmarks = new HashSet<>();
    private Collection<Vertex> landmarks = new HashSet<>();

    private List<Vertex> shortestPath;
    private List<Edge> visited;
    private Vertex meetingNode;

    public GraphVisualiser(Graph graph, BoundingBox bbox) {
        this.graph = graph;


        MIN_Y = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(bbox.SOUTH) / 2));
        MAX_Y = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(bbox.NORTH) / 2));
        MIN_X = Math.toRadians(bbox.WEST);
        MAX_X = Math.toRadians(bbox.EAST);

        System.out.println("south: " + bbox.SOUTH + "    minY: " + MIN_Y);

        double dx = Math.abs(bbox.EAST - bbox.WEST);
        double dy = Math.abs(bbox.NORTH - bbox.SOUTH);
        double bboxRatio = dx/dy;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double screenRatio = screenSize.getWidth()/(screenSize.getHeight()-100);
        // if (bboxRatio > screenRatio) {
        //     // long bounding box
        //     window_x = (int) (screenSize.getWidth()-100);
        //     window_y = (int) (dy * (screenSize.getWidth()-100)/dx);
        // } else {
        //     // tall bounding box
        //     window_x = (int) (dx * (screenSize.getHeight()-100)/dy);
        //     window_y = (int) (screenSize.getHeight()-100);
        // }
        window_x = (int) screenSize.getWidth() - 50;
        window_y = (int) screenSize.getHeight() - 50;
        image_width = (window_x - 100) * zoom_level;
        image_height = (window_y - 100) * zoom_level;

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

    public void drawPoint(Collection<Vertex> landmarks, Collection<Vertex> activeLandmarks) {
        this.landmarks = landmarks;
        this.activelandmarks = activeLandmarks;
    }

    // ================== public use methods end here ==================

    public void paint(Graphics g) {
        g.setColor(new Color(0,0,0, 20)); // 20 alpha out of 255
        drawGraph(g);
        if (this.visited != null) {
            drawVisited(g);
        }


        if (this.landmarks.size() > 0 || this.activelandmarks.size() > 0){
            drawPoint(g);
        }
        if (this.meetingNode != null){
            drawMeetingNode(g);
        }
        if (this.shortestPath != null) {
            drawPath(g);
            drawStartGoal(g);
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
        for (Vertex v: activelandmarks){
            String v_lat = Double.toString(v.getLatitude());
            String v_lon = Double.toString(v.getLongitude());
            int[] v_coords = convertToXAndY(new String[] { v_lat, v_lon });
            g.setColor(Color.BLACK);
            g.fillOval(v_coords[0] - (radius+4) / 2, v_coords[1] - (radius+4) / 2, radius+4, radius+4);
            g.setColor(Color.MAGENTA);
            g.fillOval(v_coords[0] - radius / 2, v_coords[1] - radius / 2, radius, radius);
        }
    }

    public static int[] convertToXAndY(String[] arg){
        
        double lon = Math.toRadians(Double.parseDouble(arg[1]));
        double lat = Math.toRadians(Double.parseDouble(arg[0]));
        // System.out.println("lat: " + lat);

        // https://wiki.openstreetmap.org/wiki/Mercator#Java
        // https://stackoverflow.com/a/14330009
        double y = Math.log(Math.tan(Math.PI / 4 + lat / 2));
        double x = lon;

        x = x - MIN_X; // ensure no negative coords
        y = y - MIN_Y;

        int paddingBothSides = 50 * 2; // pad 50px on all sides

        int mapWidth = image_width - paddingBothSides;
        int mapHeight = image_height - paddingBothSides;

        double adjusted_max_x = MAX_X - MIN_X;
        double adjusted_max_y = MAX_Y - MIN_Y;
        

        double mapWidthRatio = mapWidth / (adjusted_max_x);
        double mapHeightRatio = mapHeight / (adjusted_max_y);

        double globalRatio = Math.min(mapWidthRatio, mapHeightRatio);

        double heightPadding = (image_height - (globalRatio * adjusted_max_y)) / 2;
        double widthPadding = (image_width - (globalRatio * adjusted_max_x)) / 2;

        int imgX = (int) (widthPadding + (x * globalRatio));
        int imgY = (int) (image_height - heightPadding - (y * globalRatio));

        return new int[]{imgX, imgY};
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

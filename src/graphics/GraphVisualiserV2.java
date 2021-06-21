package graphics;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;

import graph.*;
import pathfinding.framework.*;
import pathfinding.standard.*;
import utility.*;

public class GraphVisualiserV2 {
    private BufferedImage img;

    private int window_x, window_y;
    private int image_width, image_height;
    private double currZoom, baseScale;

    ProjectionConstants pc;

    private BlockingQueue<Road> queue;
    private static int VIS_THREADS = 3; //TODO
    private static int PIXELS = (int) 2e4; // How big the "full-size" picture should be.
    private static Color BACKGROUND_COLOR = new Color(240, 240, 240); // TODO doesn't work :(


    public GraphVisualiserV2(BoundingBox bbox) {
        double MIN_X = Math.toRadians(bbox.WEST);
        double MAX_X = Math.toRadians(bbox.EAST);
        double MIN_Y = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(bbox.SOUTH) / 2));
        double MAX_Y = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(bbox.NORTH) / 2));

        // Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension screenSize = new Dimension(800, 600);
        window_x = (int) screenSize.getWidth() - 50;
        window_y = (int) screenSize.getHeight() - 50;
        image_width =  PIXELS;
        image_height = (int) ( PIXELS * ( (1.0*window_y)/(1.0*window_x)) );

        // Constants for projection
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
        
        pc = new ProjectionConstants(MIN_X, MIN_Y, widthPadding, heightPadding, image_height, globalRatio);


        // Handle actually getting ready to draw
        img = new BufferedImage(image_width, image_height, BufferedImage.TYPE_INT_RGB);


        // Prepare the background
        Graphics2D tempGraphics = img.createGraphics();
        tempGraphics.setColor(BACKGROUND_COLOR);
        tempGraphics.fillRect(0, 0, image_width, image_height);

        // Graphics queue (for concurrent use!)
        queue = new LinkedBlockingQueue<>();
    }

    public void drawGraph(Graph graph) {
        Graphics2D graphics = img.createGraphics();

        for (int threads = 0; threads < VIS_THREADS; threads++) {
            new Thread(new DrawingThread(queue, graphics)).start();
        }
        
        for (Vertex v : graph.getAllVertices()) {
            // Draw nodes
            int[] v_coords = Projection.convertToXAndY(pc, v.getLatitude(), v.getLongitude());

            // Draw edges
            for (Neighbor n : graph.getNeighboursOf(v)) {
                int[] n_coords = Projection.convertToXAndY(pc, n.v.getLatitude(), n.v.getLongitude());
                
                try {
                    queue.put(new Road(v_coords[0], v_coords[1], n_coords[0], n_coords[1]));
                } catch (InterruptedException e) { e.printStackTrace(); }
            }

        }
    }

    public void drawSolution(Solution s) {
        drawVisited(s.getVisited());
        // TODO draw start/end
        drawPath(s.getShortestPath());
        // TODO draw landmarks
    }

    public void showGUI(String windowName) {
        while (! queue.isEmpty()) {
            // wait for drawing to finish
        }

        currZoom = 0.9; // 0.9 to fit within first window
        baseScale = Math.min((1.0 * window_x) / (1.0 * img.getWidth()),
                             (1.0 * window_y) / (1.0 * img.getHeight()) );

        JScrollPane scrollPane = new JScrollPane(); // Enable scrolling around
        scrollPane.setPreferredSize(new Dimension(window_x, window_y));
        scrollPane.setBackground(BACKGROUND_COLOR);

        BufferedImage scaled = GraphicsUtils.zoom(img, baseScale, currZoom);
        JLabel imgLabel = new JLabel(new ImageIcon(scaled));

        scrollPane.setViewportView(imgLabel);

        JFrame window = new JFrame(windowName);
        window.setBackground(BACKGROUND_COLOR);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // TODO: does it close other windows?
        
        window.add(scrollPane, BorderLayout.CENTER);
        window.pack();
        window.setVisible(true);


        window.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent ke) {}
            public void keyReleased(KeyEvent ke) {}

            @Override
            public void keyPressed(KeyEvent ke) {
                if (KeyEvent.VK_UP != ke.getKeyCode()
                 && KeyEvent.VK_DOWN != ke.getKeyCode()) {
                    return;
                }

                String zoomText = "..."; // TODO put text in front when waiting for zoom
                
                double deltaZoom;
                if (KeyEvent.VK_UP == ke.getKeyCode()) {
                    System.out.println("up");
                    deltaZoom = 2;
                    zoomText = "Zooming in...";
                } else if (KeyEvent.VK_DOWN == ke.getKeyCode()) {
                    System.out.println("down");
                    deltaZoom = 0.5;
                    zoomText = "Zooming out...";
                } else deltaZoom = 1;

                currZoom *= deltaZoom;

                // Zoom in on another thread to enable scrolling while calculating!
                // High-level: create new BufferedImage to make setViewportView() fast
                //    (most time was taken here)
                // Now most time is in creating the BufferedImage, which can be done async
                new Thread(() -> {
                    BufferedImage zoomed = GraphicsUtils.zoom(img, baseScale, currZoom);

                    double newHorizontal = deltaZoom * scrollPane.getHorizontalScrollBar().getValue();
                    double newVertical = deltaZoom * scrollPane.getVerticalScrollBar().getValue();
                    
                    // Actually show new zoomed-in image!
                    scrollPane.setViewportView(new JLabel(new ImageIcon(zoomed)));

                    // Zoom towards center of old zoom level
                    // The constants just work, don't question it :D
                    newHorizontal += (deltaZoom > 1 ? 1.0 : -0.5) * scrollPane.getHorizontalScrollBar().getVisibleAmount() / 2;
                    newVertical   += (deltaZoom > 1 ? 1.0 : -0.5) * scrollPane.getVerticalScrollBar().getVisibleAmount() / 2;
                    // btw --->  visibleAmount := width_of_scrollpane (pixels), so it works with window resizing too
                    scrollPane.getHorizontalScrollBar().setValue((int) newHorizontal);
                    scrollPane.getVerticalScrollBar().setValue(  (int) newVertical  );
                }).start();                                        
            }
        });

    }



    // ================================================================
    // ================================================================

    private void drawPath(List<Vertex> path) {
        if (path.size() < 2) return;

        for (int i = 0; i < path.size()-1; i++) {
            Vertex a = path.get(i);
            Vertex b = path.get(i+1);
            int[] a_xy = Projection.convertToXAndY(pc, a.getLatitude(), a.getLongitude());
            int[] b_xy = Projection.convertToXAndY(pc, b.getLatitude(), b.getLongitude());
            Road road = new Road(a_xy[0], a_xy[1], b_xy[0], b_xy[1], 5); // width=5

            try {
                queue.put(road);
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    private void drawVisited(List<Edge> visited) {
        // Calculate min/max distance to get the colors right
        double minDist = Double.MAX_VALUE;
        double maxDist = 0;

        for (Edge edge : visited) {
            double dist = edge.getDist();
            if (dist > maxDist) {
                maxDist = dist;
            }
            if (dist < minDist) {
                minDist = dist;
            }
        }

        // Actually color the edges!
        double zeroedOutMaxDist = maxDist - minDist;
        double hLoDist = 0.3; // 108 degrees (green)
        double hHiDist = 1.0; // 360 degrees (red)
        for (Edge edge : visited) {
            // Color according to when an edge was considered
            // float h = (index * 1.0f) / (this.visited.size() * 1.0f); // hue

            // Color according to distance.
            // Interpolate between hLo and hHi (green -> red)
            double hRaw = ((edge.getDist()-minDist) / zeroedOutMaxDist);
            double hTranslated = hLoDist + hRaw / (hLoDist + hHiDist);

            float s = 1; // saturation
            float b = 0.9f; // brightness
            float h = (float) hTranslated;
            Color c = Color.getHSBColor(h, s, b);
            
            Vertex start = edge.getStart();
            Vertex end = edge.getEnd(); 

            int[] start_xy = Projection.convertToXAndY(pc, start.getLatitude(), start.getLongitude());
            int[] end_xy = Projection.convertToXAndY(pc, end.getLatitude(), end.getLongitude());

            try {
                queue.put(new Road(start_xy[0], start_xy[1], end_xy[0], end_xy[1], c));
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    private static void drawLandmarks(LandmarkSelector ls) {
        // TODO
    }


    

    public static void main(String[] args) {
        
        long loadStart = System.currentTimeMillis();

        Graph graph = GraphIO.loadGraphFromCSV("denmark-pruned.graph");

        long loadEnd = System.currentTimeMillis();
        System.out.println("Loading: " + (loadEnd-loadStart) + " ms");

        // long start1 = System.currentTimeMillis();
        // GraphVisualiser vis1 = new GraphVisualiser(graph, BoundingBox.Denmark);
        // vis1.visualize("V1");
        // long end1 = System.currentTimeMillis();
        // System.out.println("done!");

        // Vertex a = GraphUtils.pickRandomVertex(graph);
        // Vertex b = GraphUtils.pickRandomVertex(graph);
        Vertex a = GraphUtils.findNearestVertex(graph, Location.Skagen);
        Vertex b = GraphUtils.findNearestVertex(graph, Location.Lolland);

        PathfindingAlgo p = new DijkstraTraditional(graph);
        Solution solution = p.shortestPath(a, b);

        long start2 = System.currentTimeMillis();
        GraphVisualiserV2 vis2 = new GraphVisualiserV2(BoundingBox.Denmark);
        vis2.drawGraph(graph);
        vis2.drawVisited(solution.getVisited());
        vis2.drawPath(solution.getShortestPath());
        vis2.showGUI("V2");
        long end2 = System.currentTimeMillis();
        System.out.println("done!");




        // System.out.println("V1: " + (end1-start1) + " ms");
        System.out.println("V2: " + (end2-start2) + " ms");
    }
    
}


class DrawingThread implements Runnable {
    BlockingQueue<Road> queue;
    Graphics2D graphics;
    boolean moreRoads;
    int roads = 0;

    public DrawingThread(BlockingQueue<Road> queue, Graphics2D graphics) {
        this.queue = queue;
        this.graphics = graphics;
        moreRoads = true;
    }

    @Override
    public void run() {
        while(moreRoads) {
            Road road = null;
            try {
                road = queue.take();
            } catch (InterruptedException e) { e.printStackTrace(); }

            roads++;
            if (roads % 100000 == 0) System.out.print(".");

            if (! graphics.getColor().equals(road.c)) {
                graphics.setColor(road.c);
            }

            graphics.setStroke(new BasicStroke(road.size));

            graphics.draw(new Line2D.Double(road.x1, road.y1, road.x2, road.y2));  /*(road.x1, road.y1,
                                road.x2, road.y2);*/
        }
    }

    public void stop() {
        moreRoads = false;
    }
}

class Road {
    public final int x1, y1, x2, y2;
    public final Color c;
    public final int size;

    public Road(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.c = Color.BLACK; // default
        this.size = 1;        // default
    }

    public Road(int x1, int y1, int x2, int y2, Color c) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.c = c;
        this.size = 1;  // default
    }

    public Road(int x1, int y1, int x2, int y2, int size) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.c = Color.BLACK;
        this.size = size;
    }
}


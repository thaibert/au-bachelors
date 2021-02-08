package graph;

import java.awt.*;
import javax.swing.*;

import java.io.*;

public class Viz_ways extends Canvas{
    

    // NE = (10.2050, 56.1850)
    // SE = (10.2050, 56.1600)
    // SW = (10.1700, 56.1600)
    // NW = (10.1700, 56.1850)
    final static double MIN_LONG = 10.1700;
    final static double MAX_LONG = 10.2050;
    final static double MIN_LAT = 56.1600;
    final static double MAX_LAT = 56.1850;
    final static int window_x = 504*3;
    final static int window_y = 648*3;
    final static int radius = 6;

    public void paint(Graphics g){
        setBackground(Color.white);
        
        paintNodes(g);
        paintEdges(g);

    }

    public static void main(String[] args) {
        JFrame f = new JFrame();
        
        Viz_ways m = new Viz_ways();
        f.add(m);
        
        f.setSize(window_x,window_y);
        f.setVisible(true);
        
    }

    public void paintNodes(Graphics g){
        try {

            File file = new File("intersections.csv");
            InputStream temp = new FileInputStream(file);
            InputStreamReader input = new InputStreamReader(temp);
            System.out.println("--------------------");

            BufferedReader reader = new BufferedReader(input);
            reader.readLine();
            String currentLine = reader.readLine();
            while (currentLine != null) { //TODO when should this stop, also remember to close reader
                String[] args = currentLine.split(",");
                int[] coordinates = convertToXAndY(args);
                
                g.drawOval(coordinates[0]-radius/2, coordinates[1]-radius/2, radius, radius);
                //System.out.printf("X coordinate: %d Y coordinate: %d \n",coordinates[0], coordinates[1]);
                currentLine = reader.readLine();
            }
            reader.close(); // TODO should probably be in a final block
        } catch(Exception e) {
            System.out.println("--> " + e);

        } 
        
        
    }

    public void paintEdges(Graphics g){
        try {
            int prev_wayId = 0;
            int prev_x = 0;
            int prev_y = 0;


            File file = new File("all-roads.csv");
            InputStream temp = new FileInputStream(file);
            InputStreamReader input = new InputStreamReader(temp);
            System.out.println("--------------------");

            BufferedReader reader = new BufferedReader(input);
            reader.readLine();
            String currentLine = reader.readLine();
            while (currentLine != null) { //TODO when should this stop, also remember to close reader
                
                String[] args = currentLine.split(",");
                int[] coordinates = convertToXAndY(args);

                int currentWay_Id = Integer.parseInt((args[2]));

                if (currentWay_Id != prev_wayId){
                    prev_wayId = currentWay_Id;
                    prev_x = coordinates[0];
                    prev_y = coordinates[1];
                }
                else {
                    g.drawLine(coordinates[0], coordinates[1], prev_x, prev_y);
                    prev_x = coordinates[0];
                    prev_y = coordinates[1];
                    //System.out.printf("X coordinate: %d Y coordinate: %d \n",coordinates[0], coordinates[1]);
                }
                currentLine = reader.readLine();
            }
            reader.close(); // TODO should probably be in a final block
        } catch(Exception e) {
            System.out.println("--> " + e);

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

        int x = (int) Math.round((lon-MIN_LONG)/d_long * window_x);
        int y = window_y - (int) Math.round((lat-MIN_LAT)/d_lat * window_y);

        //System.out.printf("output x: %d, y: %d\n", x, y);


        int[] result = {x,y};
        return result;
    }
}

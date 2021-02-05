package graph;

import java.util.*;

public class Point {

    private double longitude;
    private double latitude;
    private List<Point> neighbors;
    private Map<Point, Integer> weights; 

    public Point(double longitude, double latitude, List<Point> neighbors, Map<Point, Integer> weights) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.neighbors = neighbors;
        this.weights = weights;
    }

    public List<Point> getNeighbors() {
        return neighbors;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public Map<Point, Integer> getWeights() {
        return weights;
    }
}

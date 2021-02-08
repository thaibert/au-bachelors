package graph;


public class Vertex {

    private double longitude;
    private double latitude;


    public Vertex(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void equals() {

    }

    public int hashCode() {
        return 0;
    }


}
